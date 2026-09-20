package ru.ozon.uitp.e2e.views;

import java.util.List;

/**
 * Единый контур команд сценария моста (канон R1, один UI-сценарий на реальном
 * слое 1С без .epf). Используется кнопкой «Запустить мост» на панели «Тесты» и
 * запуском набора из редактора, чтобы все пути запускали один и тот же сценарий и
 * писали один формат команд.
 *
 * <p>Контракт BSL-агента (Catalogs/Контрагенты/Forms/ФормаСписка/Module.bsl):
 * {@code {runId, commands:[{id,action,target,property?,expected?,kind?,subject?,value?}]}}.
 * Набор команд — канон R1 (открыть список → открыть карточку → изменить ИНН →
 * проверить → «Записать» → форма не модифицирована → закрыть). Действия карточки
 * исполняются реальным слоем UI (модуль ФормаЭлемента), не серверной подменой.</p>
 */
public final class BridgeScenario {

	/** Таймаут ожидания результата моста, мс (10 минут). */
	public static final long RESULT_TIMEOUT_MS = 10 * 60 * 1000L;

	private BridgeScenario() {
	}

	/**
	 * JSON-тело командного файла для конкретного runId (единый канон R1).
	 */
	public static String commandsJson(String runId) {
		return "{\"runId\":\"" + runId + "\",\"commands\":["
				+ "{\"id\":\"c1\",\"action\":\"openList\",\"target\":\"Список\"},"
				+ "{\"id\":\"c2\",\"action\":\"openCard\"},"
				+ "{\"id\":\"c3\",\"action\":\"setValue\",\"target\":\"ИНН\",\"value\":\"7701234567\"},"
				+ "{\"id\":\"c4\",\"action\":\"assertValue\",\"target\":\"ИНН\",\"expected\":\"7701234567\"},"
				+ "{\"id\":\"c5\",\"action\":\"click\",\"target\":\"Записать\",\"kind\":\"command\"},"
				+ "{\"id\":\"c6\",\"action\":\"assert\",\"subject\":\"card\",\"target\":\"Форма\",\"property\":\"Модифицированность\",\"expected\":false},"
				+ "{\"id\":\"c7\",\"action\":\"click\",\"target\":\"Закрыть\",\"kind\":\"command\"}"
				+ "]}";
	}

	/**
	 * JSON-тело командного файла для запуска конкретного тестового набора (и
	 * опционально одного теста внутри него) через движок {@code СП_Тестирование}.
	 *
	 * @param runId      runId этого прогона
	 * @param moduleName имя BSL-модуля набора (например {@code СП_Тесты_Контрагенты})
	 * @param testName   имя конкретного теста набора (может быть empty = весь набор)
	 * @return JSON-тело {@code {runId, commands:[{action:"runSet", target, test?}]}}
	 */
	public static String runSetJson(String runId, String moduleName, String testName) {
		String target = moduleName == null ? "" : moduleName;
		String cmd;
		if (testName == null || testName.isEmpty()) {
			cmd = "{\"id\":\"r1\",\"action\":\"runSet\",\"target\":\"" + target + "\"}";
		} else {
			cmd = "{\"id\":\"r1\",\"action\":\"runSet\",\"target\":\"" + target
					+ "\",\"test\":\"" + testName + "\"}";
		}
		return "{\"runId\":\"" + runId + "\",\"commands\":[" + cmd + "]}";
	}

	/** Одна цель запуска (Annotation Discovery): модуль + тест-метод. */
	public static final class Target {
		public final String module;
		public final String method;

		public Target(String module, String method) {
			this.module = module;
			this.method = method;
		}
	}

	/**
	 * JSON-тело командного файла с явным списком целей (Annotation Discovery, P0).
	 *
	 * <p>Плагин собирает цели парсингом //&Тест/@test в исходниках BSL на build-time
	 * и передаёт их мосту как {@code targets:[{module, method}]}. Движок
	 * СП_ТестированиеКлиент.ЗапуститьМодульПоЦелям выполняет их динамически в
	 * защитном блоке — без контрактных СписокТестов()/ЗапуститьНаборТестов()
	 * в тестовом модуле.</p>
	 *
	 * @param runId   runId этого прогона
	 * @param targets список целей {module, method}
	 * @return JSON-тело {@code {runId, targets:[...]}}
	 */
	public static String runTargetsJson(String runId, List<Target> targets) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"runId\":\"").append(runId).append("\",\"targets\":[");
		if (targets != null) {
			for (int i = 0; i < targets.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				Target t = targets.get(i);
				sb.append("{\"module\":\"").append(t.module)
				  .append("\",\"method\":\"").append(t.method).append("\"}");
			}
		}
		sb.append("]}");
		return sb.toString();
	}
}
