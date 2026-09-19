package ru.ozon.uitp.e2e.converter;

import java.util.List;
import java.util.UUID;

/**
 * Конвертер сценариев Vanessa Automation в программные BSL-модули расширения СП_Тестирование.
 * 
 * Правило трансформации:
 * 1 сценарий/фича Vanessa = 1 общий модуль (CommonModule) расширения:
 *  - Module.bsl:
 *      * СписокТестов() -> Массив { Имя, Описание, Теги } (контракт обнаружения Discovery)
 *      * ЗапуститьНаборТестов(Критерии) -> Массив результатов { Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка }
 *      * Процедура Тест_*() Экспорт с реальными вызовами API движка:
 *          - СП_ДействияКлиент (УстановитьЗначение, НажатьКнопку, УстановитьФлаг, ПолучитьЗначение, НайтиСтроку, ДобавитьСтроку, ПерейтиНаВкладку, ОчиститьПоле)
 *          - СП_ОжиданияКлиент (ДождатьсяВидимостиЭлемента, ДождатьсяДоступностиЭлемента, ДождатьсяОткрытияОкна, ДождатьсяЗакрытияОкна, Пауза)
 *          - СП_Утверждения (УтверждениеИстина, УтверждениеЛожь, УтверждениеРавенство, УтверждениеНеРавенство, УтверждениеЗаполнено)
 *      * ПередЗапускомТеста() / ПослеЗавершенияТеста()
 *  - <ИмяМодуля>.mdo с метаданными чисто клиентского модуля (clientManagedApplication / clientOrdinaryApplication).
 */
public class VanessaToBslConverter {

	/**
	 * Генерирует BSL-код модуля (Module.bsl) на базе сценария Vanessa,
	 * строго реализующий контракт тестового набора СП_Тестирование.
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
		sb.append("//  Движок: Specter / СП_Тестирование (1C:EDT E2E Engine)\n");
		sb.append("//©///////////////////////////////////////////////////////////////////////////©//\n\n");

		sb.append("#Область ПрограммныйИнтерфейс\n\n");

		// 1. Контракт Discovery движка СП_Тестирование
		sb.append("// Точка обнаружения тестов в наборе движком СП_Тестирование (Discovery).\n");
		sb.append("Функция СписокТестов() Экспорт\n\n");
		sb.append("\tТесты = Новый Массив;\n");
		sb.append("\tТесты.Добавить(Новый Структура(\n");
		sb.append("\t\t\"Имя, Описание, Теги\",\n");
		sb.append("\t\t\"").append(testMethodName).append("\",\n");
		sb.append("\t\t\"").append(escapeBslString(scenarioName)).append("\",\n");
		sb.append("\t\tСлужебный_ПолучитьТегиСценария()));\n\n");
		sb.append("\tВозврат Тесты;\n\n");
		sb.append("КонецФункции\n\n");

		// 2. Контракт запуска набора тестов движком СП_Тестирование
		sb.append("// Главная точка запуска набора движком СП_Тестирование.\n");
		sb.append("Функция ЗапуститьНаборТестов(Критерии = Неопределено) Экспорт\n\n");
		sb.append("\tРезультаты = Новый Массив;\n");
		sb.append("\tИмяТеста = ?(ЗначениеЗаполнено(Критерии) И Критерии.Свойство(\"ИмяТеста\"), Критерии.ИмяТеста, \"\");\n\n");
		sb.append("\tЕсли ПустаяСтрока(ИмяТеста) ИЛИ ИмяТеста = \"").append(testMethodName).append("\" Тогда\n");
		sb.append("\t\tДатаНачала = ТекущаяУниверсальнаяДатаВМиллисекундах();\n");
		sb.append("\t\tПопытка\n");
		sb.append("\t\t\tПередЗапускомТеста();\n");
		sb.append("\t\t\t").append(testMethodName).append("();\n");
		sb.append("\t\t\tПослеЗавершенияТеста();\n");
		sb.append("\t\t\tДлительность = ТекущаяУниверсальнаяДатаВМиллисекундах() - ДатаНачала;\n");
		sb.append("\t\t\tРезультаты.Добавить(Новый Структура(\n");
		sb.append("\t\t\t\t\"Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка\",\n");
		sb.append("\t\t\t\t\"").append(testMethodName).append("\", СП_Тестирование.СтатусПройден(), \"Тест выполнен успешно\", Длительность, Новый Массив, \"\"));\n");
		sb.append("\t\tИсключение\n");
		sb.append("\t\t\tДлительность = ТекущаяУниверсальнаяДатаВМиллисекундах() - ДатаНачала;\n");
		sb.append("\t\t\tТекстОшибки = ОписаниеОшибки();\n");
		sb.append("\t\t\tСтатус = ?(СтрНайти(ТекстОшибки, \"ASSERT_FAILED\") > 0, СП_Тестирование.СтатусПровален(), СП_Тестирование.СтатусПрерван());\n");
		sb.append("\t\t\tРезультаты.Добавить(Новый Структура(\n");
		sb.append("\t\t\t\t\"Имя, Статус, Сообщение, ДлительностьМс, События, Ошибка\",\n");
		sb.append("\t\t\t\t\"").append(testMethodName).append("\", Статус, ТекстОшибки, Длительность, Новый Массив, ТекстОшибки));\n");
		sb.append("\t\tКонецПопытки;\n");
		sb.append("\tКонецЕсли;\n\n");
		sb.append("\tВозврат Результаты;\n\n");
		sb.append("КонецФункции\n\n");

		// 3. Основной тестовый метод
		sb.append("// &Тест\n");
		sb.append("// @test\n");
		sb.append("Процедура ").append(testMethodName).append("() Экспорт\n\n");
		sb.append("\t// Форма сценария (открывается или связывается через агент формы)\n");
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
		sb.append("// Подготовка окружения перед выполнением теста\n");
		sb.append("Процедура ПередЗапускомТеста() Экспорт\n");
		sb.append("\t// Очистка или подготовка тестовых данных при необходимости\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("// Очистка окружения после завершения теста\n");
		sb.append("Процедура ПослеЗавершенияТеста() Экспорт\n");
		sb.append("\t// Закрытие тестовых окон или удаление временных данных\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("#КонецОбласти\n\n");

		sb.append("#Область СлужебныеПроцедурыИФункции\n\n");
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
	 * Исключает <server>true</server> для предотвращения смешения контекстов при работе с формами.
	 */
	public String generateMdoMetadata(String moduleName) {
		String uuid = UUID.randomUUID().toString();
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<mdclass:CommonModule xmlns:mdclass=\"http://g5.1c.ru/v8/dt/metadata/mdclass\" uuid=\"" + uuid + "\">\n"
				+ "  <name>" + moduleName + "</name>\n"
				+ "  <synonym>\n"
				+ "    <key>ru</key>\n"
				+ "    <value>Тест: " + moduleName.replace("СП_Тест_", "").replace('_', ' ') + "</value>\n"
				+ "  </synonym>\n"
				+ "  <comment>Сконвертировано из Vanessa Automation в движок СП_Тестирование</comment>\n"
				+ "  <clientManagedApplication>true</clientManagedApplication>\n"
				+ "  <clientOrdinaryApplication>true</clientOrdinaryApplication>\n"
				+ "</mdclass:CommonModule>\n";
	}

	private String translateStepToBsl(VanessaStep step) {
		String text = step.getNormalizedText().toLowerCase();
		List<String> params = step.getParameters();
		StringBuilder bsl = new StringBuilder();

		if (text.contains("открываю") || text.contains("открыть") || text.contains("перехожу") || text.contains("open")) {
			String formName = !params.isEmpty() ? params.get(0) : "Обработка.СП_КонсольТестов.Форма";
			bsl.append("\tПопытка\n");
			bsl.append("\t\tФорма = ОткрытьФорму(\"").append(escapeBslString(formName)).append("\");\n");
			bsl.append("\tИсключение\n");
			bsl.append("\t\t// Если форма уже открыта в текущей сессии или управляется агентом\n");
			bsl.append("\t\tФорма = Неопределено;\n");
			bsl.append("\tКонецПопытки;");
		} else if (text.contains("нажимаю кнопку") || text.contains("нажать кнопку") || text.contains("кликнуть") || text.contains("click button")) {
			String btnName = !params.isEmpty() ? params.get(0) : "ОсновныеДействия";
			bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопку(Форма, \"").append(escapeBslString(btnName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("заполняю поле") || text.contains("ввожу") || text.contains("в поле") || text.contains("type into")) {
			String fieldName = params.size() > 0 ? params.get(0) : "ПолеВвода";
			String fieldValue = params.size() > 1 ? params.get(1) : "Тестовое значение";
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьЗначение(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(fieldValue)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("устанавливаю флаг") || text.contains("снимаю флаг") || text.contains("чекбокс")) {
			String flagName = !params.isEmpty() ? params.get(0) : "Флаг";
			boolean val = !text.contains("снимаю");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьФлаг(Форма, \"").append(escapeBslString(flagName))
			   .append("\", ").append(val ? "Истина" : "Ложь").append(");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("очистить поле") || text.contains("очищаю поле")) {
			String fieldName = !params.isEmpty() ? params.get(0) : "ПолеВвода";
			bsl.append("\tРезДействия = СП_ДействияКлиент.ОчиститьПоле(Форма, \"").append(escapeBslString(fieldName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("вкладк") || text.contains("перейти на вкладку") || text.contains("перехожу к вкладке")) {
			String panelName = params.size() > 0 ? params.get(0) : "ПанельСтраниц";
			String pageName = params.size() > 1 ? params.get(1) : "ОсновнаяСтраница";
			bsl.append("\tРезВкладка = СП_ДействияКлиент.ПерейтиНаВкладку(Форма, \"").append(escapeBslString(panelName))
			   .append("\", \"").append(escapeBslString(pageName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезВкладка.ok, РезВкладка.message);");
		} else if (text.contains("жду закрытия") || text.contains("закрытия окна") || text.contains("закрытия формы")) {
			String windowTitle = !params.isEmpty() ? params.get(0) : "Форма";
			bsl.append("\tЗакрыто = СП_ОжиданияКлиент.ДождатьсяЗакрытияОкна(\"").append(escapeBslString(windowTitle)).append("\", 15);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(Закрыто, \"Окно '").append(escapeBslString(windowTitle)).append("' должно закрыться\");");
		} else if (text.contains("жду открытия") || text.contains("открытия окна")) {
			String windowTitle = !params.isEmpty() ? params.get(0) : "Форма";
			bsl.append("\tОткрыто = СП_ОжиданияКлиент.ДождатьсяОткрытияОкна(\"").append(escapeBslString(windowTitle)).append("\", 10);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(Открыто, \"Окно '").append(escapeBslString(windowTitle)).append("' должно открыться\");");
		} else if (text.contains("стал доступен") || text.contains("доступности")) {
			String elem = !params.isEmpty() ? params.get(0) : "ЭлементФормы";
			bsl.append("\tДоступно = СП_ОжиданияКлиент.ДождатьсяДоступностиЭлемента(Форма, \"").append(escapeBslString(elem)).append("\", 10);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(Доступно, \"Элемент '").append(escapeBslString(elem)).append("' должен стать доступным\");");
		} else if (text.contains("жду") || text.contains("ожидаю") || text.contains("стал видим") || text.contains("появления элемента") || text.contains("wait")) {
			String elem = !params.isEmpty() ? params.get(0) : "ЭлементФормы";
			bsl.append("\tВидимо = СП_ОжиданияКлиент.ДождатьсяВидимостиЭлемента(Форма, \"").append(escapeBslString(elem)).append("\", 10);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(Видимо, \"Таймаут ожидания видимости элемента: '").append(escapeBslString(elem)).append("'\");");
		} else if (text.contains("равно") || text.contains("содержит") || text.contains("проверяю") || text.contains("тогда поле") || text.contains("assert")) {
			String fieldName = params.size() > 0 ? params.get(0) : "Результат";
			String expectedValue = params.size() > 1 ? params.get(1) : "ОжидаемоеЗначение";
			bsl.append("\tРезПоля = СП_ДействияКлиент.ПолучитьЗначение(Форма, \"").append(escapeBslString(fieldName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезПоля.ok, РезПоля.message);\n");
			bsl.append("\tСП_Утверждения.УтверждениеРавенство(\"").append(escapeBslString(expectedValue))
			   .append("\", РезПоля.Значение, \"Поле '").append(escapeBslString(fieldName)).append("' содержит ожидаемые данные\");");
		} else if (text.contains("добавляю строку") || text.contains("добавить строку")) {
			String tabName = !params.isEmpty() ? params.get(0) : "Товары";
			bsl.append("\tЗначенияКолонок = Новый Структура;\n");
			bsl.append("\tРезТЧ = СП_ДействияКлиент.ДобавитьСтроку(Форма, \"").append(escapeBslString(tabName)).append("\", ЗначенияКолонок);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезТЧ.ok, РезТЧ.message);");
		} else if (text.contains("таблиц") || text.contains("строк") || text.contains("table") || text.contains("найти строку")) {
			String tabName = !params.isEmpty() ? params.get(0) : "Товары";
			bsl.append("\tОтборСтрок = Новый Структура;\n");
			bsl.append("\tРезТЧ = СП_ДействияКлиент.НайтиСтроку(Форма, \"").append(escapeBslString(tabName)).append("\", ОтборСтрок);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезТЧ.ok, РезТЧ.message);");
		} else {
			// Универсальный вызов шага
			bsl.append("\t// Пользовательский шаг: ").append(escapeBslString(step.getNormalizedText())).append("\n");
			bsl.append("\t// Реализуйте шаг через СП_ДействияКлиент / СП_ОжиданияКлиент или прямой вызов формы");
		}

		return bsl.toString();
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

