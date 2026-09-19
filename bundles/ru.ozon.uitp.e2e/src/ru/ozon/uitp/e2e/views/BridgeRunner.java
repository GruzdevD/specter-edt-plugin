package ru.ozon.uitp.e2e.views;

import java.io.File;

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
 * Живой прогон моста на реальном UI 1С, исполняемый в фоновом {@link Job}.
 *
 * <p>Единый контур: плагин генерирует runId, пишет командный файл моста
 * ({@code bridge-commands.json}), программно запускает тонкий клиент АУФ (копия
 * RuntimeClient-конфигурации со стартовой опцией автозапуска BSL-моста + outDir +
 * автовход) и ждёт результат {@code bridge-result-<runId>.json} для этого runId.
 * По завершении результат парсится и публикуется в {@link BridgeResultStore},
 * после чего панели «Тесты» и «Результаты» обновляются.</p>
 *
 * <p>Аварийное завершение клиента до появления файла результата НЕ маскируется
 * старым отчётом: ждём только файла конкретного runId (антимаскировка).</p>
 */
public final class BridgeRunner {

	/** Колбэк успешного завершения (результат моста получен и опубликован). */
	public interface Callback {
		void onResult(BridgeResult result);
	}

	/** Колбэк ошибки/отсутствия результата. */
	public interface ErrorCallback {
		void onError(String message);
	}

	private BridgeRunner() {
	}

	/**
	 * Запускает живой сценарий моста асинхронно.
	 *
	 * @param commandsJson JSON-тело команд (см. {@link BridgeScenario#commandsJson})
	 * @param onResult     вызывается на UI-потоке при получении результата
	 * @param onError      вызывается на UI-потоке при ошибке/таймауте
	 */
	public static void runAsync(final String commandsJson,
			final Callback onResult, final ErrorCallback onError) {
		Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					File outDir = LaunchMonitor.outDir();
					String runId = LaunchMonitor.newRunId();
					LaunchMonitor.writeCommands(outDir, commandsJson);
					LaunchMonitor.info("Specter view: командный файл моста записан runId=" + runId);

					ILaunch launch = BridgeLaunchHelper.launchClient("run");
					if (launch == null) {
						return reportError(onError, "Клиент передан на запуск, но ILaunch не найден "
								+ "(запуск мог не стартовать)");
					}
					LaunchMonitor.info("Specter view: тонкий клиент запущен: "
							+ launch.getLaunchConfiguration().getName());

					File result = LaunchMonitor.waitForBridgeResult(
							outDir, runId, launch, BridgeScenario.RESULT_TIMEOUT_MS);
					if (result == null) {
						int exit = LaunchMonitor.exitCodeOf(launch);
						String reason = exit != Integer.MIN_VALUE
								? "контролируемый процесс завершился с кодом " + exit
										+ " без файла результата"
								: "таймаут ожидания результата моста";
						LaunchMonitor.info("Specter view: результат не получен: " + reason);
						return reportError(onError, "Результат моста не получен: " + reason);
					}

					String body = LaunchMonitor.readFileSafe(result);
					BridgeResult br = BridgeResult.parse(body, result);
					if (br == null) {
						return reportError(onError, "Результат моста прочитан, но не разобран ("
								+ result.getAbsolutePath() + ")");
					}
					LaunchMonitor.info("Specter view: мост завершился status=" + br.status
							+ " steps=" + br.steps.size());
					// Публикуем на UI-потоке, чтобы панели обновились корректно.
					org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
						BridgeResultStore.get().set(br);
						if (onResult != null) {
							onResult.onResult(br);
						}
					});
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return reportError(onError, "Не удалось запустить клиент АУФ: " + e.getMessage());
				}
			}
		};
		job.schedule();
	}

	private static IStatus reportError(final ErrorCallback onError, final String message) {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
			if (onError != null) {
				onError.onError(message);
			}
		});
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}
}
