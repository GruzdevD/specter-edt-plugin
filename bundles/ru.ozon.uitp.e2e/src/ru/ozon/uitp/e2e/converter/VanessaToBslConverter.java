package ru.ozon.uitp.e2e.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	// Допускаем и дробные паузы («в течение 1.5 секунд», «в течение 2,5 сек») — \d+ их не
	// захватывал бы, а такие таймауты валидны в Gherkin. Дробь позже округляется в extractSeconds.
	private static final Pattern SECONDS_PATTERN = Pattern.compile(
			"в течение (\\d+(?:[.,]\\d+)?) секунд|в течение (\\d+(?:[.,]\\d+)?) сек|(\\d+(?:[.,]\\d+)?) секунд");

	/**
	 * Ожидаемое количество строк таблицы: оператор сравнения (русское слово или символ,
	 * опционально в кавычках) + число. Используется в шагах «количество строк <оператор> N»,
	 * где число стоит голым (вне кавычек) и парсером параметров не извлекается.
	 */
	// Допускаем и дробные ожидания («количество строк больше 1,5») — \d+ их отрезал бы и дал
	// ложноположительное сравнение. Дробь валидна в BSL как операнд (запятая = десятичный разделитель).
	private static final Pattern TABLE_COUNT_EXPECTED_PATTERN = Pattern.compile(
			"(?:не равно|больше или равно|меньше или равно|больше|меньше|равно|равен|равняется|>=|<=|>|<|=)[\"']?\\s*(\\d+(?:[.,]\\d+)?)",
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
		sb.append("//  Функционал: ").append(sanitizeComment(scenario.getFeatureName())).append("\n");
		sb.append("//  Сценарий: ").append(sanitizeComment(scenarioName)).append("\n");
		if (scenario.getSourceFile() != null) {
			sb.append("//  Исходный файл: ").append(sanitizeComment(scenario.getSourceFile().getName())).append("\n");
		}
		if (!scenario.getTags().isEmpty()) {
			sb.append("//  Теги: @").append(sanitizeComment(String.join(" @", scenario.getTags()))).append("\n");
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
			// Гарантированная очистка окружения И при падении шага (в 1С нет finally — вызываем
			// ПослеЗавершенияТеста и в ветке Исключение). Без этого упавший тест оставлял открытые
			// формы/модальные окна и загрязнял UI-контекст следующего теста в очереди. Собственный
			// Попытка внутри Исключение гарантирует, что ошибка очистки не замаскирует первопричину.
			sb.append("\t\t\tПопытка\n");
			sb.append("\t\t\t\tПослеЗавершенияТеста();\n");
			sb.append("\t\t\tИсключение\n");
			sb.append("\t\t\t\t// не маскируем первопричину ошибкой очистки\n");
			sb.append("\t\t\tКонецПопытки;\n");
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
		sb.append("// Обработка сценария Vanessa: ").append(sanitizeComment(scenarioName)).append(".\n");
		sb.append("//\n");
		sb.append("//&Тест\n");
		sb.append("// @test\n");
		sb.append("Процедура ").append(testMethodName).append("() Экспорт\n");

		// Трансляция каждого шага сценария. Тело собираем отдельно, чтобы решить,
		// нужна ли переменная Форма: автогенерация пустой «Форма = Неопределено;» в сценариях,
		// где Форма не используется, даёт неиспользуемую локальную переменную (валидатор).
		StringBuilder body = new StringBuilder();
		List<VanessaStep> steps = scenario.getSteps();
		// Раскрытие предметных шагов-макросов (других сценариев фич) происходит на верхнем
		// уровне; при невозможности честного раскрытия шаг переводится как обычно (бизнес-шаг).
		body.append(renderBodyTopLevel(steps, 0));

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
		// Детерминированный UUID по имени модуля: случайный UUID при каждой конвертации заставлял
		// EDT считать объект удалённым/созданным (лавинная перестройка индексов/Xtext-кэшей) и
		// давал конфликт по строке uuid в Git при каждом сохранении Gherkin. Имя-база стабильна.
		String uuid = UUID.nameUUIDFromBytes(moduleName.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
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

		// «текущее поле заполнено / не заполнено» - состояние АКТИВНОЙ ячейки таблицы,
		// имя поля берётся движком из контекста сессии (М2a), а не из шага.
		if (text.contains("текущее поле") && (text.contains("заполнено") || text.contains("не заполнено"))) {
			boolean filled = !text.contains("не заполнено");
			b.append("\tРезДействия = СП_ДействияКлиент.ПроверитьТекущееПоле(Форма, ").append(filled ? "Истина" : "Ложь").append(");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

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
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(Строка(РезТаблицы.Значение) = Строка(")
			   .append(bslValueRef(value))
			   .append("), \"Поле '").append(escapeBslString(field)).append("' таблицы '").append(escapeBslString(table)).append("' должно быть равно '").append(escapeBslString(value)).append("'\");");
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
			b.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначениеЯчейки(Форма, \"").append(escapeBslString(table)).append("\", 0, \"").append(escapeBslString(field)).append("\", ").append(bslValueRef(value)).append(");\n");
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

		// «выделяю все строки» — выделение всех строк таблицы формы (в т.ч. динамического списка)
		if (text.contains("выделяю все строки")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.ВыделитьВсеСтроки(Форма, \"").append(escapeBslString(table)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «перехожу к последней строке» — на последнюю строку таблицы (есть и формулировка «к последней строке таблиц»)
		if (text.contains("перехожу к последней строке") || text.contains("к последней строке таблиц")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.ПерейтиКПоследнейСтрокеТаблицы(Форма, \"").append(escapeBslString(table)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «перехожу к следующей строке» — на следующую строку таблицы
		if (text.contains("перехожу к следующей строке") || text.contains("к следующей строке таблиц")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.ПерейтиКСледующейСтрокеТаблицы(Форма, \"").append(escapeBslString(table)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «активизирую дополнение формы с именем <Y>» — это отдельный элемент формы (строка поиска и т.п.),
		// а не колонка таблицы; активируем его как обычный элемент. params = [таблица, имя дополнения].
		if (text.contains("активизирую дополнение формы") && params.size() >= 2) {
			String el = params.get(1);
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьЭлемент(Форма, \"").append(escapeBslString(el)).append("\");\n");
			b.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			b.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
			return b.toString();
		}

		// «перехожу к строке» (без отбора) — активация строки таблицы (первой). НЕ трогаем
		// «перехожу к строке:» (многострочный отбор по значениям) и «перехожу к строке по шаблону».
		if (text.contains("перехожу к строке") && !text.contains(":") && !text.contains("по шаблону")) {
			b.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			b.append("\tРезДействия = СП_ДействияКлиент.АктивизироватьСтрокуТаблицы(Форма, \"").append(escapeBslString(table)).append("\", 0);\n");
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
	 * (вне строковых литералов и комментариев) и первое её упоминание — НЕ присваивание.
	 * Ключевой сценарий: UI-шаг использует Форма (например «НажатьКнопку(Форма, …)»)
	 * раньше, чем форма открывается и присваивается («Форма = …ОткрытьФормуУниверсально»).
	 * Тогда простой поиск «Форма =» неверно решил бы, что переменная объявлена, и
	 * валидатор дал бы «Переменная 'Форма' не определена». Смотрим первое вхождение:
	 * если сразу за ним следует «=» — переменная объявляется собственным присваиванием;
	 * иначе — это использование до объявления, и нужна строка «Форма = Неопределено;».
	 */
	private static boolean needsFormVariable(String body) {
		if (body == null || body.isEmpty()) return false;
		// Убираем строковые литералы и комментарии — Форма в них не считается использованием.
		String stripped = body.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "");
		stripped = stripped.replaceAll("//[^\n]*", "");
		Matcher m = Pattern.compile("\\bФорма\\b").matcher(stripped);
		if (!m.find()) return false;
		// Позиция сразу после первого токена с пропуском пробелов.
		int after = m.end();
		while (after < stripped.length() && Character.isWhitespace(stripped.charAt(after))) {
			after++;
		}
		// Первое вхождение — присваивание «Форма = …»: объявлять не нужно.
		return after >= stripped.length() || stripped.charAt(after) != '=';
	}

	// =========================================================================
	// РАСКРЫТИЕ МАКРОСОВ (М5): реестр сценариев-фич + инлайн-раскрытие предметных
	// «бизнес-шагов», чьи реализации — ДРУГИЕ сценарии Vanessa (например «Я создаю заявку с
	// типом факторинга "Открытый"» раскрывается в шаги сценария 001_СозданиеЗаявки).
	// =========================================================================

	/**
	 * Реестр сценариев-макросов (имя → сценарий), построенный VanessaConversionManager
	 * по ВСЕЙ папке фич. Позволяет конвертеру раскрыть предметный шаг в его UI-реализацию,
	 * определяя таким образом реальные объекты/формы, которые ожидает тест.
	 */
	private Map<String, VanessaScenario> macroRegistry = new HashMap<>();

	/** Предел глубины рекурсии раскрытия макросов (защита от циклов вида A→B→A). */
	private static final int MAX_MACRO_DEPTH = 12;

	/** Кавычки (одинарные/двойные) для извлечения параметров/значений. */
	private static final Pattern QUOTED_PARAM_PATTERN = Pattern.compile("[\"']([^\"']*)[\"']");

	public void setMacroRegistry(Map<String, VanessaScenario> registry) {
		if (registry != null) {
			this.macroRegistry = registry;
		}
	}

	/** Верхнеуровневый рендер тела тестового метода (первичный сценарий), с нумерацией шагов. */
	private String renderBodyTopLevel(List<VanessaStep> steps, int base) {
		int[] counter = new int[]{base};
		String r = renderSteps(steps, java.util.Collections.emptyMap(), 0, true, counter);
		return r == null ? "" : r;
	}

	/**
	 * Рендерит список шагов в BSL, раскрывая макросы и разрешая условные блоки константой.
	 *
	 * @param steps     шаги (возможно, уже подставленной ветви макроса)
	 * @param args      связанные параметры раскрываемого макроса (имя → фактическое значение)
	 * @param depth     текущая глубина рекурсии раскрытия
	 * @param topLevel  true для первичного сценария: неразрешаемые конструкции дают комментарий,
	 *                  а не откат (в макросе же — откат к бизнес-шагу)
	 * @param counter   счётчик «// Шаг N»
	 * @return BSL-фрагмент; null, если честно раскрыть невозможно (динамическое условие/цикл)
	 */
	private String renderSteps(List<VanessaStep> steps, Map<String, String> args,
			int depth, boolean topLevel, int[] counter) {
		if (depth > MAX_MACRO_DEPTH) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < steps.size()) {
			VanessaStep s = steps.get(i);
			VanessaStep.BlockType bt = s.getBlockType();

			if (bt == VanessaStep.BlockType.IF) {
				IfBlock blk = collectIfBlock(steps, i);
				Boolean cond = resolveCondition(s.getNormalizedText(), args);
				if (cond == null) {
					if (topLevel) {
						out.append("\t// (условие не раскрыто константой): ").append(s.getNormalizedText()).append("\n");
						i = blk.afterEnd;
					} else {
						return null;
					}
					continue;
				}
				List<VanessaStep> branch = cond ? blk.ifBody : blk.elseBody;
				String rb = renderSteps(branch, args, depth + 1, topLevel, counter);
				if (rb == null) {
					return null;
				}
				out.append(rb);
				i = blk.afterEnd;
				continue;
			}

			if (bt == VanessaStep.BlockType.WHILE) {
				if (topLevel) {
					out.append("\t// (цикл не раскрыт): ").append(s.getNormalizedText()).append("\n");
					i++;
					continue;
				}
				return null;
			}

			if (bt == VanessaStep.BlockType.ELSE || bt == VanessaStep.BlockType.END_IF) {
				// Сиротские маркеры (несбалансированная разметка): пропускаем,
				// чтобы не уронить рендер списка боковых шагов.
				i++;
				continue;
			}

			// Обычный шаг: сначала пробуем раскрыть как макрос, иначе переводим примитивом.
			String expanded = expandMacroStep(s, args, depth);
			if (expanded != null) {
				out.append(expanded);
			} else {
				VanessaStep bound = args.isEmpty() ? s : rebindStep(s, args);
				counter[0]++;
				out.append("\t// Шаг ").append(counter[0]).append(" (").append(bound.getType().getKeyword()).append("): ")
				   .append(bound.getNormalizedText()).append("\n");
				out.append(translateStepToBsl(bound)).append("\n\n");
			}
			i++;
		}
		return out.toString();
	}

	/** Результат разбора условного блока: две ветви и индекс шага после КонецЕсли. */
	private static class IfBlock {
		final List<VanessaStep> ifBody = new ArrayList<>();
		final List<VanessaStep> elseBody = new ArrayList<>();
		int afterEnd;
	}

	/** Собирает ветви If/Иначе/КонецЕсли по ванессовской семантике: блок «Если…Тогда»
	 * НЕ требует явного «КонецЕсли» — он закрывается следующим маркером-условием
	 * («Если…Тогда», «Пока…Тогда»), «Иначе», «КонецЕсли» либо концом списка. Так в
	 * фичах УФД ветви «Клиент»/«Дебитор» идут двумя соседними «Если…» без закрытия. */
	private IfBlock collectIfBlock(List<VanessaStep> steps, int ifIndex) {
		IfBlock blk = new IfBlock();
		List<VanessaStep> cur = blk.ifBody;
		int i = ifIndex + 1;
		for (; i < steps.size(); i++) {
			VanessaStep t = steps.get(i);
			VanessaStep.BlockType bt = t.getBlockType();
			if (bt == VanessaStep.BlockType.IF || bt == VanessaStep.BlockType.WHILE) {
				// Следующий маркер-условие закрывает текущий блок (без КонецЕсли).
				break;
			}
			if (bt == VanessaStep.BlockType.ELSE) {
				cur = blk.elseBody;
				continue;
			}
			if (bt == VanessaStep.BlockType.END_IF) {
				i++;
				break;
			}
			cur.add(t);
		}
		// При завершении по маркеру afterEnd указывает на сам маркер (его обработает
		// внешний рендер); при «КонецЕсли» — на следующий шаг после него.
		blk.afterEnd = i;
		return blk;
	}

	/**
	 * Пытается раскрыть шаг как вызов макроса. Возвращает BSL-фрагмент раскрытия,
	 * либо null, если шаг не является вызовом макроса или раскрытие невозможно.
	 */
	private String expandMacroStep(VanessaStep step, Map<String, String> args, int depth) {
		if (macroRegistry.isEmpty() || step.getBlockType() != VanessaStep.BlockType.NONE) {
			return null;
		}
		String stepText = step.getNormalizedText();
		for (VanessaScenario sc : macroRegistry.values()) {
			Map<String, String> bound = matchMacroName(stepText, sc.getScenarioName());
			if (bound == null) {
				continue;
			}
			Map<String, String> merged = new HashMap<>(args);
			merged.putAll(bound);
			int[] counter = new int[]{0};
			String body = renderSteps(sc.getSteps(), merged, depth + 1, false, counter);
			if (body == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			sb.append("\t// --- Макрос раскрыт: ").append(sc.getScenarioName()).append(" ---\n");
			sb.append(body);
			sb.append("\t// --- /Макрос ").append(sc.getScenarioName()).append(" ---\n");
			return sb.toString();
		}
		return null;
	}

	/**
	 * Сопоставляет текст шага-вызова с именем сценария-макроса (шаблон с параметрами в кавычках).
	 * Имя сценария «…номером "НомерЗаявки" …» превращается в regex с группой для параметра;
	 * кавычки вызова дают фактические значения. Возвращает связанные параметры, либо null.
	 */
	private Map<String, String> matchMacroName(String stepText, String scenarioName) {
		if (scenarioName == null || stepText == null) {
			return null;
		}
		// Схлопываем множественные пробелы: в сценариях имена и вызовы часто отличаются
		// лишь лишними пробелами («нового КА  с типом» vs «нового КА с типом»).
		String sc = normalizeSpaces(scenarioName);
		String call = normalizeSpaces(stepText);
		List<String> placeholders = new ArrayList<>();
		StringBuilder regex = new StringBuilder("^");
		Matcher m = QUOTED_PARAM_PATTERN.matcher(sc);
		int last = 0;
		while (m.find()) {
			regex.append(Pattern.quote(sc.substring(last, m.start())));
			regex.append("(.*?)");
			placeholders.add(m.group(1).trim());
			last = m.end();
		}
		regex.append(Pattern.quote(sc.substring(last)));
		regex.append("$");
		try {
			Pattern p = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			if (!p.matcher(call).matches()) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		// Фактические значения из кавычек текста вызова — попарно с именами параметров.
		List<String> callValues = new ArrayList<>();
		Matcher cm = QUOTED_PARAM_PATTERN.matcher(call);
		while (cm.find()) {
			callValues.add(cm.group(1).trim());
		}
		Map<String, String> bound = new HashMap<>();
		for (int k = 0; k < placeholders.size() && k < callValues.size(); k++) {
			if (!placeholders.get(k).isEmpty()) {
				bound.put(placeholders.get(k), callValues.get(k));
			}
		}
		return bound;
	}

	/**
	 * Разрешает условие блока «Если …» константой по известным параметрам.
	 * Пример: Если '"[ТипКА]" = "Клиент"' Тогда → значение параметра ТипКА == "Клиент".
	 * Возвращает Boolean, либо null, если условие динамическое (неразрешимо константой).
	 */
	private Boolean resolveCondition(String condText, Map<String, String> args) {
		if (condText == null) {
			return null;
		}
		// Значения условий обёрнуты в двойные кавычки (даже внутри одинарной обёртки):
		// '"[ТипКА]" = "Клиент"' → ["[ТипКА]", "Клиент"]. Одинарные кавычки служат внешней
		// обёрткой всего выражения и значением не являются.
		List<String> quoted = extractQuoted(condText, '"');
		if (quoted.size() < 2) {
			quoted = extractQuoted(condText, '\'');
		}
		if (quoted.size() < 2) {
			// Непарного сравнения нет — это динамическое условие (напр. «если в таблице есть строка»).
			return null;
		}
		boolean negate = condText.contains("<>") || containsWord(condText, "не равно")
				|| containsWord(condText, "не равен");
		String left = deref(quoted.get(0), args);
		String right = deref(quoted.get(1), args);
		boolean eq = left != null && left.equalsIgnoreCase(right);
		return negate ? !eq : eq;
	}

	/** Схлопывает множественные пробелы и обрезает края. */
	private String normalizeSpaces(String s) {
		if (s == null) {
			return "";
		}
		return s.replaceAll("\\s+", " ").trim();
	}

	/** Извлекает значения, обёрнутые указанной кавычкой (не вложенные). */
	private List<String> extractQuoted(String text, char q) {
		List<String> out = new ArrayList<>();
		if (text == null) {
			return out;
		}
		Matcher m = Pattern.compile(Pattern.quote(String.valueOf(q)) + "([^" + q + "]*)" + Pattern.quote(String.valueOf(q)))
				.matcher(text);
		while (m.find()) {
			out.add(m.group(1).trim());
		}
		return out;
	}

	private boolean containsWord(String text, String word) {
		return text != null && Pattern.compile("(?ui)" + Pattern.quote(word)).matcher(text).find();
	}

	/** Раскрывает [ИмяПараметра] и значение в фактическое значение из args. */
	private String deref(String value, Map<String, String> args) {
		if (value == null) {
			return null;
		}
		String t = value.trim();
		if (t.startsWith("[") && t.endsWith("]")) {
			String key = t.substring(1, t.length() - 1).trim();
			if (args.containsKey(key)) {
				return args.get(key);
			}
		}
		return t;
	}

	/** Подставляет параметры [Имя] в текст (шага, параметра или строки таблицы). */
	private String substituteParams(String text, Map<String, String> args) {
		if (text == null || args == null || args.isEmpty()) {
			return text;
		}
		String out = text;
		for (Map.Entry<String, String> e : args.entrySet()) {
			out = out.replace("[" + e.getKey() + "]", e.getValue());
		}
		return out;
	}

	/** Клонирует шаг с подстановкой параметров в текст, параметры и табличные строки. */
	private VanessaStep rebindStep(VanessaStep s, Map<String, String> args) {
		String newText = substituteParams(s.getNormalizedText(), args);
		VanessaStep copy = new VanessaStep(s.getType(), newText, s.getLineNumber());
		for (String p : s.getParameters()) {
			copy.addParameter(substituteParams(p, args));
		}
		for (String r : s.getTableRows()) {
			copy.addTableRow(substituteParams(r, args));
		}
		copy.setBlockType(s.getBlockType());
		return copy;
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
			   .append("\", \"").append(escapeBslString(field)).append("\", ").append(bslValueRef(value)).append(");\n");
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

		// --- «в логе сообщений TestClient есть строка по шаблону "..."» (М4) ---
		// Ассерт виртуального журнала сессии: ванессовский серверный лог TestClient на тонком/
		// WEB клиенте физически отсутствует, поэтому проверяем журнал, который движок ведёт
		// через СП_ОжиданияКлиент.ЗаписатьСообщение (функционально эквивалентная проверка).
		if (text.contains("логе сообщений") && text.contains("по шаблону")) {
			String template = !params.isEmpty() ? params.get(0) : "";
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезВЖурнале = СП_ОжиданияКлиент.ВЖурналеЕстьСообщение(\"").append(escapeBslString(template)).append("\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезВЖурнале.ok, РезВЖурнале.message);");
			return bsl.toString();
		}

		// --- «завершаю редактирование строки» без имени таблицы (М2a) ---
		// Подтверждение ввода текущей строки; движок берёт активный элемент. Не конфликтует
		// с табличным декомпозером: шаги с «в таблице …» обрабатываются ниже, с указанием имени.
		if (text.contains("завершаю редактирование") && !text.contains("в таблице") && !text.contains("таблице ")
				&& !text.contains("таблицы")) {
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ЗавершитьРедактированиеСтроки(Форма, \"\");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
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
			   .append("\", ").append(bslValueRef(fieldValue)).append(");\n");
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
			   .append("\", ").append(bslValueRef(fieldValue)).append(");\n");
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
			   .append("\", ").append(bslValueRef(fieldValue)).append(");\n");
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
			bsl.append("\tСП_УтвержденияКлиент.УтверждениеРавенство(").append(bslValueRef(expectedValue))
			   .append(", РезПоля.Значение, \"Поле '").append(escapeBslString(fieldName)).append("' содержит ожидаемые данные\");");
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

		// --- Универсальный диспетчер бизнес-шагов (М1) ---
		// Предметные шаги проекта (создать заявку, подключить пользователя, очистить данные и т.п.)
		// не являются UI-примитивами: конвертер передаёт их в СП_ДействияКлиент.ВыполнитьБизнесШаг
		// с извлечёнными параметрами в виде Структуры. Реализация каждого предметного шага живёт
		// в наборе проекта (тело ВыполнитьБизнесШаг расширяется по СтрНачинаетсяС на ИмяШага).
		// Единичный «Пользовательский шаг»-комментарий больше не генерируется: каждый шаг получает
		// реализуемый контур (честный ok=Ложь, если шаг не зарегистрирован в наборе).
		if (params != null && !params.isEmpty()) {
			// Собрать "Параметр1, Параметр2, ..." и список значений из параметров шага.
			StringBuilder keys = new StringBuilder();
			StringBuilder values = new StringBuilder();
			for (int p = 0; p < params.size(); p++) {
				if (p > 0) {
					keys.append(", ");
					values.append(", ");
				}
				keys.append("Параметр").append(p + 1);
				values.append("\"").append(escapeBslString(params.get(p))).append("\"");
			}
			bsl.append("\t//@skip-check structure-consructor-too-many-keys\n");
			bsl.append("\tПараметрыШага = Новый Структура(\"").append(keys).append("\", ").append(values).append(");\n");
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ВыполнитьБизнесШаг(\"").append(escapeBslString(raw)).append("\", ПараметрыШага);\n");
		} else {
			bsl.append("\t//@skip-check bsl-legacy-check-string-literal\n");
			bsl.append("\tРезДействия = СП_ДействияКлиент.ВыполнитьБизнесШаг(\"").append(escapeBslString(raw)).append("\", Неопределено);\n");
		}
		bsl.append("\t//@skip-check bsl-legacy-check-dynamic-feature-access\n");
		bsl.append("\tСП_УтвержденияКлиент.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
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
				String val = m.group(g);
				if (val != null) {
					try {
						return (int) Math.round(Float.parseFloat(val.replace(',', '.')));
					} catch (NumberFormatException ignored) {
						// no-op: пробуем следующую группу паттерна
					}
				}
			}
		}
		return 10;
	}

	/**
	 * Санитизация пользовательского текста перед вставкой в //-комментарий.
	 * Названия фич/сценариев/тегов приходят из Gherkin и могут содержать переводы строк —
	 * «сырая» вставка сломала бы генерацию (текст после \n пошёл бы как код). Заменяем
	 * управляющие переносы пробелом; в строковые литералы значения всё равно уходят через
	 * escapeBslString.
	 */
	private static String sanitizeComment(String text) {
		if (text == null) return "";
		return text.replace('\r', ' ').replace('\n', ' ');
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
		// Экранирование двойной кавычки BSL.
		String res = str.replace("\"", "\"\"");
		// Многострочный текст: BSL-литерал продолжает строку знаком | в начале каждой следующей
		// строки. «Сырой» перенос ломал компиляцию («Неоконченная строка»). Нормализуем \r\n и \r
		// к \n и превращаем каждый перенос в перевод строки + | внутри литерала. Все вызовы
		// escapeBslString оборачивают результат в кавычки, поэтому "\n|" даёт корректный
		// многострочный BSL-строковый литерал.
		res = res.replace("\r\n", "\n").replace('\r', '\n');
		res = res.replace("\n", "\n|");
		return res;
	}

	/**
	 * Оборачивает текстовое значение шага в рантайм-подстановку переменных сценария
	 * ($$Имя$$ / $Имя$). Для обычного литерала движок ВычислитьЗначениеСПамятью возвращает
	 * строку как есть — вызов добавляет нулевую поведенческую дельту, но делает runtime-переменные
	 * (прочитанный с формы номер заявки и т.п.) применимыми на ВСЕХ шагах: ввод текста, проверка
	 * полей, фильтры, выбор значений. Без обёртки значение "$НомерЗаявки$" сравнивалось бы дословно.
	 */
	private String bslValueRef(String value) {
		return "СП_ТестированиеКлиент.ВычислитьЗначениеСПамятью(\"" + escapeBslString(value) + "\")";
	}
}
