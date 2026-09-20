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
	private boolean skipped = false;
	private String skipReason = "";

	public VanessaScenario(String featureName, String scenarioName, File sourceFile) {
		this.featureName = featureName;
		this.scenarioName = scenarioName;
		this.sourceFile = sourceFile;
		this.targetModuleName = generateDefaultModuleName(scenarioName);
	}

	public boolean isSkipped() {
		return skipped;
	}

	public String getSkipReason() {
		return skipReason;
	}

	public static String generateDefaultModuleName(String scenarioName) {
		if (scenarioName == null || scenarioName.trim().isEmpty()) {
			return "СП_Тест_Сценарий_Клиент";
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
		// Сконвертированные наборы исполняются в КЛИЕНТСКОМ контуре
		// (СП_ТестированиеКлиент / реальный UI 1C): модельный чек EDT
		// common-module-name-client требует суффикс "Клиент/Client".
		String suffix = "_Клиент";
		if (!cleaned.endsWith(suffix)) {
			// Ограничиваем длину имени модуля в метаданных 1C (до 64).
			int maxBase = 64 - suffix.length();
			if (cleaned.length() > maxBase) {
				cleaned = cleaned.substring(0, maxBase);
			}
			while (cleaned.endsWith("_")) {
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			}
			cleaned = cleaned + suffix;
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
		checkSkipTag(tag);
	}

	private void checkSkipTag(String tag) {
		if (tag == null) return;
		String lower = tag.toLowerCase();
		if (lower.startsWith("skip") || lower.startsWith("ignore") || lower.startsWith("bug")) {
			this.skipped = true;
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\"']([^\"']+)[\"']").matcher(tag);
			if (m.find()) {
				this.skipReason = m.group(1);
			} else {
				int idx = tag.indexOf('-');
				if (idx > 0 && idx < tag.length() - 1) {
					this.skipReason = tag.substring(idx + 1);
				} else {
					this.skipReason = tag;
				}
			}
		}
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
