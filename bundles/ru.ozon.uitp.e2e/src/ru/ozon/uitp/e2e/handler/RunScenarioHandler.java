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
import ru.ozon.uitp.e2e.views.BridgeScenario;

/**
 * Кнопка «Запустить сценарий» на главной панели EDT.
 *
 * <p>Запускает ЕДИНЫЙ контур без ручного шага: плагин генерирует runId, пишет
 * командный файл моста ({@code bridge-commands.json}), программно запускает тонкий
 * клиент АУФ (копия RuntimeClient-конфигурации со стартовой опцией автозапуска
 * BSL-моста + outDir и автовходом) и в фоновом Job ждёт результат
 * {@code bridge-result-<runId>.json} для этого же runId. Выполнение шагов на
 * реальном слое UI — дело BSL-агента; плагин лишь доставляет и контролирует
 * прогон.</p>
 *
 * <p>Проваленные assertions в результате не превращаются в успех: результат
 * моста несёт свой {@code status}, и при {@code failed} кнопка докладывает
 * не-зелёный исход. Аварийное завершение клиента до появления файла результата
 * НЕ маскируется старым отчётом (ждём только файла конкретного runId).</p>
 */
public class RunScenarioHandler extends AbstractHandler {

	private static final long RESULT_TIMEOUT_MS = 10 * 60 * 1000L; // 10 минут

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					File outDir = LaunchMonitor.outDir();

					// Единый контур: runId здесь же, командный файл пишем сами
					// (ручного запуска live-bridge-rally больше нет).
					String runId = LaunchMonitor.newRunId();
					LaunchMonitor.writeCommands(outDir, BridgeScenario.commandsJson(runId));
					LaunchMonitor.info("Командный файл моста записан: runId=" + runId);

					ILaunch launch = BridgeLaunchHelper.launchClient("run");
					if (launch == null) {
						return new Status(IStatus.ERROR, Activator.BUNDLE_ID,
								"Клиент передан на запуск, но ILaunch не найден по имени конфигурации "
										+ "(запуск мог не стартовать)", null);
					}
					String name = launch.getLaunchConfiguration() != null
							? launch.getLaunchConfiguration().getName() : "(без конфигурации)";
					LaunchMonitor.info("Тонкий клиент запущен: " + name);

					File result = LaunchMonitor.waitForBridgeResult(outDir, runId, launch, RESULT_TIMEOUT_MS);
					if (result != null) {
						String body = LaunchMonitor.readFileSafe(result);
						LaunchMonitor.info("Мост на реальном UI завершился: " + result.getAbsolutePath()
								+ " -> " + body);
						// Код 0 клиента не имеет значения: исход определяет статус результата.
						if (body.contains("\"status\":\"failed\"")) {
							return new Status(IStatus.WARNING, Activator.BUNDLE_ID,
									"Мост исполнился, но assertions провалены (runId=" + runId + "). "
											+ "См. " + result.getAbsolutePath());
						}
						return Status.OK_STATUS;
					}

					// Результата нет: либо конец таймаута, либо контролируемый процесс завершился
					// раньше файла результата (авария). Старый отчёт чужим runId не берём.
					int exit = LaunchMonitor.exitCodeOf(launch);
					String reason = exit != Integer.MIN_VALUE
							? "контролируемый процесс завершился с кодом " + exit + " без файла результата"
							: "таймаут ожидания результата моста (клиент не открыл форму/не исполнил)";
					LaunchMonitor.info("Результат моста не получен: " + reason);
					return new Status(IStatus.ERROR, Activator.BUNDLE_ID,
							"Результат моста не получен: " + reason, null);
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
