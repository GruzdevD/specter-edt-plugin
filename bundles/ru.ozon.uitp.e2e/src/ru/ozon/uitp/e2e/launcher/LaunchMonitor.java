package ru.ozon.uitp.e2e.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

/**
 * Следит за прогоном моста: дожидается появления файла результата
 * {@code bridge-result-*.json} в рабочей папке (агент BSL пишет его на реальном
 * слое UI) либо завершения процессов клиента.
 *
 * <p>Путь к рабочей папке берётся из системного свойства {@code uitp.e2e.outDir}
 * с разумным дефолтом под monorepo ui-test-platform (это константы сценария,
 * НЕ секреты).</p>
 */
public final class LaunchMonitor {

	/** Системное свойство с путём к рабочей папке моста. */
	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";

	private static final String DEFAULT_OUT_DIR =
			"/Users/dmigruzdev/git/ui-test-platform/packages/qa/e2e/out";

	private static final long POLL_MS = 1500L;

	private LaunchMonitor() {
	}

	/** Рабочая папка моста (из свойства или дефолт). */
	public static File outDir() {
		return new File(System.getProperty(OUT_DIR_PROPERTY, DEFAULT_OUT_DIR));
	}

	/**
	 * Ждёт результат моста для заданного процесса/лаунча.
	 *
	 * @param launch    запущенный клиент (может быть null, если неизвестен)
	 * @param timeoutMs максимальное время ожидания
	 * @return файл {@code bridge-result-*.json} либо найденный в outDir, либо null по таймауту
	 */
	public static File waitForBridgeResult(ILaunch launch, long timeoutMs) {
		File outDir = outDir();
		String[] before = listResultFiles(outDir);
		long startedAt = System.currentTimeMillis();
		while (System.currentTimeMillis() - startedAt < timeoutMs) {
			// 1) Появился новый файл результата — мост исполнился.
			String[] now = listResultFiles(outDir);
			if (now.length > before.length) {
				return Arrays.stream(now)
						.skip(before.length)
						.map(n -> new File(outDir, n))
						.findFirst()
						.orElse(null);
			}
			// 2) Клиент завершился (процессы термипализированы) — ждать больше нечего.
			if (isTerminated(launch)) {
				return null;
			}
			try {
				Thread.sleep(POLL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		return null;
	}

	private static boolean isTerminated(ILaunch launch) {
		if (launch == null) {
			return false;
		}
		IProcess[] procs = launch.getProcesses();
		if (procs == null || procs.length == 0) {
			return launch.isTerminated();
		}
		return Stream.of(procs).allMatch(IProcess::isTerminated);
	}

	private static String[] listResultFiles(File dir) {
		String[] files = dir.list((d, n) -> n != null && n.startsWith("bridge-result-") && n.endsWith(".json"));
		return files == null ? new String[0] : files;
	}

	/** Логирование в платформенный лог EDT. */
	public static void info(String msg) {
		ILog log = Platform.getLog(Platform.getBundle("ru.ozon.uitp.e2e"));
		if (log != null) {
			log.info(msg);
		}
	}
}
