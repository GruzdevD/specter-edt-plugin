package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Атрибуты launch-конфигурации Specter (тип {@link SpecterLaunchDelegate#TYPE_ID}).
 *
 * Конфигурация Specter НЕ описывает клиент сама — она ссылается на базовую
 * конфигурация запуска EDT (RuntimeClient: тонкий/толстый клиент с приложением,
 * пользователем и инфобазой) и добавляет параметры прогона Specter:
 * порт TESTMANAGER-режима (канал B, ADR-009).
 *
 * Схема запуска (по образцу YAxUnit ru.biatech.edt.junit):
 * delegate читает базовую конфигурацию, копирует её в рабочий клон
 * «Specter: <имя>», накладывает STARTUP_OPTION=SPECTER_START_BRIDGE,
 * режим клиент-тестирования (/TESTMANAGER) и параметры автовхода,
 * и запускает клон. Клон один — переиспользуется между прогонами.
 */
public final class SpecterLaunchAttributes {

	/** ID типа launch-конфигурации Specter (plugin.xml). */
	public static final String TYPE_ID = "ru.ozon.uitp.e2e.launcher.specter";

	/** Имя базовой конфигурации запуска EDT (RuntimeClient), обязательный атрибут. */
	public static final String BASE_LAUNCH_CONFIG = "ru.ozon.uitp.e2e.baseLaunchConfig";

	/** Порт TestClient/TESTMANAGER (канал B). Пусто/0 — не включать режим тестирования. */
	public static final String TESTING_PORT = "ru.ozon.uitp.e2e.testingPort";

	/**
	 * Имя клона базовой конфигурации, которым реально стартует клиент.
	 * Один клон на базу: не плодим конфигурации запуска.
	 */
	public static final String CLONE_PREFIX = "Specter: ";

	private SpecterLaunchAttributes() {
	}

	/** Имя базовой конфигурации запуска (ошибка CoreException, если атрибут пуст). */
	public static String baseLaunchConfigName(ILaunchConfiguration config) throws CoreException {
		String name = config.getAttribute(BASE_LAUNCH_CONFIG, ""); //$NON-NLS-1$
		if (name.isBlank()) {
			throw new CoreException(error("В конфигурации запуска Specter '" + config.getName()
					+ "' не указана базовая конфигурация запуска EDT (поле «Конфигурация запуска EDT»). "
					+ "Открой Run Configurations → Specter и выбери базовую конфигурацию."));
		}
		return name.trim();
	}

	/** Порт TESTMANAGER-режима; 0 = режим не включать. */
	public static int testingPort(ILaunchConfiguration config) throws CoreException {
		return config.getAttribute(TESTING_PORT, 0);
	}

	/** Префиксованное имя клона для базовой конфигурации. */
	public static String cloneName(String baseName) {
		return CLONE_PREFIX + baseName;
	}

	/**
	 * Готовит ВРЕМЕННЫЙ клон базовой конфигурации в памяти (working copy, БЕЗ
	 * сохранения в workspace — в Run Configurations пользовательская конфигурация
	 * всегда одна) и накладывает параметры прогона Specter.
	 * Запуск идёт от working copy: клиент стартует с параметрами моста,
	 * но конфигурация-клон нигде не persists (схема YAxUnit).
	 */
	public static ILaunchConfiguration prepareClone(ILaunchConfiguration base, String outDir,
			String user, String password, int port) throws CoreException {
		String targetName = cloneName(base.getName());

		// Клон живёт ТОЛЬКО в памяти: base.copy → working copy, doSave() НЕ вызываем.
		ILaunchConfigurationWorkingCopy wc = base.copy(targetName);

		// 1. Параметры моста Specter: маркер запуска агента в ПараметрЗапуске.
		String startupOption = BridgeLaunchHelper.STARTUP_OPTION
				+ BridgeLaunchHelper.STARTUP_OPTION_DELIM
				+ BridgeLaunchHelper.OUT_DIR_PARAM + outDir;
		wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.STARTUP_OPTION,
				startupOption);

		// 2. Автовход в ИБ (атрибуты базы уже скопированы).
		if (user != null && !user.isBlank()) {
			wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.LAUNCH_USER_NAME,
					user.trim());
		}
		if (password != null) {
			wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD,
					password);
		}

		// 3. Режим клиент-тестирования (канал B, ADR-009): ведущий сеанс /TESTMANAGER.
		applyTestingMode(wc, port);

		return wc;
	}

	/**
	 * Накладывает режим клиент-тестирования на клон: атрибуты
	 * ATTR_AUTOMATED_TESTING_MODE ("TESTMANAGER") и ATTR_AUTOMATED_TESTING_PORT_NUMBER (int).
	 * port &lt;= 0 — режим снимается (клон остался от прошлого тестового прогона).
	 */
	public static void applyTestingMode(ILaunchConfigurationWorkingCopy wc, int port) {
		if (port <= 0) {
			try {
				wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.AUTOMATED_TESTING_MODE,
						(String) null);
			} catch (Exception e) {
				ru.ozon.uitp.e2e.Activator.logError(
						"Specter: не удалось снять режим клиент-тестирования клона", e);
			}
			return;
		}
		wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.AUTOMATED_TESTING_MODE,
				"TESTMANAGER");
		wc.setAttribute(com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes.AUTOMATED_TESTING_PORT_NUMBER,
				port);
	}

	private static org.eclipse.core.runtime.IStatus error(String message) {
		return new org.eclipse.core.runtime.Status(
				org.eclipse.core.runtime.IStatus.ERROR, ru.ozon.uitp.e2e.Activator.BUNDLE_ID, message);
	}
}
