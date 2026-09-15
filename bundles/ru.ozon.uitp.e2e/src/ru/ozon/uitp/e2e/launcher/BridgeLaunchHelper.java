package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

import com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes;

/**
 * Программный запуск тонкого клиента АУФ из EDT: копирует существующую
 * RuntimeClient-конфигурацию («Тонкий клиент АУФ»), пишет в копию стартовую
 * опцию (маркер автозапуска моста BSL) и учётку для автовхода, затем запускает
 * копию через {@link DebugUITools#launch}.
 *
 * <p>Механизм повторяет yaxunit-плагин ({@code ru.biatech.edt.junit}):
 * `DebugPlugin.getLaunchManager().getLaunchConfigurations()`,
 * `ILaunchConfiguration.copy(name)`, `DebugUITools.launch(copy, "run")`.
 * EDT RuntimeClientDelegate сам подставит опцию в доп. параметры 1cv8c (/C) и
 * соберёт командную строку исполняемого клиента.</p>
 */
public final class BridgeLaunchHelper {

	/** Идентификатор EDT-типа конфигурации тонкого клиента. */
	public static final String RUNTIME_CLIENT_TYPE_ID =
			"com._1c.g5.v8.dt.launching.core.RuntimeClient";

	/** Имя существующей launch-конфигурации (файловый клиент АУФ afm). */
	public static final String SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ";

	/**
	 * Маркер автозапуска BSL-моста: модуль управляемого приложения расширения
	 * afm.OZON_UI при СтрНайти(ПараметрЗапуска, маркер) открывает ФормуСписка,
	 * чей ПриОткрытии исполняет команды моста на реальном UI.
	 */
	public static final String STARTUP_OPTION = "OZONUI_START_BRIDGE";

	/** Статический шаблон запуска (не secret — тестовая учётка afm api/1). */
	public static final String USER_NAME = "api";
	public static final String USER_PASSWORD = "1";

	private static final String PREFIX = "UITP - ";

	private BridgeLaunchHelper() {
	}

	private static IStatus error(String message) {
		return new Status(IStatus.ERROR, "ru.ozon.uitp.e2e", message);
	}

	/**
	 * Находит RuntimeClient-конфигурацию с именем {@link #SOURCE_CONFIGURATION_NAME},
	 * копирует её и запускает полученную копию в режиме {@code mode}.
	 *
	 * <p>{@link DebugUITools#launch} возвращает {@code void}; фактический
	 * {@link ILaunch} находим по имени скопированной конфигурации из менеджера
	 * запусков (как это делает yaxunit через getLaunches()).</p>
	 *
	 * @param mode режим запуска ("run" или "debug")
	 * @return запущенный {@link ILaunch} или {@code null}, если не удалось найти
	 * @throws CoreException если конфигурация-источник не найдена или запуск не удался
	 */
	public static ILaunch launchClient(String mode) throws CoreException {
		ILaunchConfiguration source = findSourceConfiguration();
		ILaunchConfigurationWorkingCopy wc = source.copy(PREFIX + source.getName());
		wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION, STARTUP_OPTION);
		// Автовход: RuntimeClientDelegate передаст /N /P в 1cv8c — клиент не
		// застрянет на окне входа (иначе GUI-запуск блокируется диалогом).
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, USER_NAME);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, USER_PASSWORD);
		ILaunchConfiguration copy = wc.doSave();
		String want = copy.getName();
		DebugUITools.launch(copy, mode);
		// Ждём появления ILaunch с нужным именем (несколько циклов).
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		for (int i = 0; i < 10; i++) {
			for (ILaunch l : lm.getLaunches()) {
				String name = l.getLaunchConfiguration() != null
						? l.getLaunchConfiguration().getName() : null;
				if (want.equals(name)) {
					return l;
				}
			}
			try {
				Thread.sleep(300L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return null;
	}

	/**
	 * Ищет RuntimeClient-конфигурацию по имени {@link #SOURCE_CONFIGURATION_NAME}.
	 *
	 * @return исходная launch-конфигурация
	 * @throws CoreException если не найдена
	 */
	public static ILaunchConfiguration findSourceConfiguration() throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType rtc = lm.getLaunchConfigurationType(RUNTIME_CLIENT_TYPE_ID);
		if (rtc == null) {
			throw new CoreException(error("Нет EDT-типа конфигурации RuntimeClient: " + RUNTIME_CLIENT_TYPE_ID));
		}
		for (ILaunchConfiguration cfg : lm.getLaunchConfigurations(rtc)) {
			if (SOURCE_CONFIGURATION_NAME.equals(cfg.getName())) {
				return cfg;
			}
		}
		throw new CoreException(error("Не найдена launch-конфигурация «" + SOURCE_CONFIGURATION_NAME + "»"));
	}
}
