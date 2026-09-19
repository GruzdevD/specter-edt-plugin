package ru.ozon.uitp.e2e.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель сценария тестирования Vanessa Automation (.feature).
 */
public class VanessaScenario {

	private String featureName;
	private String scenarioName;
	private String description;
	private File sourceFile;
	private List<String> tags = new ArrayList<>();
	private List<VanessaStep> steps = new ArrayList<>();
	private boolean selected = true;
	private String targetModuleName;

	public VanessaScenario(String featureName, String scenarioName, File sourceFile) {
		this.featureName = featureName;
		this.scenarioName = scenarioName;
		this.sourceFile = sourceFile;
		this.targetModuleName = generateDefaultModuleName(scenarioName);
	}

	public static String generateDefaultModuleName(String scenarioName) {
		if (scenarioName == null || scenarioName.trim().isEmpty()) {
			return "СП_Тест_Сценарий";
		}
		// Очищаем от спецсимволов для валидного идентификатора BSL
		String cleaned = scenarioName.replaceAll("[^a-zA-Zа-яА-Я0-9_]", "_")
				.replaceAll("_+", "_");
		if (cleaned.startsWith("_")) {
			cleaned = cleaned.substring(1);
		}
		if (cleaned.endsWith("_")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}
		if (!cleaned.startsWith("СП_Тест_")) {
			cleaned = "СП_Тест_" + cleaned;
		}
		// Ограничиваем длину имени модуля в метаданных 1C
		if (cleaned.length() > 64) {
			cleaned = cleaned.substring(0, 64);
		}
		return cleaned;
	}

	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
		this.targetModuleName = generateDefaultModuleName(scenarioName);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public List<String> getTags() {
		return tags;
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}

	public List<VanessaStep> getSteps() {
		return steps;
	}

	public void addStep(VanessaStep step) {
		this.steps.add(step);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getTargetModuleName() {
		return targetModuleName;
	}

	public void setTargetModuleName(String targetModuleName) {
		this.targetModuleName = targetModuleName;
	}

	public int getStepCount() {
		return steps.size();
	}
}
