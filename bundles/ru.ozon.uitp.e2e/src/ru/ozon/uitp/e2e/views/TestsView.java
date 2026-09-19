package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Панель «Тесты» — дерево прогона моста по образцу панели YAxUnit в EDT.
 *
 * <p>Дерево строится из последнего результата {@code bridge-result-*.json}:
 * корень — сценарий/набор со сводкой (passed/failed, число шагов), дочерние
 * узлы — отдельные команды моста со своими статусами. Панель умеет:
 * <ul>
 *   <li>{@code Запустить мост} — живой прогон сценария на реальном UI 1С в
 *       фоновом Job (см. {@link BridgeRunner}); по завершении результат
 *       публикуется в {@link BridgeResultStore} и обе панели обновляются;</li>
 *   <li>{@code Обновить} — перечитать последний результат из каталога обмена без
 *       повторного запуска клиента;</li>
 *   <li>{@code Развернуть}/{@code Свернуть} — навигация по дереву шагов.</li>
 * </ul>
 * Статус узла окрашивается: {@code passed} — зелёным, {@code failed} — красным.
 * Детали шага показываются подсказкой и в панели «Результаты».</p>
 */
public class TestsView extends ViewPart {

	/** Идентификатор панели (регистрируется в plugin.xml → org.eclipse.ui.views). */
	public static final String ID = "ru.ozon.uitp.e2e.views.TestsView";

	private final BridgeResultStore.Listener storeListener = this::refreshFromResult;

	private TreeViewer viewer;
	private ScenarioNode root;
	private ScenarioNode hintRoot;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new NodeContentProvider());
		viewer.setLabelProvider(new NodeLabelProvider());
		viewer.setAutoExpandLevel(2);

		createToolbarActions();

		BridgeResultStore.get().addListener(storeListener);

		// При открытии панели — подтянуть последний результат из каталога обмена.
		BridgeResult br = BridgeResultStore.get().current();
		if (br == null) {
			br = BridgeResultStore.get().reloadLatestFromOutDir();
		}
		refreshFromResult(br);
	}

	private void createToolbarActions() {
		IToolBarManager tb = getViewSite().getActionBars().getToolBarManager();
		tb.add(new Action("Запустить мост") {
			@Override
			public void run() {
				runScenario();
			}
		});
		tb.add(new Action("Обновить") {
			@Override
			public void run() {
				BridgeResultStore.get().reloadLatestFromOutDir();
			}
		});
		tb.add(new Action("Развернуть") {
			@Override
			public void run() {
				viewer.expandAll();
			}
		});
		tb.add(new Action("Свернуть") {
			@Override
			public void run() {
				viewer.collapseAll();
			}
		});
	}

	/** Живой прогон сценария моста; результат раскроется в обеих панелях. */
	private void runScenario() {
		setHint("Запускаю тонкий клиент АУФ…");
		BridgeRunner.runAsync(BridgeScenario.commandsJson(LaunchMonitor.newRunId()),
				r -> setHint("Прогон завершён: " + r.status + ", passed=" + r.passedCount()
						+ ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	private void refreshFromResult(BridgeResult result) {
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		if (result == null || result.status.equals("hint")) {
			setHint(result == null
					? "Результат моста не найден. Нажми «Запустить мост» или «Обновить»."
					: result.status);
			return;
		}
		root = new ScenarioNode(result);
		hintRoot = null;
		viewer.setInput(root);
		viewer.expandAll();
	}

	private void setHint(String text) {
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		// Отдельный «однострочный» узел-подсказка, не сбивающий последний результат.
		if (hintRoot == null) {
			hintRoot = new ScenarioNode(null);
			hintRoot.hintText = text;
		} else {
			hintRoot.hintText = text;
		}
		viewer.setInput(hintRoot);
		viewer.refresh();
	}

	@Override
	public void setFocus() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.getControl().setFocus();
		}
	}

	@Override
	public void dispose() {
		BridgeResultStore.get().removeListener(storeListener);
		super.dispose();
	}

	// ---------------------------------------------------------------------
	// Модель дерева
	// ---------------------------------------------------------------------

	/** Корневой узел: сценарий/набор со сводкой (или однострочная подсказка). */
	static final class ScenarioNode {
		final BridgeResult result;
		final List<StepNode> children = new ArrayList<>();
		String hintText;

		ScenarioNode(BridgeResult result) {
			this.result = result;
			if (result != null) {
				for (BridgeResult.Step s : result.steps) {
					children.add(new StepNode(s));
				}
			}
		}

		boolean isHint() {
			return result == null;
		}

		boolean isEmpty() {
			return result != null && result.steps.isEmpty();
		}
	}

	/** Дочерний узел: отдельная команда моста. */
	static final class StepNode {
		final BridgeResult.Step step;

		StepNode(BridgeResult.Step step) {
			this.step = step;
		}
	}

	private static final class NodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof ScenarioNode) {
				ScenarioNode n = (ScenarioNode) element;
				if (n.isHint() || n.isEmpty()) {
					return new Object[0];
				}
				return n.children.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ScenarioNode
					&& !((ScenarioNode) element).isHint()
					&& !((ScenarioNode) element).isEmpty();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[] { inputElement };
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
		private static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
		private static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);

		@Override
		public String getText(Object element) {
			if (element instanceof ScenarioNode) {
				ScenarioNode n = (ScenarioNode) element;
				if (n.isHint()) {
					return n.hintText;
				}
				BridgeResult r = n.result;
				return "Сценарий моста (АУФ) — " + statusText(r.status)
						+ "  [" + r.passedCount() + " ✓ / " + r.failedCount() + " ✗], шагов: " + r.steps.size();
			}
			if (element instanceof StepNode) {
				StepNode n = (StepNode) element;
				return "#" + n.step.id + "  " + n.step.action + "  —  " + statusText(n.step.status);
			}
			return String.valueOf(element);
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof StepNode) {
				return ((StepNode) element).step.isFailed() ? RED : GREEN;
			}
			if (element instanceof ScenarioNode) {
				ScenarioNode n = (ScenarioNode) element;
				if (!n.isHint() && n.result != null) {
					return n.result.isFailed() ? RED : GREEN;
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		private String statusText(String status) {
			if ("passed".equals(status)) {
				return "PASSED";
			}
			if ("failed".equals(status)) {
				return "FAILED";
			}
			return String.valueOf(status);
		}
	}
}
