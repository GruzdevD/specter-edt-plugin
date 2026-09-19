package ru.ozon.uitp.e2e.views;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.BridgeLaunchHelper;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Исправленный оркестратор запуска живого моста в фоновом Eclipse Job.
 * 
 * Ключевые исправления:
 * 1. Синхронизирован runId: принимается точно тот runId, который сформирован для команд!
 * 2. Передан IProgressMonitor: отмена в Progress View немедленно прерывает ожидание.
 * 3. Безопасное обновление UI через Display.asyncExec с проверкой на завершение воркбенча.
 */
public final class BridgeRunner {

	public interface Callback {
		void onResult(BridgeResult result);
	}

	public interface ErrorCallback {
		void onError(String message);
	}

	private BridgeRunner() {
	}

	/**
	 * Запускает сценарий моста асинхронно в Eclipse Job.
	 *
	 * @param runId        единый уникальный идентификатор прогона
	 * @param commandsJson сформированное тело команд с этим же runId
	 * @param onResult     колбэк успеха (вызывается на UI-потоке)
	 * @param onError      колбэк ошибки (вызывается на UI-потоке)
	 */
	public static void runAsync(final String runId, final String commandsJson,
			final Callback onResult, final ErrorCallback onError) {
		Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Выполнение сценария 1С через UI-мост...", 100);
					File outDir = LaunchMonitor.outDir();

					// Записываем команды со строго согласованным runId
					LaunchMonitor.writeCommands(outDir, commandsJson);
					LaunchMonitor.info("Specter view: командный файл моста записан runId=" + runId);
					monitor.worked(15);

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					// Запуск клиента через хелпер
					ILaunch launch = BridgeLaunchHelper.launchClient("run", monitor);
					if (launch == null) {
						return reportError(runId, onError, "Клиент передан на запуск, но ILaunch не найден "
								+ "(запуск мог не стартовать)");
					}
					LaunchMonitor.info("Specter view: тонкий клиент запущен: "
							+ launch.getLaunchConfiguration().getName());
					monitor.worked(20);

					// Ожидаем результат именно для согласованного runId с поддержкой отмены
					File result = LaunchMonitor.waitForBridgeResult(
							outDir, runId, launch, BridgeScenario.RESULT_TIMEOUT_MS, monitor);

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					if (result == null) {
						int exit = LaunchMonitor.exitCodeOf(launch);
						String reason = exit != Integer.MIN_VALUE
								? "Контролируемый процесс 1С завершился аварийно с кодом " + exit
										+ " до записи файла результата (проверьте синтаксис BSL и журнал регистрации 1С)"
								: "Таймаут ожидания результата моста (" + (BridgeScenario.RESULT_TIMEOUT_MS / 1000) + " сек). Клиент 1С не вернул ответ.";
						LaunchMonitor.info("Specter view: результат не получен: " + reason);
						return reportError(runId, onError, reason);
					}
					monitor.worked(50);

					String body = LaunchMonitor.readFileSafe(result);
					BridgeResult br = BridgeResult.parse(body, result);
					if (br == null) {
						return reportError(runId, onError, "Результат моста прочитан, но не разобран ("
								+ result.getAbsolutePath() + ")");
					}
					LaunchMonitor.info("Specter view: мост завершился status=" + br.status
							+ " steps=" + br.steps.size());

					// Публикуем результат на UI-потоке
					dispatchToUI(() -> {
						BridgeResultStore.get().set(br);
						if (onResult != null) {
							onResult.onResult(br);
						}
					});
					monitor.worked(15);
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return reportError(runId, onError, "Не удалось запустить клиент АУФ: " + e.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private static IStatus reportError(final String runId, final ErrorCallback onError, final String message) {
		dispatchToUI(() -> {
			// Публикуем синтетический FAILED результат в BridgeResultStore, чтобы панель Результаты мгновенно отобразила ошибку
			BridgeResult errResult = BridgeResult.createErrorResult(runId, "Ошибка выполнения", message);
			BridgeResultStore.get().set(errResult);

			if (onError != null) {
				onError.onError(message);
			}
		});
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}

	private static void dispatchToUI(Runnable runnable) {
		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(runnable);
		}
	}
}