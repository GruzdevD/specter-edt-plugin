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

		// ИБ должна быть синхронна с конфигурацией проекта ДО запуска: иначе EDT-делегат
		// поднимает модальное «Обновление приложения» и прогон висит (YAxUnit делает
		// тот же программный апдейт перед запуском, без модалок).
		updateInfobaseIfNeeded(base, monitor);

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

	/**
	 * Программно приводит ИБ проекта базовой конфигурации в состояние UPDATED
	 * (IApplicationManager.getUpdateState + update(INCREMENTAL)) — тот же путь,
	 * что у MCP-сервера edt-mcp (LaunchLifecycleUtils.updateApplicationIfNeeded).
	 * Если ИБ уже UPDATED — мгновенный no-op; ошибки логируются, но прогон
	 * НЕ прерывают: EDT-делегат в крайнем случае покажет свою модалку, как раньше.
	 */
	private static void updateInfobaseIfNeeded(ILaunchConfiguration base, IProgressMonitor monitor) {
		org.osgi.framework.ServiceReference<com.e1c.g5.dt.applications.IApplicationManager> ref = null;
		try {
			String projectName = base.getAttribute(
					"com._1c.g5.v8.dt.debug.core.ATTR_PROJECT_NAME", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (projectName.isEmpty()) {
				return;
			}
			// IApplicationManager — OSGi-сервис (см. EdtServices edt-mcp); берём через
			// сервисную ссылку, чтобы не зависеть от internal-activator'ов.
			org.osgi.framework.Bundle appBundle = org.osgi.framework.FrameworkUtil
					.getBundle(com.e1c.g5.dt.applications.IApplicationManager.class);
			if (appBundle == null || appBundle.getBundleContext() == null) {
				return;
			}
			ref = appBundle.getBundleContext()
					.getServiceReference(com.e1c.g5.dt.applications.IApplicationManager.class);
			com.e1c.g5.dt.applications.IApplicationManager appManager = ref != null
					? appBundle.getBundleContext().getService(ref) : null;
			if (appManager == null) {
				return;
			}
			org.eclipse.core.resources.IProject project =
					org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (!project.exists() || !project.isOpen()) {
				return;
			}
			com.e1c.g5.dt.applications.IApplication application =
					appManager.getDefaultApplication(project).orElse(null);
			if (application == null) {
				return;
			}
			com.e1c.g5.dt.applications.ApplicationUpdateState state =
					appManager.getUpdateState(application);
			if (state == com.e1c.g5.dt.applications.ApplicationUpdateState.UPDATED
					|| state == com.e1c.g5.dt.applications.ApplicationUpdateState.BEING_UPDATED) {
				return;
			}
			ru.ozon.uitp.e2e.Activator.logInfo("Specter: ИБ '" + projectName
					+ "' не синхронна (" + state + ") — программное обновление перед запуском");
			appManager.update(application,
					com.e1c.g5.dt.applications.ApplicationUpdateType.INCREMENTAL,
					null, monitor);
			ru.ozon.uitp.e2e.Activator.logInfo("Specter: программное обновление ИБ завершено");
		} catch (Exception e) {
			// Не блокируем прогон: EDT-делегат в крайнем случае спросит модалкой, как раньше.
			ru.ozon.uitp.e2e.Activator.logError(
					"Specter: программное обновление ИБ не удалось (прогон продолжен)", e);
		} finally {
			if (ref != null) {
				org.osgi.framework.FrameworkUtil
						.getBundle(com.e1c.g5.dt.applications.IApplicationManager.class)
						.getBundleContext().ungetService(ref);
			}
		}
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
