package ru.ozon.uitp.e2e.views;

/**
 * Единый контур команд сценария моста (канон R1, один UI-сценарий на реальном
 * слое 1С без .epf). Используется и toolbar-командой {@code RunScenarioHandler},
 * и кнопкой «Запустить» на панели «Тесты», чтобы оба пути запускали один и тот же
 * сценарий и писали один формат команд.
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
	 * опционально одного теста внутри него) через движок {@code OZON_UI_Тестирование}.
	 *
	 * @param runId      runId этого прогона
	 * @param moduleName имя BSL-модуля набора (например {@code OZON_UI_Тесты_Контрагенты})
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
}
