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
	public static final String STARTUP_OPTION_DELIM = "|";
	public static final String OUT_DIR_PARAM = "outDir=";

	/** Системные свойства конфигурации: задаются в среде EDT (eclipse.ini / -D). */
	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";
	public static final String USER_NAME_PROPERTY = "uitp.e2e.launchUser";
	public static final String USER_PASSWORD_PROPERTY = "uitp.e2e.launchPassword";

	private static final String PREFIX = "UITP - ";

	private BridgeLaunchHelper() {
	}

	private static IStatus error(String message) {
		return new Status(IStatus.ERROR, "ru.ozon.uitp.e2e", message);
	}

	/**
	 * Читает обязательное системное свойство конфигурации ({@code -Dname=value}).
	 *
	 * @param property имя системного свойства
	 * @param what     человеко-читаемое описание («каталог обмена», «пользователь ИБ»)
	 * @return значение свойства
	 * @throws CoreException если свойство не задано — запуск без каталога обмена или
	 *                       без учётной записи недопустим (иначе клиент уйдёт на скрытый
	 *                       локальный путь или застрянет на окне входа)
	 */
	private static String requiredProperty(String property, String what) throws CoreException {
		String value = System.getProperty(property);
		if (value == null || value.isBlank()) {
			throw new CoreException(error("Не задано системное свойство -D" + property
					+ " (" + what + "). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		return value.trim();
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
		// Каталог обмена и учётка — из среды EDT, НЕ хардкод и НЕ в репо.
		String outDir = requiredProperty(OUT_DIR_PROPERTY, "каталог обмена моста");
		String user = requiredProperty(USER_NAME_PROPERTY, "пользователь ИБ для автовхода");
		String password = requiredProperty(USER_PASSWORD_PROPERTY, "пароль ИБ для автовхода");

		ILaunchConfigurationWorkingCopy wc = source.copy(PREFIX + source.getName());
		// Стартовая опция: маркер автозапуска BSL-моста + каталог обмена.
		// RuntimeClientDelegate передаст её в доп. параметры 1cv8c (/C).
		wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION,
				STARTUP_OPTION + STARTUP_OPTION_DELIM + OUT_DIR_PARAM + outDir);
		// Автовход: RuntimeClientDelegate передаст /N /P в 1cv8c — клиент не
		// застрянет на окне входа (иначе GUI-запуск блокируется диалогом).
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);
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
