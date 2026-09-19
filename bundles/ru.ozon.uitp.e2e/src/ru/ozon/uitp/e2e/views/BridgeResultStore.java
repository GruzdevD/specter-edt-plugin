package ru.ozon.uitp.e2e.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Общий стор последнего результата прогона моста. Связывает панели «Тесты» и
 * «Результаты»: панель-источник публикует найденный/свежеполученный результат,
 * остальные подписчики обновляют дерево/текст через {@link Listener}.
 *
 * <p>Хранит как модель результата, так и источник (файл), чтобы навигация
 * «к файлу/кодy» была возможной и автоматически находила последний результат
 * из каталога обмена при старте EDT.</p>
 */
public final class BridgeResultStore {

	/** Слушатель обновления результата (панели подписываются). */
	public interface Listener {
		/** Вызывается на UI-потоке при публикации нового результата. */
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

	/**
	 * Публикует результат и уведомляет подписчиков (вызывается с UI-потока).
	 */
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
	 * Ищет самый свежий {@code bridge-result-*.json} в каталоге обмена моста и
	 * публикует его. Используется при открытии панели и кнопкой «Обновить» без
	 * повторного запуска клиента.
	 *
	 * @return найденный результат или {@code null}, если нет/не читается
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
