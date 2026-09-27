package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

/**
 * Точка выбора launch-контура прогонов Specter:
 *
 * 1. Если в workspace есть конфигурация типа {@link SpecterLaunchAttributes#TYPE_ID}
 *    («Specter UI-тесты», по образцу YAxUnit) — запускаем первую найденную:
 *    её delegate сам готовит клон базовой конфигурации с параметрами моста.
 * 2. Иначе — legacy-путь {@link BridgeLaunchHelper#launchClient} (-D-свойства).
 *
 * Оба пути возвращают ILaunch уже стартованного клона (ожидание появления
 * launch в менеджере встроено), либо null при отмене/неудаче.
 */
public final class SpecterLaunchSupport {

	private SpecterLaunchSupport() {
	}

	public static ILaunch launchOrLegacy(String mode, IProgressMonitor monitor) throws CoreException {
		ILaunchConfiguration specterConfig = findSpecterConfiguration();
		if (specterConfig != null) {
			return launchAndWait(specterConfig, mode, monitor);
		}
		return BridgeLaunchHelper.launchClient(mode, monitor);
	}

	/** Первая конфигурация типа Specter в workspace (или null). */
	public static ILaunchConfiguration findSpecterConfiguration() throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = lm.getLaunchConfigurationType(SpecterLaunchAttributes.TYPE_ID);
		if (type == null) {
			return null;
		}
		for (ILaunchConfiguration c : lm.getLaunchConfigurations(type)) {
			return c;
		}
		return null;
	}

	/**
	 * Запускает конфигурацию через её delegate (config.launch) и ждёт появления
	 * соответствующего ILaunch — прямая конфигурация Specter появляется в
	 * менеджере сразу под своим именем (клон стартует внутри delegate).
	 */
	private static ILaunch launchAndWait(ILaunchConfiguration config, String mode,
			IProgressMonitor monitor) throws CoreException {
		String want = config.getName();
		config.launch(mode, monitor);

		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		for (int i = 0; i < 150; i++) {
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
}
