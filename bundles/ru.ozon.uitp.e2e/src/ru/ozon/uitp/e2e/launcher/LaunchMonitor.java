package ru.ozon.uitp.e2e.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import ru.ozon.uitp.e2e.Activator;

/**
 * Контроллер обмена и слежения за прогоном моста.
 * Обеспечивает антимаскировку по уникальному runId и поддержку отмены через IProgressMonitor.
 */
public final class LaunchMonitor {

	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";
	private static final String COMMANDS_FILE = "bridge-commands.json";
	private static final String RESULT_PREFIX = "bridge-result-";
	private static final String RESULT_SUFFIX = ".json";
	private static final long POLL_MS = 1000L;

	private LaunchMonitor() {
	}

	public static File outDir() throws CoreException {
		String out = System.getProperty(OUT_DIR_PROPERTY);
		if (out == null || out.isBlank()) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Не задано системное свойство -D" + OUT_DIR_PROPERTY
							+ " (каталог обмена моста). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		File dir = new File(out.trim());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public static String newRunId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public static void writeCommands(File outDir, String content) throws CoreException {
		File tmp = new File(outDir, COMMANDS_FILE + ".tmp");
		File target = new File(outDir, COMMANDS_FILE);
		try {
			Files.write(tmp.toPath(), content.getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Не удалось записать командный файл моста в " + target.getAbsolutePath(), e));
		}
	}

	/**
	 * Ожидает появления файла результата для конкретного runId с поддержкой отмены.
	 */
	public static File waitForBridgeResult(File outDir, String runId, ILaunch launch,
			long timeoutMs, IProgressMonitor monitor) {
		File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
		long startedAt = System.currentTimeMillis();

		while (System.currentTimeMillis() - startedAt < timeoutMs) {
			if (monitor != null && monitor.isCanceled()) {
				info("Ожидание моста прервано пользователем (runId=" + runId + ")");
				return null;
			}
			if (expected.isFile()) {
				return expected;
			}
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

	public static int exitCodeOf(ILaunch launch) {
		if (launch == null) return Integer.MIN_VALUE;
		IProcess[] procs = launch.getProcesses();
		if (procs == null) return Integer.MIN_VALUE;
		for (IProcess p : procs) {
			if (p.isTerminated()) {
				try {
					return p.getExitValue();
				} catch (org.eclipse.debug.core.DebugException e) {
					// игнорируем
				}
			}
		}
		return Integer.MIN_VALUE;
	}

	private static boolean isTerminated(ILaunch launch) {
		if (launch == null) return false;
		IProcess[] procs = launch.getProcesses();
		if (procs == null || procs.length == 0) return launch.isTerminated();
		return Stream.of(procs).allMatch(IProcess::isTerminated);
	}

	public static String readFileSafe(File f) {
		if (f == null || !f.isFile()) return "";
		for (int attempt = 0; attempt < 3; attempt++) {
			try {
				String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
				if (!content.isEmpty()) {
					return content;
				}
				Thread.sleep(30);
			} catch (IOException | InterruptedException e) {
				try {
					Thread.sleep(30);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		try {
			return Files.readString(f.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	public static void info(String msg) {
		ILog log = Platform.getLog(Platform.getBundle(Activator.BUNDLE_ID));
		if (log != null) {
			log.info(msg);
		}
	}
}