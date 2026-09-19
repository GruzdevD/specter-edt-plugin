package ru.ozon.uitp.e2e.converter;

import java.util.List;
import java.util.UUID;

/**
 * Конвертер сценариев Vanessa Automation в программные BSL-модули нашего расширения СП_Тестирование.
 * 
 * Правило трансформации:
 * 1 тест/сценарий = 1 новый общий модуль (CommonModule) расширения:
 *  - Module.bsl с экспортными процедурами &Тест / Тест_*
 *  - <ИмяМодуля>.mdo с метаданными клиентского модуля
 */
public class VanessaToBslConverter {

	/**
	 * Генерирует BSL-код модуля (Module.bsl) на базе сценария Vanessa.
	 */
	public String generateBslModuleCode(VanessaScenario scenario) {
		StringBuilder sb = new StringBuilder();
		String moduleName = scenario.getTargetModuleName();
		String scenarioName = scenario.getScenarioName();

		sb.append("//©///////////////////////////////////////////////////////////////////////////©//\n");
		sb.append("//  Модуль теста: ").append(moduleName).append("\n");
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

		// Основной тестовый метод
		String testMethodName = generateTestMethodName(scenarioName);
		sb.append("// &Тест\n");
		sb.append("// @test\n");
		sb.append("Процедура ").append(testMethodName).append("() Экспорт\n\n");
		sb.append("\t// 1. Инициализация и подключение тестового клиента 1С\n");
		sb.append("\tКонтекстТеста = СП_Тестирование.СоздатьКонтекст(\"").append(escapeBslString(scenarioName)).append("\");\n");
		sb.append("\tТестовыйКлиент = СП_ТестовыйКлиент.Подключить(КонтекстТеста);\n");
		sb.append("\tФорма = Неопределено;\n\n");

		// Трансляция каждого шага сценария
		List<VanessaStep> steps = scenario.getSteps();
		for (int i = 0; i < steps.size(); i++) {
			VanessaStep step = steps.get(i);
			sb.append("\t// Шаг ").append(i + 1).append(" (").append(step.getType().getKeyword()).append("): ")
			  .append(step.getNormalizedText()).append("\n");

			String bslStepCode = translateStepToBsl(step);
			sb.append(bslStepCode).append("\n");
		}

		sb.append("\t// Фиксация успешного завершения сценария\n");
		sb.append("\tСП_Протокол.ЗафиксироватьУспех(КонтекстТеста, \"Сценарий выполнен успешно\");\n\n");
		sb.append("КонецПроцедуры\n\n");

		// Вспомогательные методы подготовки/очистки если нужно
		sb.append("// Процедура вызывается перед выполнением теста\n");
		sb.append("Процедура ПередЗапускомТеста() Экспорт\n");
		sb.append("\t// Очистка или подготовка тестовых данных\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("// Процедура вызывается после завершения теста\n");
		sb.append("Процедура ПослеЗавершенияТеста() Экспорт\n");
		sb.append("\t// Закрытие открытых окон и сессий\n");
		sb.append("\tСП_ТестовыйКлиент.ЗакрытьВсеОкна();\n");
		sb.append("КонецПроцедуры\n\n");

		sb.append("#КонецОбласти\n");

		return sb.toString();
	}

	/**
	 * Генерирует XML метаданных (.mdo) для нового общего модуля 1C.
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
				+ "  <server>true</server>\n"
				+ "</mdclass:CommonModule>\n";
	}

	private String translateStepToBsl(VanessaStep step) {
		String text = step.getNormalizedText().toLowerCase();
		List<String> params = step.getParameters();
		StringBuilder bsl = new StringBuilder();

		if (text.contains("открываю") || text.contains("открыть") || text.contains("перехожу") || text.contains("open")) {
			String formOrNav = !params.isEmpty() ? params.get(0) : "Обработка.СП_КонсольТестов";
			bsl.append("\tФорма = СП_ТестовыйКлиент.ОткрытьФорму(\"").append(escapeBslString(formOrNav)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(Форма <> Неопределено, \"Форма '").append(escapeBslString(formOrNav)).append("' должна быть открыта\");");
		} else if (text.contains("нажимаю кнопку") || text.contains("нажать кнопку") || text.contains("кликнуть") || text.contains("click button")) {
			String btnName = !params.isEmpty() ? params.get(0) : "ОсновныеДействия";
			bsl.append("\tРезДействия = СП_ДействияКлиент.НажатьКнопку(Форма, \"").append(escapeBslString(btnName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("заполняю поле") || text.contains("ввожу") || text.contains("в поле") || text.contains("type into")) {
			String fieldName = params.size() > 0 ? params.get(0) : "ПолеВвода";
			String fieldValue = params.size() > 1 ? params.get(1) : "Тестовое значение";
			bsl.append("\tРезДействия = СП_ДействияКлиент.ЗаполнитьПоле(Форма, \"").append(escapeBslString(fieldName))
			   .append("\", \"").append(escapeBslString(fieldValue)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("устанавливаю флаг") || text.contains("снимаю флаг") || text.contains("чекбокс")) {
			String flagName = !params.isEmpty() ? params.get(0) : "Флаг";
			boolean val = !text.contains("снимаю");
			bsl.append("\tРезДействия = СП_ДействияКлиент.УстановитьФлаг(Форма, \"").append(escapeBslString(flagName))
			   .append("\", ").append(val ? "Истина" : "Ложь").append(");\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезДействия.ok, РезДействия.message);");
		} else if (text.contains("жду") || text.contains("ожидаю") || text.contains("wait")) {
			String elem = !params.isEmpty() ? params.get(0) : "ЭлементФормы";
			bsl.append("\tРезОжидания = СП_ОжиданияКлиент.ЖдатьПоявленияЭлемента(Форма, \"").append(escapeBslString(elem)).append("\", 10);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезОжидания.ok, \"Таймаут ожидания элемента: '").append(escapeBslString(elem)).append("'\");");
		} else if (text.contains("равно") || text.contains("содержит") || text.contains("проверяю") || text.contains("тогда поле") || text.contains("assert")) {
			String fieldName = params.size() > 0 ? params.get(0) : "Результат";
			String expectedValue = params.size() > 1 ? params.get(1) : "ОжидаемоеЗначение";
			bsl.append("\tФактическоеЗначение = СП_ИнспекторКлиент.ПолучитьЗначениеЭлемента(Форма, \"").append(escapeBslString(fieldName)).append("\");\n");
			bsl.append("\tСП_Утверждения.УтверждениеРавенство(\"").append(escapeBslString(expectedValue))
			   .append("\", ФактическоеЗначение, \"Поле '").append(escapeBslString(fieldName)).append("' содержит корректные данные\");");
		} else if (text.contains("таблиц") || text.contains("строк") || text.contains("table")) {
			String tabName = !params.isEmpty() ? params.get(0) : "Товары";
			bsl.append("\tРезТаблицы = СП_ДействияКлиент.НайтиСтрокуТаблицы(Форма, \"").append(escapeBslString(tabName)).append("\", Новый Структура);\n");
			bsl.append("\tСП_Утверждения.УтверждениеИстина(РезТаблицы.ok, РезТаблицы.message);");
		} else {
			// Универсальный вызов шага
			bsl.append("\t// Выполнение кастомного шага:\n");
			bsl.append("\tСП_Протокол.ЛогИнфо(КонтекстТеста, \"Выполнение: ").append(escapeBslString(step.getNormalizedText())).append("\");");
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
