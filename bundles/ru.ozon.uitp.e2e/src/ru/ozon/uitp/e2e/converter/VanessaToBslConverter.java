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
		sb.append("Процедура ").append(testMethodName).append("() Экспорт\n\n");
		sb.append("\t// Форма сценария (открывается через СП_ТестированиеКлиент.ОткрытьФормуУниверсально)\n");
		sb.append("\tФорма = Неопределено;\n\n");

		// Трансляция каждого шага сценария
		List<VanessaStep> steps = scenario.getSteps();
		for (int i = 0; i < steps.size(); i++) {
			VanessaStep step = steps.get(i);
			sb.append("\t// Шаг ").append(i + 1).append(" (").append(step.getType().getKeyword()).append("): ")
			  .append(step.getNormalizedText()).append("\n");

			String bslStepCode = translateStepToBsl(step);
			sb.append(bslStepCode).append("\n\n");
		}

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
	 * Транслирует один шаг Vanessa во фрагмент тела клиентской процедуры Тест_*().
	 * Возвращает строку (возможно, многострочную) с завершающим переводом строки.
	 */
	private String translateStepToBsl(VanessaStep step) {
		String raw = step.getNormalizedText();
		String text = raw.toLowerCase();
		List<String> params = step.getParameters();
		StringBuilder bsl = new StringBuilder();

		// --- Кнопка выбора у поля: сложная семантика выбора, честный TODO ---
		if (text.contains("кнопку выбора") || text.contains("кнопка выбора")) {
			bsl.append("\t// TODO (клиентский выбор): ").append(escapeBslString(raw)).append("\n");
			bsl.append("\t// В движке нет десктопной кнопки выбора; реализовать через\n");
			bsl.append("\t// СП_ДействияКлиент.УстановитьЗначение(Форма, <Поле>, <Значение>) или АктивизироватьЭлемент.");
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

		// --- Табличный контекст: семантика сложная, честный TODO (не путать кнопки/поля таблицы) ---
		if (text.contains("в таблице") || text.contains("таблиц")) {
			bsl.append("\t// TODO (табличный контекст): ").append(escapeBslString(raw)).append("\n");
			bsl.append("\t// В движке есть СП_ДействияКлиент: НайтиСтроку / ДобавитьСтроку / УстановитьЗначениеЯчейки\n");
			bsl.append("\t// СП_ДействияКлиент.НайтиСтроку(Форма, <таблица>, <отбор>); (реализовать по сценарию)");
			return bsl.toString();
		}

		// --- Нажатие кнопки с именем ---
		if (text.contains("нажимаю") && text.contains("кнопку") && text.contains("именем")
				|| text.contains("нажать кнопку") || text.contains("нажимаю кнопку") || text.contains("click button")) {
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
		if (text.contains("открытия окна") || text.contains("открывается окно")) {
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
		if (text.contains("доступности") || text.contains("стал доступен") || text.contains("стал активен")) {
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

		// --- Универсальный вызов шага ---
		bsl.append("\t// Пользовательский шаг: ").append(escapeBslString(raw)).append("\n");
		bsl.append("\t// Реализуйте шаг через СП_ДействияКлиент / СП_ОжиданияКлиент или прямой вызов формы");
		return bsl.toString();
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
