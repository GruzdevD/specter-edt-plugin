package ru.ozon.uitp.e2e.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер файлов сценариев Vanessa Automation (.feature / Gherkin).
 * Поддерживает ключевые слова на русском и английском языках, теги, параметры и шаги.
 */
public class VanessaFeatureParser {

	private static final Pattern FEATURE_PATTERN = Pattern.compile("^\\s*(?:Функционал|Функция|Свойство|Feature)\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SCENARIO_PATTERN = Pattern.compile("^\\s*(?:Сценарий|Структура сценария|Scenario|Scenario Outline)\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_PATTERN = Pattern.compile("@([\\w\\-]+)");
	private static final Pattern STEP_PATTERN = Pattern.compile("^\\s*(Дано|Когда|Тогда|И|Но|Given|When|Then|And|But)\\s+(.*)$", Pattern.CASE_INSENSITIVE);

	/**
	 * Парсит .feature файл и возвращает список найденных сценариев.
	 */
	public List<VanessaScenario> parseFeatureFile(File file) throws IOException {
		List<VanessaScenario> scenarios = new ArrayList<>();
		if (!file.exists() || !file.isFile()) {
			return scenarios;
		}

		String featureName = file.getName().replaceFirst("[.][^.]+$", "");
		List<String> currentTags = new ArrayList<>();
		VanessaScenario currentScenario = null;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			int lineNum = 0;

			while ((line = reader.readLine()) != null) {
				lineNum++;
				String trimmed = line.trim();

				// Пропуск пустых строк и комментариев
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}

				// Поиск тегов (@smoke, @regress, etc.)
				if (trimmed.startsWith("@")) {
					Matcher tagMatcher = TAG_PATTERN.matcher(trimmed);
					while (tagMatcher.find()) {
						currentTags.add(tagMatcher.group(1));
					}
					continue;
				}

				// Поиск заголовка функционала (Feature:)
				Matcher featureMatcher = FEATURE_PATTERN.matcher(line);
				if (featureMatcher.find()) {
					featureName = featureMatcher.group(1).trim();
					continue;
				}

				// Поиск начала сценария (Сценарий:)
				Matcher scenarioMatcher = SCENARIO_PATTERN.matcher(line);
				if (scenarioMatcher.find()) {
					String scenarioName = scenarioMatcher.group(1).trim();
					currentScenario = new VanessaScenario(featureName, scenarioName, file);
					for (String tag : currentTags) {
						currentScenario.addTag(tag);
					}
					currentTags.clear();
					scenarios.add(currentScenario);
					continue;
				}

				// Поиск шагов (Дано, Когда, Тогда, И, Но)
				if (currentScenario != null) {
					Matcher stepMatcher = STEP_PATTERN.matcher(line);
					if (stepMatcher.find()) {
						String keyword = stepMatcher.group(1).toLowerCase();
						String stepText = stepMatcher.group(2).trim();

						VanessaStep.StepType type = mapStepType(keyword);
						VanessaStep step = new VanessaStep(type, stepText, lineNum);
						extractParameters(stepText, step);

						currentScenario.addStep(step);
					} else if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
						// Табличные данные шага
						if (!currentScenario.getSteps().isEmpty()) {
							VanessaStep lastStep = currentScenario.getSteps().get(currentScenario.getSteps().size() - 1);
							lastStep.addTableRow(trimmed);
						}
					}
				}
			}
		}

		return scenarios;
	}

	private VanessaStep.StepType mapStepType(String keyword) {
		switch (keyword) {
			case "дано":
			case "given":
				return VanessaStep.StepType.GIVEN;
			case "когда":
			case "when":
				return VanessaStep.StepType.WHEN;
			case "тогда":
			case "then":
				return VanessaStep.StepType.THEN;
			case "но":
			case "but":
				return VanessaStep.StepType.BUT;
			case "и":
			case "and":
			default:
				return VanessaStep.StepType.AND;
		}
	}

	private void extractParameters(String text, VanessaStep step) {
		// Извлечение параметров в кавычках: "..." или '...'
		Pattern paramPattern = Pattern.compile("[\"']([^\"']+)[\"']");
		Matcher matcher = paramPattern.matcher(text);
		while (matcher.find()) {
			step.addParameter(matcher.group(1));
		}
	}
}
