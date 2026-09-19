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
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;

import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Панель «Тесты» — дерево моста по образцу панели YAxUnit в EDT.
 *
 * <p>Показывает дерево из последнего результата {@code bridge-result-*.json}
 * (сценарий/набор → шаги со статусами) и, когда в активном редакторе открыт
 * тестовый набор (общий модуль {@code OZON_UI_Тесты_*} / {@code УИ_Тесты_*}),
 * — дополнительный корень набора с его тестами (методами BSL-модуля). Отсюда
 * набор можно запустить ({@code Запустить набор <имя>}) через мост: движок
 * {@code OZON_UI_Тестирование} исполняет его и пишет результат в стор — дерево
 * обновляется автоматически.</p>
 *
 * <p>Toolbar: «Запустить мост» (канон R1 — живой UI-сценарий), «Запустить набор»
 * (активна, когда открыт тестовый набор в редакторе), «Обновить» (перечитать
 * последний результат), «Развернуть»/«Свернуть». Статусы окрашиваются: passed —
 * зелёным, failed — красным.</p>
 */
public class TestsView extends ViewPart {

	/** Идентификатор панели (регистрируется в plugin.xml → org.eclipse.ui.views). */
	public static final String ID = "ru.ozon.uitp.e2e.views.TestsView";

	private final BridgeResultStore.Listener storeListener = this::refreshFromResult;

	private TreeViewer viewer;
	private Action runSetAction;

	// Корни дерева.
	private RootSetNode setNode;       // активный тестовый набор из редактора (может быть null)
	private BridgeResult lastResult;   // последний результат моста (может быть null)
	private String hintText;           // однострочная подсказка (если ничего нет)

	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			refreshActiveSet();
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			refreshActiveSet();
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			refreshActiveSet();
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new NodeContentProvider());
		viewer.setLabelProvider(new NodeLabelProvider());
		viewer.setAutoExpandLevel(2);

		createToolbarActions();

		BridgeResultStore.get().addListener(storeListener);

		// Подписка на смену активного редактора: определяем открытый набор.
		try {
			getSite().getPage().addPartListener(partListener);
		} catch (RuntimeException e) {
			// страница недоступна — определение набора просто не будет автореагировать
		}

		BridgeResult br = BridgeResultStore.get().current();
		if (br == null) {
			br = BridgeResultStore.get().reloadLatestFromOutDir();
		}
		lastResult = br;
		refreshActiveSet();
		rebuild();
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

	/** Живой прогон сценария-канона R1 на реальном слое UI 1С. */
	private void runScenario() {
		setHint("Запускаю тонкий клиент АУФ (сценарий-канон R1)…");
		BridgeRunner.runAsync(BridgeScenario.commandsJson(LaunchMonitor.newRunId()),
				r -> setHint("Прогон завершён: " + r.status + ", passed=" + r.passedCount()
						+ ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	/** Запуск конкретного тестового набора (и опц. теста), открытого в редакторе. */
	private void runActiveSet() {
		if (setNode == null) {
			return;
		}
		String moduleName = setNode.moduleName;
		String test = setNode.selectedTest == null ? "" : setNode.selectedTest;
		String what = test.isEmpty() ? "весь набор" : "тест " + test;
		setHint("Запускаю набор " + moduleName + " (" + what + ")…");
		BridgeRunner.runAsync(BridgeScenario.runSetJson(LaunchMonitor.newRunId(), moduleName, test),
				r -> setHint("Набор " + moduleName + ": " + r.status
						+ ", passed=" + r.passedCount() + ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	/** Определяет активный тестовый набор из редактора и обновляет кнопку. */
	private void refreshActiveSet() {
		CommonModule cm = EditorModuleSupport.activeCommonModule(EditorModuleSupport.activeEditor());
		String name = cm == null ? null : EditorModuleSupport.moduleName(cm);
		if (name != null && EditorModuleSupport.isTestSetName(name)) {
			List<String> tests = EditorModuleSupport.testNames(cm);
			setNode = new RootSetNode(name, tests);
			if (tests.size() > 0) {
				setNode.selectedTest = tests.get(0);
			}
			runSetAction.setEnabled(true);
			runSetAction.setToolTipText("Запустить набор " + name);
		} else {
			setNode = null;
			runSetAction.setEnabled(false);
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
			getSite().getPage().removePartListener(partListener);
		} catch (RuntimeException e) {
			// игнорируем
		}
		super.dispose();
	}

	// ---------------------------------------------------------------------
	// Модель дерева
	// ---------------------------------------------------------------------

	/** Корень: активный тестовый набор из редактора + его тесты. */
	static final class RootSetNode {
		final String moduleName;
		final List<TestNode> tests = new ArrayList<>();
		String selectedTest;

		RootSetNode(String moduleName, List<String> testNames) {
			this.moduleName = moduleName;
			for (String t : testNames) {
				tests.add(new TestNode(t));
			}
		}
	}

	static final class TestNode {
		final String name;

		TestNode(String name) {
			this.name = name;
		}
	}

	static final class RootResultNode {
		final BridgeResult result;
		final List<StepNode> steps = new ArrayList<>();

		RootResultNode(BridgeResult result) {
			this.result = result;
			for (BridgeResult.Step s : result.steps) {
				steps.add(new StepNode(s));
			}
		}
	}

	/** Дочерний узел: отдельная команда/шаг моста. */
	static final class StepNode {
		final BridgeResult.Step step;

		StepNode(BridgeResult.Step step) {
			this.step = step;
		}
	}

	private static final class NodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			TestsView v = (TestsView) inputElement;
			List<Object> roots = new ArrayList<>();
			if (v.setNode != null) {
				roots.add(v.setNode);
			}
			if (v.lastResult != null) {
				roots.add(new RootResultNode(v.lastResult));
			}
			if (roots.isEmpty()) {
				roots.add(new HintNode(v.hintText == null ? "" : v.hintText));
			}
			return roots.toArray();
		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof RootSetNode) {
				return ((RootSetNode) element).tests.toArray();
			}
			if (element instanceof RootResultNode) {
				return ((RootResultNode) element).steps.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return element instanceof HintNode ? null : element;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof RootSetNode || element instanceof RootResultNode;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	static final class HintNode {
		final String text;

		HintNode(String text) {
			this.text = text;
		}
	}

	private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
		private static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
		private static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);
		private static final Color BLUE = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);

		@Override
		public String getText(Object element) {
			if (element instanceof HintNode) {
				return ((HintNode) element).text;
			}
			if (element instanceof RootSetNode) {
				RootSetNode n = (RootSetNode) element;
				return "Набор: " + n.moduleName + "   (тестов: " + n.tests.size()
						+ "; запуск через мост OZON_UI_Тестирование)";
			}
			if (element instanceof TestNode) {
				return "🔬 " + ((TestNode) element).name;
			}
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
			if (element instanceof RootSetNode) {
				return BLUE;
			}
			if (element instanceof RootResultNode) {
				return ((RootResultNode) element).result.isFailed() ? RED : GREEN;
			}
			if (element instanceof StepNode) {
				return ((StepNode) element).step.isFailed() ? RED : GREEN;
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
