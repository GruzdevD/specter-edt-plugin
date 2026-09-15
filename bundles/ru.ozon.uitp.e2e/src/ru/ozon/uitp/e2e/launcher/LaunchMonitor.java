package ru.ozon.uitp.e2e.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

/**
 * Следит за прогоном моста: дожидается появления файла результата
 * {@code bridge-result-<runId>.json} — строго для того runId, который плагин
 * сгенерировал и отправил, — либо завершения контролируемого процесса клиента.
 *
 * <p>Рабочая папка берётся из обязательного системного свойства
 * {@code uitp.e2e.outDir} (без скрытого дефолта — запуск без каталога обмена
 * недопустим).</p>
 *
 * <p>Антимаскировка: результат принимается ТОЛЬКО по точному имени файла
 * конкретного runId (уникальный UUID на каждый запуск), поэтому старый отчёт
 * предыдущего прогона не может быть выдан за результат этого. Если контролируемый
 * процесс клиента завершился (аварийно или штатно) раньше, чем появился файл
 * результата, — это НЕ успех: возвращается {@code null}, вызывающий показывает
 * аварию, а не найденный по сторонам файл.</p>
 */
public final class LaunchMonitor {

	/** Системное свойство с путём к рабочей папке моста. */
	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";

	private static final String COMMANDS_FILE = "bridge-commands.json";
	private static final String RESULT_PREFIX = "bridge-result-";
	private static final String RESULT_SUFFIX = ".json";

	private static final long POLL_MS = 1500L;

	private LaunchMonitor() {
	}

	/**
	 * Рабочая папка моста из обязательного системного свойства.
	 *
	 * @return каталог обмена
	 * @throws CoreException если {@code uitp.e2e.outDir} не задан
	 */
	public static File outDir() throws CoreException {
		String out = System.getProperty(OUT_DIR_PROPERTY);
		if (out == null || out.isBlank()) {
			throw new CoreException(new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.IStatus.ERROR, "ru.ozon.uitp.e2e",
					"Не задано системное свойство -D" + OUT_DIR_PROPERTY
							+ " (каталог обмена моста). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		return new File(out.trim());
	}

	/** Генерирует уникальный runId для одного прогона моста. */
	public static String newRunId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Пишет командный файл моста атомарно (tmp → переименование), стартуя контур.
	 *
	 * @param outDir  каталог обмена
	 * @param content JSON тело {@code {runId, commands:[...]}} — контракт BSL-агента
	 * @throws CoreException если запись не удалась
	 */
	public static void writeCommands(File outDir, String content) throws CoreException {
		File tmp = new File(outDir, COMMANDS_FILE + ".tmp");
		File target = new File(outDir, COMMANDS_FILE);
		try {
			Files.write(tmp.toPath(), content.getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new CoreException(new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.IStatus.ERROR, "ru.ozon.uitp.e2e",
					"Не удалось записать командный файл моста в " + target.getAbsolutePath(), e));
		}
	}

	/**
	 * Ждёт результат моста для конкретного запуска (по runId).
	 *
	 * @param outDir    каталог обмена
	 * @param runId     runId этого прогона, сгенерированный {@link #newRunId()}
	 * @param launch    запущенный клиент (контролируемый процесс; может быть null)
	 * @param timeoutMs максимальное время ожидания
	 * @return файл {@code bridge-result-<runId>.json}, либо {@code null} по таймауту
	 *         или при завершении контролируемого процесса без результата
	 */
	public static File waitForBridgeResult(File outDir, String runId, ILaunch launch, long timeoutMs) {
		File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
		long startedAt = System.currentTimeMillis();
		while (System.currentTimeMillis() - startedAt < timeoutMs) {
			// 1) Появился именно наш файл результата — мост исполнился для этого runId.
			if (expected.isFile()) {
				return expected;
			}
			// 2) Контролируемый процесс завершился раньше результата -> авария/выход.
			//    Старый отчёт чужого runId не подставляем (мы ищем только expected).
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

	/**
	 * Код завершения контролируемого процесса клиента, если тот уже завершился.
	 *
	 * @param launch контролируемый запуск
	 * @return код выхода первого процесса, который завершился и сообщил код;
	 *         {@code Integer.MIN_VALUE}, если кода ещё нет или процесса нет
	 */
	public static int exitCodeOf(ILaunch launch) {
		if (launch == null) {
			return Integer.MIN_VALUE;
		}
		IProcess[] procs = launch.getProcesses();
		if (procs == null) {
			return Integer.MIN_VALUE;
		}
		for (IProcess p : procs) {
			if (p.isTerminated()) {
				try {
					return p.getExitValue();
				} catch (org.eclipse.debug.core.DebugException e) {
					// код неизвестен — не падаем, сигналим отсутствием кода
				}
			}
		}
		return Integer.MIN_VALUE;
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

	/**
	 * Читает содержимое файла результата моста (для показа/логирования исхода).
	 * Пустой, если не прочитался — читать результат не обязательно для успеха.
	 */
	public static String readFileSafe(File f) {
		if (f == null || !f.isFile()) {
			return "";
		}
		try {
			return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	/** Логирование в платформенный лог EDT. */
	public static void info(String msg) {
		ILog log = Platform.getLog(Platform.getBundle("ru.ozon.uitp.e2e"));
		if (log != null) {
			log.info(msg);
		}
	}
}
