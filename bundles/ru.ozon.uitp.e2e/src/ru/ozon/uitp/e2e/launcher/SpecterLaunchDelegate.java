package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * Delegate launch-конфигурации Specter (по образцу YAxUnit
 * ru.biatech.edt.junit.launcher.v8.LaunchConfigurationDelegate).
 *
 * Конфигурация Specter ссылается на базовую конфигурацию запуска EDT
 * ({@link SpecterLaunchAttributes#BASE_LAUNCH_CONFIG}); запуск готовит
 * единственный рабочий клон «Specter: <база>» с параметрами моста
 * (SPECTER_START_BRIDGE, автовход, /TESTMANAGER) и стартует его.
 *
 * Запуск идёт напрямую через {@code clone.launch(mode, monitor)} — не через
 * {@code DebugUITools.launch}, чтобы не открывались модальные диалоги
 * (совпадает с практикой MCP-сервера edt-mcp).
 */
public class SpecterLaunchDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {

		String baseName = SpecterLaunchAttributes.baseLaunchConfigName(config);
		ILaunchManager lm = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration base = findByName(lm, baseName);
		if (base == null) {
			throw new CoreException(error("Базовая конфигурация запуска EDT '" + baseName
					+ "' не найдена (конфигурация Specter '" + config.getName()
					+ "'). Создай её в EDT (Run → Run Configurations → 1С) или выбери "
					+ "другую в настройках конфигурации Specter."));
		}

		// Параметры прогона: каталог обмена и порт TESTMANAGER берутся из
		// системных свойств плагина (-Duitp.e2e.outDir), порт — из атрибута конфигурации.
		String outDir = LaunchMonitor.outDir().getAbsolutePath();
		String user = System.getProperty(BridgeLaunchHelper.USER_NAME_PROPERTY, ""); //$NON-NLS-1$
		String password = System.getProperty(BridgeLaunchHelper.USER_PASSWORD_PROPERTY, ""); //$NON-NLS-1$
		int port = SpecterLaunchAttributes.testingPort(config);

		ILaunchConfiguration clone = SpecterLaunchAttributes.prepareClone(
				base, outDir, user, password, port);

		clone.launch(mode, monitor);
	}

	private static ILaunchConfiguration findByName(ILaunchManager lm, String name) throws CoreException {
		for (ILaunchConfiguration c : lm.getLaunchConfigurations()) {
			if (name.equals(c.getName())) {
				return c;
			}
		}
		return null;
	}

	private static org.eclipse.core.runtime.IStatus error(String message) {
		return new org.eclipse.core.runtime.Status(
				org.eclipse.core.runtime.IStatus.ERROR, ru.ozon.uitp.e2e.Activator.BUNDLE_ID, message);
	}
}
