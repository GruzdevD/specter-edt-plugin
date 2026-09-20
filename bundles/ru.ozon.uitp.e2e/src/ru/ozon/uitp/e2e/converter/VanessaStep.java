package ru.ozon.uitp.e2e.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель отдельного шага сценария Vanessa Automation.
 */
public class VanessaStep {

	public enum StepType {
		GIVEN("Дано"),
		WHEN("Когда"),
		THEN("Тогда"),
		AND("И"),
		BUT("Но");

		private final String keyword;

		StepType(String keyword) {
			this.keyword = keyword;
		}

		public String getKeyword() {
			return keyword;
		}
	}

	/**
	 * Вид блочного маркера в сценарии (для раскрытия макросов на уровне конвертера).
	 * Обычные шаги имеют NONE. Маркеры Если/Иначе/КонецЕсли/Пока не являются
	 * действиями — это структурная разметка, которую сохраняет парсер, чтобы
	 * конвертер мог разрешить условную ветвь константой (значение параметра макроса).
	 */
	public enum BlockType {
		NONE,
		IF,
		ELSE,
		END_IF,
		WHILE
	}

	private StepType type;
	private String rawText;
	private String normalizedText;
	private List<String> parameters = new ArrayList<>();
	private List<String> tableRows = new ArrayList<>();
	private int lineNumber;
	private BlockType blockType = BlockType.NONE;

	public VanessaStep(StepType type, String rawText, int lineNumber) {
		this.type = type;
		this.rawText = rawText;
		this.normalizedText = rawText != null ? rawText.trim() : "";
		this.lineNumber = lineNumber;
	}

	public StepType getType() {
		return type;
	}

	public void setType(StepType type) {
		this.type = type;
	}

	public String getRawText() {
		return rawText;
	}

	public String getNormalizedText() {
		return normalizedText;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void addParameter(String parameter) {
		this.parameters.add(parameter);
	}

	public List<String> getTableRows() {
		return tableRows;
	}

	public void addTableRow(String row) {
		this.tableRows.add(row);
	}

	public BlockType getBlockType() {
		return blockType;
	}

	public void setBlockType(BlockType blockType) {
		this.blockType = blockType;
	}
}
