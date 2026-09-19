package ru.ozon.uitp.e2e.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Потокобезопасный стор последнего результата прогона моста.
 * Поддерживает синхронное чтение и асинхронную подгрузку в фоновом Job.
 */
public final class BridgeResultStore {

	public interface Listener {
		void resultUpdated(BridgeResult result);
	}

	private static final BridgeResultStore INSTANCE = new BridgeResultStore();
	private final List<Listener> listeners = new ArrayList<>();
	private volatile BridgeResult current;

	private BridgeResultStore() {
	}

	public static BridgeResultStore get() {
		return INSTANCE;
	}

	public BridgeResult current() {
		return current;
	}

	public void set(BridgeResult result) {
		this.current = result;
		List<Listener> copy;
		synchronized (listeners) {
			copy = new ArrayList<>(listeners);
		}
		for (Listener l : copy) {
			l.resultUpdated(result);
		}
	}

	public void addListener(Listener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public void removeListener(Listener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	/**
	 * Асинхронно вычитывает последний результат в фоне, предотвращая подвисание UI.
	 */
	public void reloadLatestFromOutDirAsync(Consumer<BridgeResult> onComplete) {
		Job job = new Job("Specter: чтение последнего отчёта моста") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				BridgeResult result = reloadLatestFromOutDir();
				Display display = Display.getDefault();
				if (display != null && !display.isDisposed()) {
					display.asyncExec(() -> {
						if (onComplete != null) {
							onComplete.accept(result);
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * Синхронная вычитка файла. Рекомендуется вызывать только из фоновых Job!
	 */
	public BridgeResult reloadLatestFromOutDir() {
		try {
			File outDir = LaunchMonitor.outDir();
			if (outDir == null || !outDir.isDirectory()) {
				return null;
			}
			File[] files = outDir.listFiles((dir, name) ->
					name.startsWith("bridge-result-") && name.endsWith(".json"));
			if (files == null || files.length == 0) {
				return null;
			}
			File latest = files[0];
			for (File f : files) {
				if (f.lastModified() > latest.lastModified()) {
					latest = f;
				}
			}
			String body = LaunchMonitor.readFileSafe(latest);
			BridgeResult r = BridgeResult.parse(body, latest);
			if (r != null) {
				set(r);
			}
			return r;
		} catch (CoreException e) {
			return null;
		}
	}
}