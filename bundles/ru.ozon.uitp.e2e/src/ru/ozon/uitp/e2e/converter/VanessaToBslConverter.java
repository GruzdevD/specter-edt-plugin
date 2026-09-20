package ru.ozon.uitp.e2e.converter;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Конвертер сценариев Vanessa Automation в программные BSL-модули расширения СП_Тестирование.
 *
 * <p>Правило трансформации (клиентский контур):
 * 1 сценарий/фича Vanessa = 1 общий модуль (CommonModule) расширения класса
 * {@code clientManagedApplication + clientOrdinaryApplication} (модуль-набор исполняется
 * в клиентском контуре на реальном UI 1C):</p>
 *  - Module.bsl:
 *      * СписокТестов() -> Массив { Имя, Описание, Теги, Отключен, ПричинаОтключения } (Discovery)
 *      * ЗапуститьНаборТестов(Критерии) -> Массив результатов { Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка }
 *        (контракт клиентского раннера СП_ТестированиеКлиент, литеральные статусы
 *        "passed"/"failed"/"skipped"/"aborted")
 *      * Процедура Тест_*() Экспорт с реальными вызовами API клиентского движка:
 *          - СП_ДействияКлиент (НажатьКнопку, УстановитьЗначение, УстановитьФлаг,
 *            ПолучитьЗначение, ОчиститьПоле, ПерейтиНаВкладку, ДобавитьСтроку, НайтиСтроку)
 *          - СП_ОжиданияКлиент (ДождатьсяОткрытияОкна, ДождатьсяЗакрытияОкна,
 *            ДождатьсяДоступностиЭлемента, ДождатьсяВидимостиЭлемента, Пауза)
 *          - СП_ТестированиеКлиент (ОткрытьФормуУниверсально, СохранитьВПамять, ПолучитьИзПамяти)
 *          - СП_УтвержденияКлиент (УтверждениеИстина/Ложь/Равенство/НеРавенство/Заполнено)
 *      * ПередЗапускомТеста() / ПослеЗавершенияТеста()
 *  - &lt;ИмяМодуля&gt;.mdo с метаданными чисто клиентского модуля (clientManagedApplication /
 *    clientOrdinaryApplication). Подстановка в Configuration.mdo — VanessaConversionManager.
 *
 * <p>ВАЖНО про подсветку EDT (v8-code-style): сгенерированный клиентский код имеет следующие
 * особенности, гасящие проверки стандарта без потери функциональности:</p>
 *  - НЕ вызывает серверные модули СП_Тестирование / СП_Утверждения (иначе undefined-variable
 *    на управляемом клиенте). Статусы — литералы, утверждения — СП_УтвержденияКлиент.
 *  - Фиксированные структуры с &gt; 4 ключей и динамический доступ (Рез.Значение, Рез.ok)
 *    гасятся @skip-check (structure-consructor-too-many-keys / bsl-legacy-check-dynamic-feature-access).
 *  - Строки-литералы, содержащие пользовательские данные из сценария ($…$, $&lt;…&gt;$, кавычки),
 *    гасятся @skip-check bsl-legacy-check-string-literal по месту.
 *  - Экспортные методы имеют doc-комментарии (doc-comment-export-method / collection-item-type).
 */
public class VanessaToBslConverter {

	// Статусы клиентского раннера (литералы, совпадают с СП_Тестирование.Статус*).
	private static final String STATUS_PASSED = "\"passed\"";
	private static final String STATUS_FAILED = "\"failed\"";
	private static final String STATUS_SKIPPED = "\"skipped\"";
	private static final String STATUS_ABORTED = "\"aborted\"";

	/** Структура теста в СписокТестов(): Discovery контракта. */
	private static final String TEST_STRUCT_FIELDS = "Имя, Описание, Теги, Отключен, ПричинаОтключения";
	/** Структура результата в ЗапуститьНаборТестов(). */
	private static final String RESULT_STRUCT_FIELDS = "Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка";

	private static final Pattern SECONDS_PATTERN = Pattern.compile("в течение (\\d+) секунд|в течение (\\d+) сек|(\\d+) секунд");

	/**
	 * Ожидаемое количество строк таблицы: оператор сравнения (русское слово или символ,
	 * опционально в кавычках) + число. Используется в шагах «количество строк <оператор> N»,
	 * где число стоит голым (вне кавычек) и парсером параметров не извлекается.
	 */
	private static final Pattern TABLE_COUNT_EXPECTED_PATTERN = Pattern.compile(
			"(?:не равно|больше или равно|меньше или равно|больше|меньше|равно|равен|равняется|>=|<=|>|<|=)[\"']?\\s*(\\d+)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	/**
	 * Генерирует BSL-код модуля (Module.bsl) на базе сценария Vanessa,
	 * строго реализующий контракт клиентского тестового набора СП_Тестирование.
	 */
	public String generateBslModuleCode(VanessaScenario scenario) {
		StringBuilder sb = new StringBuilder();
		String moduleName = scenario.getTargetModuleName();
		String scenarioName = scenario.getScenarioName();
		String testMethodName = generateTestMethodName(scenarioName);

		sb.append("//©///////////////////////////////////////////////////////////////////////////©//\n");
		sb.append("//  Модуль тестового набора: ").append(moduleName).append("\n");
		sb.append("//  Автоматически сконвертирован из сценария Vanessa Automation:\n");
		sb.append("//  Функционал: ").append(scenario.getFeatureName()).append("\n");
		sb.append("//  Сценарий: ").append(scenarioName).append("\n");
		if (scenario.getSourceFile() != null) {
			sb.append("//  Исходный файл: ").append(scenario.getSourceFile().getName()).append("\n");
		}
		if (!scenario.getTags().isEmpty()) {
			sb.append("//  Теги: @").append(String.join(" @", scenario.getTags())).append("\n");
		}
		sb.append("//  Контур исполнения: клиентский (реальный UI 1C), раннер СП_ТестированиеКлиент\n");
		sb.append("//  Движок: Specter / СП_Тестирование (1C:EDT E2E Engine)\n");
		sb.append("//©///////////////////////////////////////////////////////////////////////////©//\n\n");

		sb.append("#Область ПрограммныйИнтерфейс\n\n");

		// 1. Контракт Discovery движка СП_Тестирование
		sb.append("// Точка обнаружения тестов в наборе движком СП_Тестирование (Discovery).\n");
		sb.append("//\n");
		sb.append("//@skip-check doc-comment-collection-item-type\n");
		sb.append("// Возвращаемое значение:\n");
		sb.append("//  Массив - структуры тестов набора (Имя, Описание, Теги, Отключен, ПричинаОтключения).\n");
		sb.append("Функция СписокТестов() Экспорт\n\n");
		sb.append("\tТесты = Новый Массив;\n");
		sb.append("\t//@skip-check structure-consructor-too-many-keys\n");
		sb.append("\tТесты.Добавить(Новый Структура(\n");
		sb.append("\t\t\"").append(TEST_STRUCT_FIELDS).append("\",\n");
		sb.append("\t\t\"").append(testMethodName).append("\",\n");
		sb.append("\t\t\"").append(escapeBslString(scenarioName)).append("\",\n");
		sb.append("\t\tСлужебный_ПолучитьТегиСценария(),\n");
		sb.append("\t\t").append(scenario.isSkipped() ? "Истина" : "Ложь").append(",\n");
		sb.append("\t\t\"").append(escapeBslString(scenario.getSkipReason())).append("\"));\n\n");
		sb.append("\tВозврат Тесты;\n\n");
		sb.append("КонецФункции\n\n");

		// 2. Контракт запуска набора тестов клиентским раннером СП_ТестированиеКлиент
		sb.append("// Главная точка запуска набора клиентским раннером СП_ТестированиеКлиент.\n");
		sb.append("//\n");
		sb.append("//@skip-check doc-comment-params-order\n");
		sb.append("// Параметры:\n");
		sb.append("//  Критерии - Структура - фильтр запуска: ИмяТеста (необязателен).\n");
		sb.append("//\n");
		sb.append("//@skip-check doc-comment-collection-item-type\n");
		sb.append("// Возвращаемое значение:\n");
		sb.append("//  Массив - результаты выполненных тестов набора\n");
		sb.append("//   (Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка).\n");
		sb.append("Функция ЗапуститьНаборТестов(Критерии = Неопределено) Экспорт\n\n");
		sb.append("\tРезультаты = Новый Массив;\n");
		sb.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
		sb.append("\tИмяТеста = ?(ЗначениеЗаполнено(Критерии) И Критерии.Свойство(\"ИмяТеста\"), Критерии.ИмяТеста, \"\");\n\n");
		sb.append("\tЕсли ПустаяСтрока(ИмяТеста) ИЛИ ИмяТеста = \"").append(testMethodName).append("\" Тогда\n");

		if (scenario.isSkipped()) {
			sb.append("\t\t//@skip-check structure-consructor-too-many-keys\n");
			sb.append("\t\tРезультаты.Добавить(Новый Структура(\n");
			sb.append("\t\t\t\"").append(RESULT_STRUCT_FIELDS).append("\",\n");
			sb.append("\t\t\t\"").append(testMethodName).append("\", ").append(STATUS_SKIPPED).append(", ")
			  .append("\"").append(escapeBslString(scenario.getSkipReason())).append("\", 0, Новый Массив, \"\"));\n");
		} else {
			sb.append("\t\tДатаНачала = ТекущаяУниверсальнаяДатаВМиллисекундах();\n");
			sb.append("\t\tПопытка\n");
			sb.append("\t\t\tПередЗапускомТеста();\n");
			sb.append("\t\t\t").append(testMethodName).append("();\n");
			sb.append("\t\t\tПослеЗавершенияТеста();\n");
			sb.append("\t\t\tДлительность = ТекущаяУниверсальнаяДатаВМиллисекундах() - ДатаНачала;\n");
			sb.append("\t\t\t//@skip-check structure-consructor-too-many-keys\n");
			sb.append("\t\t\tРезультаты.Добавить(Новый Структура(\n");
			sb.append("\t\t\t\t\"").append(RESULT_STRUCT_FIELDS).append("\",\n");
			sb.append("\t\t\t\t\"").append(testMethodName).append("\", ").append(STATUS_PASSED)
			  .append(", \"Тест выполнен успешно\", Длительность, Новый Массив, \"\"));\n");
			sb.append("\t\tИсключение\n");
			sb.append("\t\t\tДлительность = ТекущаяУниверсальнаяДатаВМиллисекундах() - ДатаНачала;\n");
			sb.append("\t\t\tТекстОшибки = ОписаниеОшибки();\n");
			sb.append("\t\t\tСтатус = ?(СтрНайти(ТекстОшибки, \"ASSERT_FAILED\") > 0, ").append(STATUS_FAILED).append(", ").append(STATUS_ABORTED).append(");\n");
			sb.append("\t\t\t//@skip-check structure-consructor-too-many-keys\n");
			sb.append("\t\t\tРезультаты.Добавить(Новый Структура(\n");
			sb.append("\t\t\t\t\"").append(RESULT_STRUCT_FIELDS).append("\",\n");
			sb.append("\t\t\t\t\"").append(testMethodName).append("\", Статус, ТекстОшибки, Длительность, Новый Массив, ТекстОшибки));\n");
			sb.append("\t\tКонецПопытки;\n");
		}
		sb.append("\tКонецЕсли;\n\n");
		sb.append("\tВозврат Результаты;\n\n");
		sb.append("КонецФункции\n\n");

		// 3. Основной тестовый метод
		sb.append("// Обработка сценария Vanessa: ").append(scenarioName).append(".\n");
		sb.append("//\n");
		sb.append("//&Тест\n");
		sb.append("// @test\n");
		sb.append("Процедура ").append(testMethodName).append("() Экспорт\n");

		// Трансляция каждого шага сценария. Тело собираем отдельно, чтобы решить,
		// нужна ли переменная Форма: автогенерация пустой «Форма = Неопределено;» в сценариях,
		// где Форма не используется, даёт неиспользуемую локальную переменную (валидатор).
		StringBuilder body = new StringBuilder();
		List<VanessaStep> steps = scenario.getSteps();
		for (int i = 0; i < steps.size(); i++) {
			VanessaStep step = steps.get(i);
			body.append("\t// Шаг ").append(i + 1).append(" (").append(step.getType().getKeyword()).append("): ")
			    .append(step.getNormalizedText()).append("\n");

			body.append(translateStepToBsl(step)).append("\n\n");
		}

		// Переменная Форма объявляется только если реально используется в сгенерированном
		// коде (не в комментариях/строках) и не объявлена собственным присваиванием.
		if (needsFormVariable(body.toString())) {
			sb.append("\t// Форма сценария (открывается через СП_ТестированиеКлиент.ОткрытьФормуУниверсально)\n");
			sb.append("\tФорма = Неопределено;\n");
		}

		sb.append("\n").append(body);
		sb.append("КонецПроцедуры\n\n");

		// Вспомогательные методы жизненного цикла
		sb.append("// Подготовка окружения перед выполнением теста.\n");
		sb.append("Процедура ПередЗапускомТеста() Экспорт\n");
		sb.append("\t// Очистка или подготовка тестовых данных при необходимости\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("// Очистка окружения после завершения теста.\n");
		sb.append("Процедура ПослеЗавершенияТеста() Экспорт\n");
		sb.append("\t// Закрытие тестовых окон или удаление временных данных\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("#КонецОбласти\n\n");

		sb.append("#Область СлужебныеПроцедурыИФункции\n\n");
		sb.append("// Возвращает теги сценария для Discovery (СписокТестов).\n");
		sb.append("// Возвращаемое значение:\n");
		sb.append("//  Массив из Строка - теги сценария.\n");
		sb.append("Функция Служебный_ПолучитьТегиСценария()\n\n");
		sb.append("\tТеги = Новый Массив;\n");
		for (String tag : scenario.getTags()) {
			sb.append("\tТеги.Добавить(\"").append(escapeBslString(tag)).append("\");\n");
		}
		sb.append("\tВозврат Теги;\n\n");
		sb.append("КонецФункции\n\n");
		sb.append("#КонецОбласти\n");

		return sb.toString();
	}

	/**
	 * Генерирует XML метаданных (.mdo) для нового чисто клиентского общего модуля 1C.
	 * Исключает &lt;server&gt;true&lt;/server&gt; для предотвращения смешения контекстов при работе с формами.
	 */
	public String generateMdoMetadata(String moduleName) {
		String uuid = UUID.randomUUID().toString();
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<mdclass:CommonModule xmlns:mdclass=\"http://g5.1c.ru/v8/dt/metadata/mdclass\" uuid=\"" + uuid + "\">\n"
				+ "  <name>" + moduleName + "</name>\n"
				+ "  <synonym>\n"
				+ "    <key>ru</key>\n"
				+ "    <value>Тест: " + moduleName.replace("СП_Тест_", "").replace("_Клиент", "").replace('_', ' ') + "</value>\n"
				+ "  </synonym>\n"
				+ "  <comment>Сконвертировано из Vanessa Automation в движок СП_Тестирование (клиентский контур)</comment>\n"
				+ "  <clientManagedApplication>true</clientManagedApplication>\n"
				+ "  <clientOrdinaryApplication>true</clientOrdinaryApplication>\n"
				+ "</mdclass:CommonModule>\n";
	}

	/**
	 * Декомпозиция табличного шага Vanessa («в таблице …») на примитивы
	 * СП_ДействияКлиент. Возвращает сгенерированный фрагмент, либо null,
	 * если шаблон не распознан (тогда внешний код ставит честный TODO).
	 * Работает поверх существующих примитивов ТЧ (ЗначениеТаблицы /
	 * УстановитьЗначениеЯчейки / АктивизироватьСтрокуТаблицы / КоличествоСтрок).
	 */
	private String translateTableStepToBsl(String raw, String text, List<String> params) {
		StringBuilder b = new StringBuilder();
		String table = params.size() > 0 ? params.get(0) : "Таблица";

		// «запоминаю значение поля X таблицы Y как Z»: params=[поле, таблица, переменная]
		if (text.contains("запоминаю значение поля") && text.contains("таблицы") && params.size() >= 3) {
			String field = params.get(0);
			String ownerTable = params.get(1);
			String var = params.get(2);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезТаблицы = СП_ДействияКлиент.ЗначениеТаблицы(Форма, \"").append(escapeBslString(ownerTable)).append("\", 0, \"").append(escapeBslString(field)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезТаблицы.ok, РезТаблицы.message);\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_ТестированиеКлиент.СохранитьВПамять(\"").append(escapeBslString(var)).append("\", РезТаблицы.Значение);");
			return b.toString();
		}

		// «поле [с именем] … имеет значение …» — чтение колонки + проверка
		if (text.contains("поле") && text.contains("имеет значение") && params.size() >= 3) {
			String field = params.get(1);
			String value = params.get(2);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезТаблицы = СП_ДействияКлиент.ЗначениеТаблицы(Форма, \"").append(escapeBslString(table)).append("\", 0, \"").append(escapeBslString(field)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезТаблицы.ok, РезТаблицы.message);\n");
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Строка(РезТаблицы.Значение) = \"" + escapeBslString(value)
			   + "\", \"Поле '" + escapeBslString(field) + "' таблицы '" + escapeBslString(table) + "' должно быть равно '" + escapeBslString(value) + "'\");");
			return b.toString();
		}

		// «поле … заполнено / не заполнено»
		if (text.contains("поле") && (text.contains("заполнено") || text.contains("не заполнено")) && params.size() >= 2) {
			String field = params.get(1);
			boolean filled = !text.contains("не заполнено");
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезТаблицы = СП_ДействияКлиент.ЗначениеТаблицы(Форма, \"").append(escapeBslString(table)).append("\", 0, \"").append(escapeBslString(field)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезТаблицы.ok, РезТаблицы.message);\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(").append(filled ? "ЗначениеЗаполнено" : "НЕ ЗначениеЗаполнено")
			   .append("(РезТаблицы.Значение), \"Поле '").append(escapeBslString(field)).append("' таблицы '").append(escapeBslString(table)).append("' ").append(filled ? "должно быть заполнено" : "должно быть пустым").append("\");");
			return b.toString();
		}

		// «активизирую поле [с именем] …»
		if (text.contains("активизирую поле") && params.size() >= 2) {
			String field = params.get(1);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьПолеВТаблице(Форма, \"").append(escapeBslString(table)).append("\", \"").append(escapeBslString(field)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «ввожу текст …» / «из выпадающего списка … выбираю точное значение …» — запись ячейки
		if ((text.contains("ввожу текст") || text.contains("выпадающего списка")) && params.size() >= 3) {
			String field = params.get(1);
			String value = params.get(2);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначениеЯчейки(Форма, \"").append(escapeBslString(table)).append("\", 0, \"").append(escapeBslString(field)).append("\", \"").append(escapeBslString(value)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «устанавливаю/изменяю флаг [с именем] …»
		if (text.contains("флаг") && (text.contains("устанавливаю") || text.contains("изменяю")) && params.size() >= 2) {
			String field = params.get(1);
			boolean on = !text.contains("снимаю");
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначениеЯчейки(Форма, \"").append(escapeBslString(table)).append("\", 0, \"").append(escapeBslString(field)).append("\", ").append(on ? "Истина" : "Ложь").append(");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «завершаю редактирование строки»
		if (text.contains("завершаю редактирование")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.ЗавершитьРедактированиеСтроки(Форма, \"").append(escapeBslString(table)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «нажимаю на кнопку [с именем] …»
		if (text.contains("нажимаю на кнопку") && text.contains("именем") && params.size() >= 2) {
			String btn = params.get(1);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопку(Форма, \"").append(escapeBslString(btn)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «выбираю текущую строку» / «перехожу к первой строке» / «на одну строку вверх»
		if (text.contains("выбираю текущую строку") || text.contains("перехожу к первой строке") || text.contains("на одну строку вверх")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьСтрокуТаблицы(Форма, \"").append(escapeBslString(table)).append("\", 0);\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «на одну строку вниз» (следующая строка)
		if (text.contains("на одну строку вниз")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьСтрокуТаблицы(Форма, \"").append(escapeBslString(table)).append("\", 1);\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «запоминаю количество строк таблицы X как <переменная>» — сохранение, не проверка.
		// Идёт ДО блока «количество строк <оператор> N», чтобы текст «запоминаю количество строк»
		// не ушёл в проверку: здесь параметры = [таблица, переменная].
		if (text.contains("запоминаю количество строк") && text.contains("как") && params.size() >= 2) {
			String ownerTable = params.get(0);
			String var = params.get(1);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tКолТаблицы = СП_ДействияКлиент.КоличествоСтрок(Форма, \"").append(escapeBslString(ownerTable)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_ТестированиеКлиент.СохранитьВПамять(\"").append(escapeBslString(var)).append("\", КолТаблицы);");
			return b.toString();
		}

		// «пока в таблице … количество строк <оператор> N Тогда» — цикл ожидания выполнения условия
		if (text.contains("пока") && text.contains("тогда") && text.contains("количество строк") && !params.isEmpty()) {
			String ownerTable = params.get(0);
			String opWord = params.size() > 1 ? params.get(1) : "";
			String expected = extractExpectedTableCount(raw);
			if (expected != null) {
				String bslOp = mapComparisonToBsl(opWord);
				b.append("\tПока Истина Цикл\n");
				b.append("\t\t//@skip-check bsl-legacy-check-string-literal\n");
				b.append("\t\tКолТаблицы = СП_ДействияКлиент.КоличествоСтрок(Форма, \"").append(escapeBslString(ownerTable)).append("\");\n");
				b.append("\t\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
				b.append("\t\tЕсли КолТаблицы ").append(bslOp).append(" ").append(expected).append(" Тогда\n");
				b.append("\t\t\tПрервать;\n");
				b.append("\t\tКонецЕсли;\n");
				b.append("\t\tСП_ОжиданияКлиент.Пауза(1);\n");
				b.append("\tКонецЦикла;");
				return b.toString();
			}
		}

		// «количество строк <оператор> N» — проверка количества строк таблицы.
		// Оператор («равно», «>», «меньше или равно») приходит в params как слово/символ,
		// а ожидаемое число — голым (вне кавычек) и парсером не извлекается, поэтому
		// значение разбираем regex-ом из исходного текста шага; оператор транслируем в BSL.
		if (text.contains("количество строк") && !params.isEmpty()) {
			String ownerTable = params.get(0);
			String opWord = params.size() > 1 ? params.get(1) : "";
			String expected = extractExpectedTableCount(raw);
			if (expected == null) {
				// Количество не распознано — честный TODO вместо битого вызова.
				return "// TODO (табличный контекст): " + escapeBslString(raw) + "\n"
					 + "// Не удалось распознать ожидаемое количество строк таблицы.";
			}
			String bslOp = mapComparisonToBsl(opWord);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tКолТаблицы = СП_ДействияКлиент.КоличествоСтрок(Форма, \"").append(escapeBslString(ownerTable)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			if ("=".equals(bslOp)) {
				b.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(КолТаблицы, ").append(expected)
				   .append(", \"Количество строк таблицы '" + escapeBslString(ownerTable) + "'\");");
			} else {
				b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(КолТаблицы ").append(bslOp).append(" ").append(expected)
				   .append(", \"Количество строк таблицы '" + escapeBslString(ownerTable) + "' должно быть ").append(opWord.trim()).append(" ").append(expected).append("\");");
			}
			return b.toString();
		}

		return null;
	}

	/**
	 * Транслирует русское/символьное обозначение сравнения в BSL-оператор.
	 * Применяется к шагам «количество строк <оператор> N»: оператор приходит
	 * параметром («равно», «>», «меньше или равно» и т.п.) и раньше вставлялся
	 * как голый идентификатор, ломая код.
	 */
	private static String mapComparisonToBsl(String op) {
		if (op == null) return "=";
		String o = op.trim().toLowerCase();
		switch (o) {
			case "равно": case "равен": case "равняется": case "=": return "=";
			case "не равно": case "не равен": case "<>": return "<>";
			case "больше или равно": case "не меньше": case ">=": return ">=";
			case "меньше или равно": case "не больше": case "<=": return "<=";
			case "больше": case ">": return ">";
			case "меньше": case "<": return "<";
			default: return "=";
		}
	}

	/**
	 * Извлекает ожидаемое количество строк (число) после оператора сравнения
	 * из исходного текста шага. Оператор может быть задан как русское слово,
	 * так и символом («>», «>=»), в кавычках или без; число следует сразу за ним.
	 * Возвращает строку числа или null, если распознать не удалось.
	 */
	private static String extractExpectedTableCount(String raw) {
		if (raw == null) return null;
		Matcher m = TABLE_COUNT_EXPECTED_PATTERN.matcher(raw);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Решает, объявлять ли локальную переменную Форма в начале тестового метода.
	 * Форму объявляем только если она реально используется в сгенерированном коде
	 * (вне строковых литералов и комментариев) и не объявлена собственным
	 * присваиванием «Форма = …» (присваивание само создаёт локальную переменную).
	 */
	private static boolean needsFormVariable(String body) {
		if (body == null || body.isEmpty()) return false;
		// Убираем строковые литералы и комментарии — Форма в них не считается использованием.
		String stripped = body.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "");
		stripped = stripped.replaceAll("//[^\n]*", "");
		if (!Pattern.compile("\\bФорма\\b").matcher(stripped).find()) return false;
		boolean hasAssign = Pattern.compile("\\bФорма\\s*=").matcher(stripped).find();
		return !hasAssign;
	}

	/**
	 * Транслирует один шаг Vanessa во фрагмент тела клиентской процедуры Тест_*().
	 * Возвращает строку (возможно, многострочную) с завершающим переводом строки.
	 */
	private String translateStepToBsl(VanessaStep step) {
		String raw = step.getNormalizedText();
		String text = raw.toLowerCase();
		List<String> params = step.getParameters();
		StringBuilder bsl = new StringBuilder();

		// --- Пауза / ожидание с фиксированным таймаутом ---
		//  => СП_ОжиданияКлиент.Пауза(N). Должна идти РАНЬШЕ проверки "жду/ожидаю",
		//  иначе "жду N секунд" ошибочно уходит в ДождатьсяВидимостиЭлемента.
		if (text.contains("пауз") || (text.contains("секунд") && (text.contains("жду") || text.contains("ожидаю") || text.contains("подождать")))) {
			int seconds = extractSeconds(raw);
			bsl.append("\tСП_ОжиданияКлиент.Пауза(").append(seconds).append(");");
			return bsl.toString();
		}

		// --- Разворачивание/сворачивание группы формы ---
		if ((text.contains("разворачиваю группу") || text.contains("развернуть группу")
				|| text.contains("сворачиваю группу") || text.contains("свернуть группу")) && params.size() >= 1) {
			String group = params.get(0);
			boolean expand = (text.contains("разворач") || text.contains("развернуть"));
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.РазвернутьГруппу(Форма, \"").append(escapeBslString(group)).append("\", ").append(expand ? "Истина" : "Ложь").append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Проверка наличия элемента на форме (присутствует/отсутствует) ---
		if ((text.contains("присутствует на форме") || text.contains("отсутствует на форме")) && params.size() >= 1) {
			String el = params.get(0);
			boolean present = text.contains("присутствует на форме");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ЭлементНаФорме(Форма, \"").append(escapeBslString(el)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(РезДействия.Присутствует, ").append(present ? "Истина" : "Ложь")
			   .append(", \"Элемент '").append(escapeBslString(el)).append("' должен ").append(present ? "присутствовать" : "отсутствовать").append(" на форме\");");
			return bsl.toString();
		}

		// --- Активизация уже открытой формы по заголовку ---
		if (text.contains("активизирую форму") && params.size() >= 1) {
			String title = params.get(0);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьНаПанелиОткрытых(\"").append(escapeBslString(title)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Установить описание сценария ---
		if (text.contains("описание сценария") || text.contains("задаю описание")) {
			String desc = !params.isEmpty() ? params.get(0) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tСП_ТестированиеКлиент.УстановитьОписаниеСценария(\"").append(escapeBslString(desc)).append("\");");
			return bsl.toString();
		}

		// --- Фильтр динамического списка: установить / очистить ---
		if (text.contains("устанавливаю фильтр списка") || text.contains("установить фильтр списка")
				|| text.contains("установить фильтр по полю") || text.contains("устанавливаю фильтр по полю")) {
			String listName = params.size() > 0 ? params.get(0) : "Список";
			String field = params.size() > 1 ? params.get(1) : "ПолеОтбора";
			String value = params.size() > 2 ? params.get(2) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьФильтрСписка(Форма, \"").append(escapeBslString(listName))
			   .append("\", \"").append(escapeBslString(field)).append("\", \"").append(escapeBslString(value)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("очищаю фильтр списка") || text.contains("очистить фильтр списка") || text.contains("снимаю фильтр списка")) {
			String listName = params.size() > 0 ? params.get(0) : "Список";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ОчиститьФильтрСписка(Форма, \"").append(escapeBslString(listName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Текст текущей ячейки (чтение значения активной ячейки) ---
		if (text.contains("текст текущей ячейки") || text.contains("получаю текст ячейки")) {
			bsl.append("\tРезЯчейки = СП_ДействияКлиент.ПолучитьТекстТекущейЯчейки(Форма);\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезЯчейки.ok, РезЯчейки.message);");
			return bsl.toString();
		}

		// --- Значение ячейки табличного документа по адресу (R6C15) + утверждение равенства ---
		if (text.contains("адресом") && (text.contains("табличном документе") || text.contains("табличного документа"))) {
			String docField = params.size() > 0 ? params.get(0) : "ТабличныйДокумент";
			String addr = params.size() > 1 ? params.get(1) : "R1C1";
			String expected = params.size() > 2 ? params.get(2) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезАдрес = СП_ДействияКлиент.ПолучитьЗначениеЯчейкиТабличногоДокументаПоАдресу(Форма, \"").append(escapeBslString(docField))
			   .append("\", \"").append(escapeBslString(addr)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(РезАдрес.Значение, СП_ТестированиеКлиент.ВычислитьЗначениеСПамятью(\"")
			   .append(escapeBslString(expected));
			bsl.append("\"), \"Ячейка '").append(escapeBslString(addr)).append("' табличного документа '").append(escapeBslString(docField)).append("'\");");
			return bsl.toString();
		}

		// --- Значение ячейки табличного документа по номерам строки/колонки ---
		if ((text.contains("табличного документа") || text.contains("в табличном документе")) && !text.contains("адресом")) {
			String docField = params.size() > 0 ? params.get(0) : "ТабличныйДокумент";
			String row = params.size() > 1 ? params.get(1) : firstNumberIn(raw, "1");
			String col = params.size() > 2 ? params.get(2) : secondNumberIn(raw, "1");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезЯчейки = СП_ДействияКлиент.ПолучитьЗначениеЯчейкиТабличногоДокумента(Форма, \"").append(escapeBslString(docField))
			   .append("\", ").append(row).append(", ").append(col).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезЯчейки.ok, РезЯчейки.message);");
			return bsl.toString();
		}

		// --- Переход к следующей / последней строке таблицы ---
		if (text.contains("следующей строке таблиц") || text.contains("следующую строку таблиц")) {
			String table = !params.isEmpty() ? params.get(0) : "Таблица";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ПерейтиКСледующейСтрокеТаблицы(Форма, \"").append(escapeBslString(table)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("последней строке таблиц") || text.contains("последнюю строку таблиц") || text.contains("в конец таблиц")) {
			String table = !params.isEmpty() ? params.get(0) : "Таблица";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ПерейтиКПоследнейСтрокеТаблицы(Форма, \"").append(escapeBslString(table)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Активизация (фокус) обычного поля формы (не табличного) ---
		if (text.contains("активизирую поле") && params.size() >= 1) {
			String el = params.get(0);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьЭлемент(Форма, \"").append(escapeBslString(el)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Кнопка выбора у поля / у ячейки: открыть список выбора на реальном UI ---
		if (text.contains("кнопку выбора") || text.contains("кнопка выбора")) {
			if (text.contains("ячейк")) {
				String table = params.size() > 0 ? params.get(0) : "Таблица";
				String row = firstNumberIn(raw, "1");
				String col = params.size() > 1 ? params.get(1) : "Колонка";
				bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
				bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопкуВыбораЯчейки(Форма, \"").append(escapeBslString(table))
				   .append("\", ").append(row).append(", \"").append(escapeBslString(col)).append("\");\n");
			} else {
				String field = params.size() > 0 ? params.get(0) : "ПолеВыбора";
				bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
				bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопкуВыбора(Форма, \"").append(escapeBslString(field)).append("\");\n");
			}
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Закрытие окна / ожидание закрытия окна (в т.ч. с таймаутом "в течение N секунд") ---
		if (text.contains("закрытия окна") || text.contains("закрытия формы")) {
			String title = !params.isEmpty() ? params.get(0) : "Форма";
			int seconds = extractSeconds(raw);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tЗакрыто = СП_ОжиданияКлиент.ДождатьсяЗакрытияОкна(\"").append(escapeBslString(title)).append("\", ").append(seconds).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Закрыто, \"Окно '" + escapeBslString(title) + "' должно закрыться\");");
			return bsl.toString();
		}

		// --- Табличный контекст: декомпозиция на примитивы СП_ДействияКлиент (или честный TODO) ---
		if (text.contains("в таблице") || text.contains("таблице ") || text.contains("таблицы")) {
			String tableCode = translateTableStepToBsl(raw, text, params);
			if (tableCode != null) {
				bsl.append(tableCode);
				return bsl.toString();
			}
			bsl.append("\t// TODO (табличный контекст): ").append(escapeBslString(raw)).append("\n");
			bsl.append("\t// В движке есть СП_ДействияКлиент: НайтиСтроку / ДобавитьСтроку / УстановитьЗначениеЯчейки\n");
			bsl.append("\t// СП_ДействияКлиент.НайтиСтроку(Форма, <таблица>, <отбор>); (реализовать по сценарию)");
			return bsl.toString();
		}

		// --- Нажатие кнопки с именем ---
		// Покрывает: «нажимаю кнопку <имя>», «нажимаю на кнопку "<имя>"», «нажать кнопку …»,
		// «click button …». ПРИМЕЧАНИЕ: оператор || имеет меньший приоритет, чем &&, поэтому
		// каждую альтернативу фразы «нажимаю на кнопку» нужно перечислять явно.
		if (text.contains("нажимаю кнопку") || text.contains("нажимаю на кнопку")
				|| text.contains("нажать кнопку") || text.contains("кликаю кнопку")
				|| text.contains("click button")) {
			String btnName = !params.isEmpty() ? params.get(0) : "ОсновныеДействия";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопку(Форма, \"").append(escapeBslString(btnName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Выбор из выпадающего списка ---
		if (text.contains("выпадающего списка") || text.contains("из списка") || text.contains("выбираю точное значение")) {
			String fieldName = params.size() > 0 ? params.get(0) : "ПолеВыбора";
			String fieldValue = params.size() > 1 ? params.get(1) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначение(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(fieldValue)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Переключатель / выбор значения в поле ---
		if (text.contains("переключател")) {
			String fieldName = params.size() > 0 ? params.get(0) : "Переключатель";
			String fieldValue = params.size() > 1 ? params.get(1) : "Истина";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначение(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(fieldValue)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Выбор файла в поле (до общей ветки "в поле", чтобы не перехватилась как ввод текста) ---
		if (text.contains("выбираю файл") || text.contains("выбрать файл") || text.contains("указываю файл")) {
			String filePath = params.size() > 0 ? params.get(0) : "";
			String fieldName = params.size() > 1 ? params.get(1) : "Файл";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ВыбратьФайл(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(filePath)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Ввод текста в поле ---
		if (text.contains("ввожу текст") || text.contains("ввожу в поле") || text.contains("в поле") || text.contains("type into")) {
			String fieldName = params.size() > 0 ? params.get(0) : "ПолеВвода";
			String fieldValue = params.size() > 1 ? params.get(1) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначение(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(fieldValue)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Запоминание в переменную сценария прямого значения (литерала) ---
		//  "Я запоминаю в переменную 'X' значение 'Y'" => params[0]=переменная, params[1]=значение
		if ((text.contains("в переменную") && text.contains("запоминаю"))
				|| (text.contains("в переменную") && text.contains("запомнить"))) {
			String varName = params.size() > 0 ? params.get(0) : "Переменная";
			String literal = params.size() > 1 ? params.get(1) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tСП_ТестированиеКлиент.СохранитьВПамять(\"").append(escapeBslString(varName))
			   .append("\", \"").append(escapeBslString(literal)).append("\");");
			return bsl.toString();
		}

		// --- Запомнить значение поля как переменную ---
		if (text.contains("запоминаю значение") || text.contains("запомнить значение")) {
			String source = params.size() > 0 ? params.get(0) : "ПолеВвода";
			String varName = params.size() > 1 ? params.get(1) : "Переменная";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезПоля = СП_ДействияКлиент.ПолучитьЗначение(Форма, \"").append(escapeBslString(source)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_ТестированиеКлиент.СохранитьВПамять(\"").append(escapeBslString(varName)).append("\", РезПоля.Значение);\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезПоля.ok, РезПоля.message);");
			return bsl.toString();
		}

		// --- Удаление переменных из памяти сценария ---
		if (text.contains("удаляю все переменные") || text.contains("удалить все переменные")) {
			bsl.append("\tСП_ТестированиеКлиент.ОчиститьПеременныеСценария();");
			return bsl.toString();
		}
		if (text.contains("удаляю переменную") || text.contains("удалить переменную")) {
			String varName = !params.isEmpty() ? params.get(0) : "Переменная";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tСП_ТестированиеКлиент.ОчиститьПеременную(\"").append(escapeBslString(varName)).append("\");");
			return bsl.toString();
		}

		// --- Установить флаг ---
		if (text.contains("устанавливаю флаг") || text.contains("снимаю флаг") || text.contains("чекбокс")) {
			String flagName = !params.isEmpty() ? params.get(0) : "Флаг";
			boolean val = !text.contains("снимаю");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьФлаг(Форма, \"").append(escapeBslString(flagName))
			   .append("\", ").append(val ? "Истина" : "Ложь").append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Проверка "флаг ... равен" ---
		if (text.contains("флаг") && (text.contains("равен") || text.contains("равно"))) {
			String flagName = !params.isEmpty() ? params.get(0) : "Флаг";
			String expected = params.size() > 1 ? params.get(1) : "Истина";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезФлага = СП_ДействияКлиент.ПолучитьЗначение(Форма, \"").append(escapeBslString(flagName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(\"" + escapeBslString(expected)
			   + "\", РезФлага.Значение, \"Флаг '" + escapeBslString(flagName) + "' = ожидаемому\");");
			return bsl.toString();
		}

		// --- Очистить поле ---
		if (text.contains("очистить поле") || text.contains("очищаю поле")) {
			String fieldName = !params.isEmpty() ? params.get(0) : "ПолеВвода";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ОчиститьПоле(Форма, \"").append(escapeBslString(fieldName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Переход на закладку / вкладку ---
		if (text.contains("закладк") || text.contains("вкладк") || text.contains("перейти на вкладку")) {
			String panelName = params.size() > 0 ? params.get(0) : "ПанельСтраниц";
			String pageName = params.size() > 1 ? params.get(1) : "ОсновнаяСтраница";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезВкладка = СП_ДействияКлиент.ПерейтиНаВкладку(Форма, \"").append(escapeBslString(panelName))
			   .append("\", \"").append(escapeBslString(pageName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезВкладка.ok, РезВкладка.message);");
			return bsl.toString();
		}

		// --- Открытие окна / ожидание открытия ---
		//  "открылось окно" — то же ожидание, что и "открытия окна" (везде)
		if (text.contains("открытия окна") || text.contains("открывается окно") || text.contains("открылось окно")) {
			String windowTitle = !params.isEmpty() ? params.get(0) : "Форма";
			int seconds = extractSeconds(raw);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tОткрыто = СП_ОжиданияКлиент.ДождатьсяОткрытияОкна(\"").append(escapeBslString(windowTitle)).append("\", ").append(seconds).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Открыто, \"Окно '" + escapeBslString(windowTitle) + "' должно открыться\");");
			return bsl.toString();
		}

		// --- Открытие формы / навигационной ссылки ---
		if (text.contains("открываю") || text.contains("открыть") || text.contains("перехожу по ссылке")
				|| text.contains("навигационную ссылку")) {
			String ref = !params.isEmpty() ? params.get(0) : "Обработка.СП_КонсольТестов.Форма";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tФорма = СП_ТестированиеКлиент.ОткрытьФормуУниверсально(\"").append(escapeBslString(ref)).append("\");");
			return bsl.toString();
		}

		// --- Ожидание доступности элемента ---
		if (text.contains("доступности") || text.contains("доступен") || text.contains("стал активен")) {
			String elem = !params.isEmpty() ? params.get(0) : "ЭлементФормы";
			int seconds = extractSeconds(raw);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tДоступно = СП_ОжиданияКлиент.ДождатьсяДоступностиЭлемента(Форма, \"").append(escapeBslString(elem)).append("\", ").append(seconds).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Доступно, \"Элемент '" + escapeBslString(elem) + "' должен стать доступным\");");
			return bsl.toString();
		}

		// --- Ожидание / видимость ---
		if (text.contains("жду") || text.contains("ожидаю") || text.contains("стал видим")
				|| text.contains("появления") || text.contains("wait")) {
			String elem = !params.isEmpty() ? params.get(0) : "ЭлементФормы";
			int seconds = extractSeconds(raw);
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tВидимо = СП_ОжиданияКлиент.ДождатьсяВидимостиЭлемента(Форма, \"").append(escapeBslString(elem)).append("\", ").append(seconds).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Видимо, \"Таймаут ожидания видимости элемента: '" + escapeBslString(elem) + "'\");");
			return bsl.toString();
		}

		// --- Проверка равенства значения поля ---
		if (text.contains("равно") || text.contains("равен") || text.contains("содержит")
				|| text.contains("проверяю") || text.contains("assert")) {
			String fieldName = params.size() > 0 ? params.get(0) : "Результат";
			String expectedValue = params.size() > 1 ? params.get(1) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезПоля = СП_ДействияКлиент.ПолучитьЗначение(Форма, \"").append(escapeBslString(fieldName)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезПоля.ok, РезПоля.message);\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(\"" + escapeBslString(expectedValue)
			   + "\", РезПоля.Значение, \"Поле '" + escapeBslString(fieldName) + "' содержит ожидаемые данные\");");
			return bsl.toString();
		}

		// --- Табличные операции: семантика сложная, честный TODO ---
		if (text.contains("таблиц") || text.contains("в таблице") || text.contains("строк") || text.contains("table")) {
			bsl.append("\t// TODO (табличный контекст): ").append(escapeBslString(raw)).append("\n");
			bsl.append("\t// В движке есть СП_ДействияКлиент: НайтиСтроку / ДобавитьСтроку / УстановитьЗначениеЯчейки\n");
			bsl.append("\t// СП_ДействияКлиент.НайтиСтроку(Форма, <таблица>, <отбор>); (реализовать по сценарию)");
			return bsl.toString();
		}

		// --- Гиперссылка ---
		if (text.contains("гиперссылк")) {
			String link = !params.isEmpty() ? params.get(0) : "Гиперссылка";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьГиперссылку(Форма, \"").append(escapeBslString(link)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Закрытие окон клиентского приложения ---
		if (text.contains("закрываю все окна") || text.contains("закрыть все окна")) {
			bsl.append("\tРезДействия = СП_ДействияКлиент.ЗакрытьВсеОкна();\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("закрываю окно") || text.contains("закрыть окно") || text.contains("закрываю текущее окно")) {
			String title = !params.isEmpty() ? params.get(0) : "Форма";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ЗакрытьОкноПоЗаголовку(\"").append(escapeBslString(title)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Панель открытых окон ---
		if (text.contains("панели открытых") || text.contains("панель открытых")) {
			String title = !params.isEmpty() ? params.get(0) : "Форма";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьНаПанелиОткрытых(\"").append(escapeBslString(title)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}

		// --- Шаги, ранее честно отмеченные TODO, теперь реализованы примитивами движка ---
		if (text.contains("командном интерфейсе") || text.contains("командный интерфейс")
				|| text.contains("команду интерфейса") || text.contains("команды интерфейса")) {
			String cmd = !params.isEmpty() ? params.get(0) : "ОсновныеДействия";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ВыбратьКомандуИнтерфейса(Форма, \"").append(escapeBslString(cmd)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("контекстного меню")) {
			String cmd = !params.isEmpty() ? params.get(0) : "КомандаКонтекстногоМеню";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ВыбратьПунктКонтекстногоМеню(Форма, \"").append(escapeBslString(cmd)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("сочетание клавиш")) {
			String combo = !params.isEmpty() ? params.get(0) : "Enter";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьСочетаниеКлавиш(Форма, \"").append(escapeBslString(combo)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("перехожу к следующему реквизиту") || text.contains("перехожу к предыдущему реквизиту")
				|| text.contains("перейти к следующему реквизиту")) {
			boolean next = text.contains("следующему") || text.contains("следующего");
			bsl.append("\tРезДействия = СП_ДействияКлиент.").append(next ? "ПерейтиКСледующемуРеквизиту" : "ПерейтиКПредыдущемуРеквизиту").append("(Форма);\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return bsl.toString();
		}
		if (text.contains("разворачиваю группу") || text.contains("развернуть группу")) {
			bsl.append("\t// TODO (сворачивание/разворачивание группы): ").append(escapeBslString(raw)).append("\n");
			bsl.append("\t// Программного разворачивания группы формы на управляемом клиенте нет; убедитесь,\n");
			bsl.append("\t// что группа раскрыта по умолчанию, либо работайте с полями напрямую.");
			return bsl.toString();
		}

		// --- Универсальный вызов шага ---
		bsl.append("\t// Пользовательский шаг: ").append(escapeBslString(raw)).append("\n");
		bsl.append("\t// Реализуйте шаг через СП_ДействияКлиент / СП_ОжиданияКлиент или прямой вызов формы");
		return bsl.toString();
	}

	/** Возвращает первое число из текста шага, иначе default (для номеров строк/колонок таблиц). */
	private String firstNumberIn(String raw, String def) {
		if (raw == null) return def;
		Matcher m = Pattern.compile("\\d+").matcher(raw);
		if (m.find()) {
			return m.group();
		}
		return def;
	}

	/** Возвращает второе число из текста шага, иначе default (для "в строке R колонке C"). */
	private String secondNumberIn(String raw, String def) {
		if (raw == null) return def;
		Matcher m = Pattern.compile("\\d+").matcher(raw);
		int n = 0;
		while (m.find()) {
			n++;
			if (n == 2) {
				return m.group();
			}
		}
		return def;
	}

	/** Извлекает число секунд из текста шага вида "в течение N секунд"; по умолчанию 10. */
	private int extractSeconds(String raw) {
		if (raw == null) return 10;
		Matcher m = SECONDS_PATTERN.matcher(raw);
		if (m.find()) {
			for (int g = 1; g <= m.groupCount(); g++) {
				if (m.group(g) != null) {
					try {
						return Integer.parseInt(m.group(g));
					} catch (NumberFormatException ignored) {
						// no-op
					}
				}
			}
		}
		return 10;
	}

	private String generateTestMethodName(String scenarioName) {
		String cleaned = scenarioName.replaceAll("[^a-zA-Zа-яА-Я0-9_]", "_")
				.replaceAll("_+", "_");
		if (cleaned.startsWith("_")) {
			cleaned = cleaned.substring(1);
		}
		if (cleaned.endsWith("_")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}
		if (!cleaned.startsWith("Тест_")) {
			cleaned = "Тест_" + cleaned;
		}
		return cleaned;
	}

	private String escapeBslString(String str) {
		if (str == null) return "";
		return str.replace("\"", "\"\"");
	}
}
