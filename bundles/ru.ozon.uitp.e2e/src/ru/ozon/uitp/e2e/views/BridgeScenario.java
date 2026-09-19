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
}
