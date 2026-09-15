package ru.ozon.uitp.e2e.handler;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;

import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.BridgeLaunchHelper;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Кнопка «Запустить сценарий» на главной панели EDT.
 *
 * <p>Запускает тонкий клиент АУФ программно (копия RuntimeClient-конфигурации
 * со стартовой опцией автозапуска BSL-моста и автовходом), затем в фоновом Job
 * ждёт результат моста на реальном UI ({@code bridge-result-*.json}).</p>
 */
public class RunScenarioHandler extends AbstractHandler {

	private static final long RESULT_TIMEOUT_MS = 10 * 60 * 1000L; // 10 минут

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("UITP: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ILaunch launch = BridgeLaunchHelper.launchClient("run");
					String name = launch.getLaunchConfiguration() != null
							? launch.getLaunchConfiguration().getName() : "(без конфигурации)";
					LaunchMonitor.info("Тонкий клиент запущен: " + name);
					File result = LaunchMonitor.waitForBridgeResult(launch, RESULT_TIMEOUT_MS);
					if (result != null) {
						LaunchMonitor.info("Мост на реальном UI завершился: " + result.getAbsolutePath());
						return Status.OK_STATUS;
					}
					LaunchMonitor.info("Таймаут ожидания результата моста (процесс мог не открыть форму)");
					return new Status(IStatus.WARNING, Activator.BUNDLE_ID,
							"Таймаут ожидания результата моста", null);
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, Activator.BUNDLE_ID,
							"Не удалось запустить клиент АУФ: " + e.getMessage(), e);
				}
			}
		};
		job.schedule();
		return null;
	}
}
