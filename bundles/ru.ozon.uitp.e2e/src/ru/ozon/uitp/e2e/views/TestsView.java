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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Панель «Тесты» (Specter): дерево запуска и активный набор BSL.
 * Все I/O операции строго асинхронны, ресурсы SWT безопасно утилизируются.
 */
public class TestsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.TestsView";

	private TreeViewer viewer;
	private Action runSetAction;
	private RootSetNode setNode;
	private BridgeResult lastResult;
	private String hintText;

	private final BridgeResultStore.Listener storeListener = this::refreshFromResult;
	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (partRef != null && "com._1c.g5.v8.dt.bsl.ui.BslEditor".equals(partRef.getId())) {
				refreshActiveSet();
			}
		}
		@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override public void partClosed(IWorkbenchPartReference partRef) { refreshActiveSet(); }
		@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override public void partOpened(IWorkbenchPartReference partRef) {}
		@Override public void partHidden(IWorkbenchPartReference partRef) {}
		@Override public void partVisible(IWorkbenchPartReference partRef) {}
		@Override public void partInputChanged(IWorkbenchPartReference partRef) { refreshActiveSet(); }
	};

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new NodeContentProvider());
		viewer.setLabelProvider(new NodeLabelProvider());

		createToolbarActions();
		BridgeResultStore.get().addListener(storeListener);

		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().addPartListener(partListener);
			}
		} catch (RuntimeException e) {
			// игнорируем во вспомогательных контекстах
		}

		// Безопасное получение текущего результата без синхронного дискового I/O:
		BridgeResult br = BridgeResultStore.get().current();
		if (br == null) {
			setHint("Поиск последнего прогона...");
			BridgeResultStore.get().reloadLatestFromOutDirAsync(loaded -> {
				if (viewer != null && !viewer.getControl().isDisposed()) {
					lastResult = loaded;
					hintText = null;
					refreshActiveSet();
					rebuild();
				}
			});
		} else {
			lastResult = br;
			refreshActiveSet();
			rebuild();
		}
	}

	private void createToolbarActions() {
		IToolBarManager tb = getViewSite().getActionBars().getToolBarManager();
		tb.add(new Action("Запустить мост") {
			@Override
			public void run() {
				runScenario();
			}
		});
		runSetAction = new Action("Запустить набор") {
			@Override
			public void run() {
				runActiveSet();
			}
		};
		runSetAction.setEnabled(false);
		tb.add(runSetAction);
		tb.add(new Action("Обновить") {
			@Override
			public void run() {
				setHint("Обновление данных из каталога обмена...");
				BridgeResultStore.get().reloadLatestFromOutDirAsync(r -> {
					if (viewer != null && !viewer.getControl().isDisposed()) {
						setHint(r != null ? "Отчёт обновлен" : "Отчёты не найдены");
						rebuild();
					}
				});
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

	/** Живой прогон сценария-канона R1 с согласованным runId */
	private void runScenario() {
		setHint("Запускаю тонкий клиент АУФ (сценарий-канон R1)…");
		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.commandsJson(runId);
		BridgeRunner.runAsync(runId, json,
				r -> setHint("Прогон завершён: " + r.status + ", passed=" + r.passedCount()
						+ ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	/** Запуск набора с согласованным runId */
	private void runActiveSet() {
		if (setNode == null) return;
		String moduleName = setNode.moduleName;
		String test = setNode.selectedTest == null ? "" : setNode.selectedTest;
		String what = test.isEmpty() ? "весь набор" : "тест " + test;
		setHint("Запускаю набор " + moduleName + " (" + what + ")…");

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.runSetJson(runId, moduleName, test);
		BridgeRunner.runAsync(runId, json,
				r -> setHint("Набор " + moduleName + ": " + r.status
						+ ", passed=" + r.passedCount() + ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	private void refreshActiveSet() {
		CommonModule cm = EditorModuleSupport.activeCommonModule(EditorModuleSupport.activeEditor());
		String name = cm == null ? null : EditorModuleSupport.moduleName(cm);
		if (name != null && EditorModuleSupport.isTestSetName(name)) {
			List<String> tests = EditorModuleSupport.testNames(cm);
			setNode = new RootSetNode(name, tests);
			if (!tests.isEmpty()) {
				setNode.selectedTest = tests.get(0);
			}
			if (runSetAction != null) {
				runSetAction.setEnabled(true);
				runSetAction.setToolTipText("Запустить набор " + name);
			}
		} else {
			setNode = null;
			if (runSetAction != null) {
				runSetAction.setEnabled(false);
			}
		}
		rebuild();
	}

	private void refreshFromResult(BridgeResult result) {
		lastResult = result;
		rebuild();
	}

	private void setHint(String text) {
		this.hintText = text;
		rebuild();
	}

	private void rebuild() {
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		boolean hasAny = setNode != null || lastResult != null;
		if (!hasAny && hintText == null) {
			hintText = "Ничего не открыто. Нажми «Запустить мост» или открой тестовый набор в редакторе.";
		}
		viewer.setInput(this);
		viewer.refresh();
		if (setNode != null || lastResult != null) {
			viewer.expandAll();
		}
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
		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().removePartListener(partListener);
			}
		} catch (RuntimeException e) {
			// игнорируем
		}
		super.dispose();
	}

	// Модели узлов TreeViewer
	static final class RootSetNode {
		final String moduleName;
		final List<TestNode> tests = new ArrayList<>();
		String selectedTest;
		RootSetNode(String moduleName, List<String> testNames) {
			this.moduleName = moduleName;
			for (String t : testNames) tests.add(new TestNode(t));
		}
	}

	static final class TestNode {
		final String name;
		TestNode(String name) { this.name = name; }
	}

	static final class RootResultNode {
		final BridgeResult result;
		final List<StepNode> steps = new ArrayList<>();
		RootResultNode(BridgeResult result) {
			this.result = result;
			for (BridgeResult.Step s : result.steps) steps.add(new StepNode(s));
		}
	}

	static final class StepNode {
		final BridgeResult.Step step;
		StepNode(BridgeResult.Step step) { this.step = step; }
	}

	private static final class NodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			TestsView v = (TestsView) inputElement;
			List<Object> roots = new ArrayList<>();
			if (v.setNode != null) roots.add(v.setNode);
			if (v.lastResult != null) roots.add(new RootResultNode(v.lastResult));
			if (roots.isEmpty()) roots.add(new HintNode(v.hintText == null ? "" : v.hintText));
			return roots.toArray();
		}
		@Override
		public Object[] getChildren(Object el) {
			if (el instanceof RootSetNode) return ((RootSetNode) el).tests.toArray();
			if (el instanceof RootResultNode) return ((RootResultNode) el).steps.toArray();
			return new Object[0];
		}
		@Override public Object getParent(Object el) { return null; }
		@Override public boolean hasChildren(Object el) {
			return el instanceof RootSetNode || el instanceof RootResultNode;
		}
		@Override public void inputChanged(Viewer v, Object o, Object n) {}
	}

	static final class HintNode {
		final String text;
		HintNode(String text) { this.text = text; }
	}

	private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof HintNode) return ((HintNode) element).text;
			if (element instanceof RootSetNode) {
				RootSetNode n = (RootSetNode) element;
				return "Набор: " + n.moduleName + "   (тестов: " + n.tests.size()
						+ "; запуск через мост OZON_UI_Тестирование)";
			}
			if (element instanceof TestNode) return "🔬 " + ((TestNode) element).name;
			if (element instanceof RootResultNode) {
				RootResultNode n = (RootResultNode) element;
				BridgeResult r = n.result;
				return "Прогон моста — " + statusText(r.status)
						+ "   [" + r.passedCount() + " ✓ / " + r.failedCount() + " ✗], шагов: " + r.steps.size();
			}
			if (element instanceof StepNode) {
				StepNode n = (StepNode) element;
				return "#" + n.step.id + "  " + n.step.action + "  —  " + statusText(n.step.status);
			}
			return String.valueOf(element);
		}

		@Override
		public Color getForeground(Object element) {
			Display d = Display.getCurrent();
			if (d == null) d = Display.getDefault();
			if (d == null || d.isDisposed()) return null;

			if (element instanceof RootSetNode) {
				return d.getSystemColor(SWT.COLOR_BLUE);
			}
			if (element instanceof RootResultNode) {
				return ((RootResultNode) element).result.isFailed()
						? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
			}
			if (element instanceof StepNode) {
				return ((StepNode) element).step.isFailed()
						? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		private String statusText(String status) {
			if ("passed".equals(status)) return "PASSED";
			if ("failed".equals(status)) return "FAILED";
			return String.valueOf(status);
		}
	}
}