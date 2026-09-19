package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import ru.ozon.uitp.e2e.Activator;

/**
 * Хелпер программного запуска тонкого клиента 1С из EDT.
 * Оптимизирован для повторного использования копии конфигурации и гибкого сопоставления.
 */
public final class BridgeLaunchHelper {

	public static final String RUNTIME_CLIENT_TYPE_ID =
			"com._1c.g5.v8.dt.launching.core.RuntimeClient";
	public static final String DEFAULT_SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ";
	public static final String LAUNCH_CONFIG_PROPERTY = "uitp.e2e.launchConfig";

	public static final String STARTUP_OPTION = "OZONUI_START_BRIDGE";
	public static final String STARTUP_OPTION_DELIM = "|";
	public static final String OUT_DIR_PARAM = "outDir=";

	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";
	public static final String USER_NAME_PROPERTY = "uitp.e2e.launchUser";
	public static final String USER_PASSWORD_PROPERTY = "uitp.e2e.launchPassword";

	private static final String PREFIX = "UITP - ";

	private BridgeLaunchHelper() {
	}

	private static IStatus error(String message) {
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}

	private static String requiredProperty(String property, String what) throws CoreException {
		String value = System.getProperty(property);
		if (value == null || value.isBlank()) {
			throw new CoreException(error("Не задано системное свойство -D" + property
					+ " (" + what + "). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		return value.trim();
	}

	public static ILaunch launchClient(String mode, IProgressMonitor monitor) throws CoreException {
		ILaunchConfiguration source = findSourceConfiguration();

		String outDir = requiredProperty(OUT_DIR_PROPERTY, "каталог обмена моста");
		String user = requiredProperty(USER_NAME_PROPERTY, "пользователь ИБ для автовхода");
		String password = requiredProperty(USER_PASSWORD_PROPERTY, "пароль ИБ для автовхода");

		String targetName = PREFIX + source.getName();
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();

		// Ищем существующую копию, чтобы не размножать конфигурации в workspace:
		ILaunchConfiguration existing = null;
		for (ILaunchConfiguration c : lm.getLaunchConfigurations()) {
			if (targetName.equals(c.getName())) {
				existing = c;
				break;
			}
		}

		ILaunchConfigurationWorkingCopy wc = (existing != null)
				? existing.getWorkingCopy()
				: source.copy(targetName);

		wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION,
				STARTUP_OPTION + STARTUP_OPTION_DELIM + OUT_DIR_PARAM + outDir);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);

		ILaunchConfiguration copy = wc.doSave();
		String want = copy.getName();

		DebugUITools.launch(copy, mode);

		// Ждём появления ILaunch с контролем отмены
		for (int i = 0; i < 15; i++) {
			if (monitor != null && monitor.isCanceled()) {
				return null;
			}
			for (ILaunch l : lm.getLaunches()) {
				String name = l.getLaunchConfiguration() != null
						? l.getLaunchConfiguration().getName() : null;
				if (want.equals(name)) {
					return l;
				}
			}
			try {
				Thread.sleep(200L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return null;
	}

	public static ILaunchConfiguration findSourceConfiguration() throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType rtc = lm.getLaunchConfigurationType(RUNTIME_CLIENT_TYPE_ID);
		if (rtc == null) {
			throw new CoreException(error("Нет EDT-типа конфигурации RuntimeClient: " + RUNTIME_CLIENT_TYPE_ID));
		}

		ILaunchConfiguration[] configs = lm.getLaunchConfigurations(rtc);
		if (configs == null || configs.length == 0) {
			throw new CoreException(error("В проекте нет ни одной конфигурации типа RuntimeClient (1С)"));
		}

		// 1. Проверяем системное свойство
		String custom = System.getProperty(LAUNCH_CONFIG_PROPERTY);
		if (custom != null && !custom.isBlank()) {
			for (ILaunchConfiguration cfg : configs) {
				if (custom.trim().equals(cfg.getName())) return cfg;
			}
		}

		// 2. Ищем дефолтное имя «Тонкий клиент АУФ»
		for (ILaunchConfiguration cfg : configs) {
			if (DEFAULT_SOURCE_CONFIGURATION_NAME.equals(cfg.getName())) {
				return cfg;
			}
		}

		// 3. Fallback на первую доступную
		return configs[0];
	}
}