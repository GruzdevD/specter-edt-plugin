package ru.ozon.uitp.e2e.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель результата прогона BSL-моста с лёгким JSON-парсером без внешних зависимостей.
 *
 * <p>Контракт агента (Catalogs/Контрагенты/Forms/ФормаСписка/Module.bsl,
 * {@code ЗаписатьРезультатНаСервере}):
 * <pre>
 * { "runId": "&lt;id&gt;", "status": "passed|failed",
 *   "steps": [ { "id": "c1", "action": "openList", "status": "passed|failed", "detail": "..." } ] }
 * </pre>
 * Парсер написан вручную (рекурсивный спуск) намеренно: плагин собирается против
 * локального p2-пула 1С, где нет json-библиотек, а тянуть отдельную зависимость
 * в Require-Bundle не хочется ради одного чтения файла результата.</p>
 */
public final class BridgeResult {

	/** Статус всего прогона: passed или failed. */
	public final String runId;
	public final String status;
	public final List<Step> steps;
	public final File source;

	public BridgeResult(String runId, String status, List<Step> steps, File source) {
		this.runId = runId;
		this.status = status;
		this.steps = steps == null ? new ArrayList<>() : steps;
		this.source = source;
	}

	/**
	 * Создаёт синтетический результат ошибки (например, сбой компиляции, ошибка запуска, таймаут).
	 */
	public static BridgeResult createErrorResult(String runId, String action, String errorDetail) {
		List<Step> steps = new ArrayList<>();
		steps.add(new Step("err-1", action == null || action.isEmpty() ? "Execution" : action, "failed", errorDetail));
		return new BridgeResult(runId == null ? "error" : runId, "failed", steps, null);
	}

	/** Один шаг (команда) сценария и его исход. */
	public static final class Step {
		public final String id;
		public final String action;
		public final String status;
		public final String detail;

		Step(String id, String action, String status, String detail) {
			this.id = id == null ? "" : id;
			this.action = action == null ? "" : action;
			this.status = status == null ? "" : status;
			this.detail = detail == null ? "" : detail;
		}

		public boolean isFailed() {
			return "failed".equals(status);
		}
	}

	/** Сводка: сколько шагов прошло / провалено. */
	public int passedCount() {
		int n = 0;
		for (Step s : steps) {
			if (!s.isFailed()) {
				n++;
			}
		}
		return n;
	}

	public int failedCount() {
		return steps.size() - passedCount();
	}

	public boolean isFailed() {
		return "failed".equals(status);
	}

	@Override
	public String toString() {
		return "BridgeResult{" + "runId='" + runId + "', status='" + status
				+ "', steps=" + steps.size() + '}';
	}

	// ---------------------------------------------------------------------
	// Парсер
	// ---------------------------------------------------------------------

	/**
	 * Разбирает тело результата моста.
	 *
	 * @param body   JSON-строка файла результата
	 * @param source файл, из которого прочитан (для навигации), может быть null
	 * @return модель результата; {@code null}, если разобрать не удалось
	 */
	public static BridgeResult parse(String body, File source) {
		if (body == null) {
			return null;
		}
		try {
			Parser p = new Parser(body);
			p.ws();
			if (!p.peek('{')) {
				return null;
			}
			String runId = null;
			String status = null;
			List<Step> steps = new ArrayList<>();
			p.next(); // {
			while (true) {
				p.ws();
				if (p.peek('}')) {
					p.next();
					break;
				}
				String key = p.parseString();
				p.ws();
				if (!p.peek(':')) {
					return null;
				}
				p.next();
				p.ws();
				switch (key) {
					case "runId":
						runId = p.parseString();
						break;
					case "status":
						status = p.parseString();
						break;
					case "steps":
						p.ws();
						if (p.peek('[')) {
							p.next();
							while (true) {
								p.ws();
								if (p.peek(']')) {
									p.next();
									break;
								}
								steps.add(parseStep(p));
								p.ws();
								if (p.peek(',')) {
									p.next();
								}
							}
						}
						break;
					default:
						p.skipValue();
						break;
				}
				p.ws();
				if (p.peek(',')) {
					p.next();
				}
			}
			return new BridgeResult(runId == null ? "" : runId,
					status == null ? "unknown" : status, steps, source);
		} catch (Exception e) {
			return null;
		}
	}

	private static Step parseStep(Parser p) {
		String id = "";
		String action = "";
		String status = "";
		String detail = "";
		p.next(); // {
		while (true) {
			p.ws();
			if (p.peek('}')) {
				p.next();
				break;
			}
			String key = p.parseString();
			p.ws();
			if (!p.peek(':')) {
				return new Step(id, action, status, detail);
			}
			p.next();
			p.ws();
			switch (key) {
				case "id":
					id = p.parseString();
					break;
				case "action":
					action = p.parseString();
					break;
				case "status":
					status = p.parseString();
					break;
				case "detail":
					detail = p.parseString();
					break;
				default:
					p.skipValue();
					break;
			}
			p.ws();
			if (p.peek(',')) {
				p.next();
			}
		}
		return new Step(id, action, status, detail);
	}

	/** Минимальный JSON-токенизатор, достаточный для нашего контракта. */
	private static final class Parser {
		private final String s;
		private int i;

		Parser(String s) {
			this.s = s;
		}

		void ws() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
				i++;
			}
		}

		boolean peek(char c) {
			return i < s.length() && s.charAt(i) == c;
		}

		void next() {
			if (i < s.length()) {
				i++;
			}
		}

		String parseString() {
			ws();
			if (i >= s.length() || s.charAt(i) != '"') {
				throw new IllegalStateException("ожидалась строка");
			}
			i++; // открывающая кавычка
			StringBuilder sb = new StringBuilder();
			while (i < s.length()) {
				char c = s.charAt(i);
				if (c == '\\') {
					// экранирование: \\uXXXX (unicode) и простые escape (\", \\, \/ — 1С пишет \/)
					i++;
					if (i >= s.length()) {
						break;
					}
					char e = s.charAt(i);
					switch (e) {
						case 'u':
							sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
							i += 4;
							break;
						case 'n':
							sb.append('\n');
							break;
						case 't':
							sb.append('\t');
							break;
						case 'r':
							sb.append('\r');
							break;
						default:
							sb.append(e);
							break;
					}
					i++;
				} else if (c == '"') {
					i++;
					return sb.toString();
				} else {
					sb.append(c);
					i++;
				}
			}
			throw new IllegalStateException("незакрытая строка");
		}

		/** Пропускает произвольное значение (для неизвестных ключей). */
		void skipValue() {
			ws();
			if (i >= s.length()) {
				return;
			}
			char c = s.charAt(i);
			if (c == '"') {
				parseString();
				return;
			}
			if (c == '{') {
				int depth = 0;
				while (i < s.length()) {
					char c2 = s.charAt(i);
					if (c2 == '"') {
						parseString();
						continue;
					}
					if (c2 == '{') {
						depth++;
					} else if (c2 == '}') {
						depth--;
						i++;
						if (depth == 0) {
							return;
						}
						continue;
					} else if (c2 == '[') {
						depth++;
					} else if (c2 == ']') {
						depth--;
					}
					i++;
				}
				return;
			}
			if (c == '[') {
				int depth = 0;
				while (i < s.length()) {
					char c2 = s.charAt(i);
					if (c2 == '"') {
						parseString();
						continue;
					}
					if (c2 == '[') {
						depth++;
					} else if (c2 == ']') {
						depth--;
						i++;
						if (depth == 0) {
							return;
						}
						continue;
					} else if (c2 == '{') {
						depth++;
					} else if (c2 == '}') {
						depth--;
					}
					i++;
				}
				return;
			}
			// литерал / число
			while (i < s.length()) {
				char c2 = s.charAt(i);
				if (c2 == ',' || c2 == '}' || c2 == ']') {
					break;
				}
				i++;
			}
		}
	}
}
