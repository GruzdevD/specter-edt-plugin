package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Объединённая панель «Specter: Тесты и Результаты»:
 * Единая среда с деревом запуска тестов (слева) и детальным протоколом результатов (справа).
 * Включает кастомный брендированный Empty State с эмблемой Specter на фоне,
 * когда отчёты ещё не сформированы.
 */
public class TestsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.TestsView";

	private SashForm splitContainer;
	private TreeViewer testViewer;
	private StyledText resultsText;
	private Composite resultsHeader;
	private Label statusLabel;
	private Label metricsLabel;

	private Action runBridgeAction;
	private Action runSetAction;
	private Action runSingleTestAction;
	private Action refreshAction;
	private Action clearAction;

	private RootSetNode activeSetNode;
	private BridgeResult currentResult;
	private String customStatusHint;
	private Image specterEmblemImage;

	private final BridgeResultStore.Listener storeListener = this::updateResultFromStore;
	private final IPartListener2 editorPartListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (partRef != null && "com._1c.g5.v8.dt.bsl.ui.BslEditor".equals(partRef.getId())) {
				refreshActiveEditorSet();
			}
		}
		@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override public void partClosed(IWorkbenchPartReference partRef) { refreshActiveEditorSet(); }
		@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override public void partOpened(IWorkbenchPartReference partRef) {}
		@Override public void partHidden(IWorkbenchPartReference partRef) {}
		@Override public void partVisible(IWorkbenchPartReference partRef) {}
		@Override public void partInputChanged(IWorkbenchPartReference partRef) { refreshActiveEditorSet(); }
	};

	@Override
	public void createPartControl(Composite parent) {
		loadSpecterEmblem();

		// Сплиттер SashForm: разделение экрана на левую и правую части
		splitContainer = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
		splitContainer.setLayout(new FillLayout());

		// Левая часть: Дерево тестов
		createTestsTreeSection(splitContainer);

		// Правая часть: Детальные результаты и протокол шагов
		createResultsSection(splitContainer);

		// 45% ширина дерева тестов, 55% протокол результатов
		splitContainer.setWeights(new int[] { 45, 55 });

		createToolbarActions();

		BridgeResultStore.get().addListener(storeListener);

		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().addPartListener(editorPartListener);
			}
		} catch (RuntimeException ignored) {
		}

		// Загрузка последнего отчёта
		BridgeResult br = BridgeResultStore.get().current();
		if (br == null) {
			customStatusHint = "Готов к запуску сценариев 1С (ожидание первого прогона)";
			BridgeResultStore.get().reloadLatestFromOutDirAsync(loaded -> {
				Display.getDefault().asyncExec(() -> {
					if (splitContainer != null && !splitContainer.isDisposed()) {
						currentResult = loaded;
						refreshActiveEditorSet();
						renderResults();
						rebuildTree();
					}
				});
			});
		} else {
			currentResult = br;
			refreshActiveEditorSet();
			renderResults();
			rebuildTree();
		}
	}

	private void loadSpecterEmblem() {
		try {
			org.eclipse.jface.resource.ImageDescriptor desc = Activator.imageDescriptorFromPlugin(
				Activator.BUNDLE_ID, "icons/specter-tests@2x.png");
			if (desc == null) {
				desc = Activator.imageDescriptorFromPlugin(Activator.BUNDLE_ID, "icons/specter-tests.png");
			}
			if (desc != null) {
				specterEmblemImage = desc.createImage();
				setTitleImage(specterEmblemImage);
			}
		} catch (Throwable ignored) {
		}
	}

	private void createTestsTreeSection(Composite parent) {
		Composite leftComp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		leftComp.setLayout(gl);

		testViewer = new TreeViewer(leftComp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		testViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		testViewer.setContentProvider(new TestsTreeContentProvider());
		testViewer.setLabelProvider(new TestsTreeStyledLabelProvider());

		// Кастомная фоновая эмблема Specter в бэкграунде дерева, когда нет данных
		testViewer.getTree().addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (activeSetNode == null && currentResult == null && specterEmblemImage != null && !specterEmblemImage.isDisposed()) {
					drawWatermark(e.gc, testViewer.getTree().getClientArea(), "Specter E2E Runner", "Откройте тестовый BSL-модуль или запустите сценарий (R1)");
				}
			}
		});

		testViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) testViewer.getSelection();
				Object el = sel.getFirstElement();
				if (el instanceof TestNode) {
					runSelectedSingleTest(((TestNode) el).name);
				} else if (el instanceof RootSetNode) {
					runActiveSet();
				}
			}
		});
	}

	private void createResultsSection(Composite parent) {
		Composite rightComp = new Composite(parent, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		rightComp.setLayout(gl);

		// Заголовок карточки результатов со статус-индикаторами
		resultsHeader = new Composite(rightComp, SWT.NONE);
		resultsHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout hgl = new GridLayout(2, false);
		hgl.marginWidth = 8;
		hgl.marginHeight = 6;
		resultsHeader.setLayout(hgl);

		statusLabel = new Label(resultsHeader, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusLabel.setText("Результаты выполнения сценариев 1С");

		metricsLabel = new Label(resultsHeader, SWT.RIGHT);
		metricsLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		metricsLabel.setText("");

		// Многострочный лог шагов
		resultsText = new StyledText(rightComp, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		resultsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsText.setEditable(false);

		// Отрисовка красивой эмблемы Specter на фоне текста, когда отчёты отсутствуют
		resultsText.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (currentResult == null && specterEmblemImage != null && !specterEmblemImage.isDisposed()) {
					drawWatermark(e.gc, resultsText.getClientArea(), "Specter: Протокол сценариев 1С", "Отчёты ещё не сформированы. Нажмите «Запустить E2E-сценарий».");
				}
			}
		});
	}

	private void drawWatermark(GC gc, Rectangle bounds, String title, String subtitle) {
		if (specterEmblemImage == null || specterEmblemImage.isDisposed()) return;
		try {
			int imgW = specterEmblemImage.getBounds().width;
			int imgH = specterEmblemImage.getBounds().height;

			int x = (bounds.width - imgW) / 2;
			int y = Math.max(24, (bounds.height - imgH) / 2 - 36);

			gc.setAlpha(40);
			gc.drawImage(specterEmblemImage, x, y);
			gc.setAlpha(255);

			Display d = (Display) gc.getDevice();
			gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_GRAY));
			int tw = gc.stringExtent(title).x;
			gc.drawString(title, (bounds.width - tw) / 2, y + imgH + 12, true);

			gc.setForeground(d.getSystemColor(SWT.COLOR_GRAY));
			int sw = gc.stringExtent(subtitle).x;
			gc.drawString(subtitle, (bounds.width - sw) / 2, y + imgH + 32, true);
		} catch (Throwable ignored) {
		}
	}

	private void createToolbarActions() {
		IToolBarManager tb = getViewSite().getActionBars().getToolBarManager();

		runBridgeAction = new Action("Запустить мост (R1-канон)") {
			@Override public void run() { runBridgeScenario(); }
		};
		runBridgeAction.setToolTipText("Выполнить полный сквозной UI-сценарий R1 карточки через тонкий клиент");
		try {
			runBridgeAction.setImageDescriptor(Activator.imageDescriptorFromPlugin(Activator.BUNDLE_ID, "icons/specter-tests.png"));
		} catch (Throwable ignored) {}
		tb.add(runBridgeAction);

		runSetAction = new Action("Запустить набор BSL") {
			@Override public void run() { runActiveSet(); }
		};
		runSetAction.setEnabled(false);
		runSetAction.setToolTipText("Запустить тестовый набор из открытого в EDT BSL-редактора");
		tb.add(runSetAction);

		runSingleTestAction = new Action("Запустить выбранный тест") {
			@Override public void run() {
				IStructuredSelection sel = (IStructuredSelection) testViewer.getSelection();
				Object obj = sel.getFirstElement();
				if (obj instanceof TestNode) {
					runSelectedSingleTest(((TestNode) obj).name);
				}
			}
		};
		runSingleTestAction.setToolTipText("Запустить только выделенный тест");
		tb.add(runSingleTestAction);

		tb.add(new Separator());

		refreshAction = new Action("Обновить отчёты") {
			@Override public void run() {
				setCustomStatus("Обновление данных из каталога обмена...");
				BridgeResultStore.get().reloadLatestFromOutDirAsync(r -> {
					Display.getDefault().asyncExec(() -> {
						if (splitContainer != null && !splitContainer.isDisposed()) {
							currentResult = r;
							setCustomStatus(r != null ? "Отчёт успешно загружен" : "Ожидание формирования первого отчёта моста...");
							renderResults();
							rebuildTree();
						}
					});
				});
			}
		};
		refreshAction.setToolTipText("Перечитать результаты из exchange каталога");
		tb.add(refreshAction);

		clearAction = new Action("Очистить результаты") {
			@Override public void run() {
				currentResult = null;
				setCustomStatus("Результаты очищены. Готов к новому прогону.");
				renderResults();
				rebuildTree();
			}
		};
		tb.add(clearAction);

		tb.add(new Action("Развернуть") {
			@Override public void run() { testViewer.expandAll(); }
		});
		tb.add(new Action("Свернуть") {
			@Override public void run() { testViewer.collapseAll(); }
		});
	}

	/** Запуск сценария моста R1-канон */
	private void runBridgeScenario() {
		setCustomStatus("Запускаю тонкий клиент АУФ (сценарий-канон R1)…");
		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.commandsJson(runId);
		LaunchMonitor.info("TestsView: Запуск сценария моста R1 runId=" + runId);

		BridgeRunner.runAsync(runId, json,
			r -> Display.getDefault().asyncExec(() -> {
				currentResult = r;
				setCustomStatus("Прогон завершён: " + r.status.toUpperCase() + ", passed=" + r.passedCount() + ", failed=" + r.failedCount());
				renderResults();
				rebuildTree();
			}),
			err -> Display.getDefault().asyncExec(() -> {
				setCustomStatus("Ошибка выполнения сценария: " + err);
				renderResults();
			})
		);
	}

	/** Запуск активного набора из BSL редактора */
	private void runActiveSet() {
		if (activeSetNode == null) return;
		String moduleName = activeSetNode.moduleName;
		String test = activeSetNode.selectedTest == null ? "" : activeSetNode.selectedTest;
		String what = test.isEmpty() ? "весь набор" : "тест " + test;
		setCustomStatus("Запускаю набор " + moduleName + " (" + what + ")…");

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.runSetJson(runId, moduleName, test);
		LaunchMonitor.info("TestsView: Запуск набора " + moduleName + " runId=" + runId);

		BridgeRunner.runAsync(runId, json,
			r -> Display.getDefault().asyncExec(() -> {
				currentResult = r;
				setCustomStatus("Набор " + moduleName + ": " + r.status.toUpperCase()
						+ ", passed=" + r.passedCount() + ", failed=" + r.failedCount());
				renderResults();
				rebuildTree();
			}),
			err -> Display.getDefault().asyncExec(() -> {
				setCustomStatus("Ошибка: " + err);
				renderResults();
			})
		);
	}

	/** Запуск одного конкретного теста */
	private void runSelectedSingleTest(String testName) {
		if (activeSetNode == null) return;
		activeSetNode.selectedTest = testName;
		runActiveSet();
	}

	private void refreshActiveEditorSet() {
		CommonModule cm = EditorModuleSupport.activeCommonModule(EditorModuleSupport.activeEditor());
		String name = cm == null ? null : EditorModuleSupport.moduleName(cm);
		if (name != null && EditorModuleSupport.isTestSetName(name)) {
			List<String> tests = EditorModuleSupport.testNames(cm);
			activeSetNode = new RootSetNode(name, tests);
			if (!tests.isEmpty()) {
				activeSetNode.selectedTest = tests.get(0);
			}
			if (runSetAction != null) {
				runSetAction.setEnabled(true);
				runSetAction.setToolTipText("Запустить набор " + name);
			}
		} else {
			activeSetNode = null;
			if (runSetAction != null) {
				runSetAction.setEnabled(false);
			}
		}
		rebuildTree();
	}

	private void updateResultFromStore(BridgeResult r) {
		Display.getDefault().asyncExec(() -> {
			if (splitContainer != null && !splitContainer.isDisposed()) {
				currentResult = r;
				renderResults();
				rebuildTree();
			}
		});
	}

	private void setCustomStatus(String text) {
		this.customStatusHint = text;
		if (statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText(text);
		}
	}

	private void rebuildTree() {
		if (testViewer == null || testViewer.getControl().isDisposed()) return;
		testViewer.setInput(this);
		testViewer.refresh();
		if (activeSetNode != null || currentResult != null) {
			testViewer.expandAll();
		}
	}

	/** Отрисовка протокола результатов в правой части */
	private void renderResults() {
		if (resultsText == null || resultsText.isDisposed()) return;

		if (currentResult == null || "hint".equals(currentResult.status)) {
			resultsText.setText("");
			statusLabel.setText("Specter: Ожидание первого запуска моста");
			metricsLabel.setText("");
			resultsText.redraw();
			return;
		}

		boolean isPassed = !currentResult.isFailed();
		String statusText = isPassed ? "PASSED" : "FAILED";
		statusLabel.setText("Прогон " + currentResult.runId + " — " + statusText);
		metricsLabel.setText(currentResult.passedCount() + " ✓ / " + currentResult.failedCount() + " ✗  (шагов: " + currentResult.steps.size() + ")");

		StringBuilder sb = new StringBuilder();
		sb.append("=== SPECTER TEST RUNNER PROTOCOL ===\n");
		sb.append("Run ID:   ").append(currentResult.runId).append('\n');
		sb.append("Статус:   ").append(statusText).append(" (пройдено: ")
		  .append(currentResult.passedCount()).append(", провалено: ")
		  .append(currentResult.failedCount()).append(")\n");

		if (currentResult.source != null) {
			sb.append("Файл:     ").append(currentResult.source.getAbsolutePath()).append('\n');
		}
		sb.append("\nШаги сценария:\n");
		sb.append("------------------------------------------------------------\n");

		for (BridgeResult.Step s : currentResult.steps) {
			String stepStatus = s.status != null ? s.status.toUpperCase() : "UNKNOWN";
			sb.append(String.format("  #%-2d  %-24s [%s]", s.id, s.action, stepStatus));
			if (s.detail != null && !s.detail.isEmpty()) {
				sb.append("\n       └─ ").append(s.detail);
			}
			sb.append('\n');
		}
		sb.append("------------------------------------------------------------\n");

		String fullText = sb.toString();
		resultsText.setText(fullText);

		// Подсветка статусов PASSED (зеленый) и FAILED (красный)
		Color green = resultsText.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		Color red = resultsText.getDisplay().getSystemColor(SWT.COLOR_RED);

		List<StyleRange> ranges = new ArrayList<>();
		int idx = 0;
		while ((idx = fullText.indexOf("PASSED", idx)) != -1) {
			StyleRange sr = new StyleRange();
			sr.start = idx;
			sr.length = 6;
			sr.foreground = green;
			sr.fontStyle = SWT.BOLD;
			ranges.add(sr);
			idx += 6;
		}
		idx = 0;
		while ((idx = fullText.indexOf("FAILED", idx)) != -1) {
			StyleRange sr = new StyleRange();
			sr.start = idx;
			sr.length = 6;
			sr.foreground = red;
			sr.fontStyle = SWT.BOLD;
			ranges.add(sr);
			idx += 6;
		}
		resultsText.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	@Override
	public void setFocus() {
		if (testViewer != null && !testViewer.getControl().isDisposed()) {
			testViewer.getControl().setFocus();
		}
	}

	@Override
	public void dispose() {
		BridgeResultStore.get().removeListener(storeListener);
		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().removePartListener(editorPartListener);
			}
		} catch (RuntimeException ignored) {
		}
		if (specterEmblemImage != null && !specterEmblemImage.isDisposed()) {
			specterEmblemImage.dispose();
		}
		super.dispose();
	}

	// Модели дерева тестов
	public static final class RootSetNode {
		public final String moduleName;
		public final List<TestNode> tests = new ArrayList<>();
		public String selectedTest;
		public RootSetNode(String moduleName, List<String> testNames) {
			this.moduleName = moduleName;
			for (String t : testNames) tests.add(new TestNode(t));
		}
	}

	public static final class TestNode {
		public final String name;
		public TestNode(String name) { this.name = name; }
	}

	public static final class RootResultNode {
		public final BridgeResult result;
		public final List<StepNode> steps = new ArrayList<>();
		public RootResultNode(BridgeResult result) {
			this.result = result;
			for (BridgeResult.Step s : result.steps) steps.add(new StepNode(s));
		}
	}

	public static final class StepNode {
		public final BridgeResult.Step step;
		public StepNode(BridgeResult.Step step) { this.step = step; }
	}

	public static final class EmptyPlaceholderNode {
		public final String message;
		public EmptyPlaceholderNode(String msg) { this.message = msg; }
	}

	private static final class TestsTreeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			TestsView v = (TestsView) inputElement;
			List<Object> roots = new ArrayList<>();
			if (v.activeSetNode != null) roots.add(v.activeSetNode);
			if (v.currentResult != null) roots.add(new RootResultNode(v.currentResult));
			if (roots.isEmpty()) {
				String hint = v.customStatusHint != null ? v.customStatusHint
					: "Откройте тестовый BSL-модуль или нажмите «Запустить мост»";
				roots.add(new EmptyPlaceholderNode(hint));
			}
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

	private static final class TestsTreeLabelProvider extends LabelProvider implements IColorProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof EmptyPlaceholderNode) {
				return "⚡ " + ((EmptyPlaceholderNode) element).message;
			}
			if (element instanceof RootSetNode) {
				RootSetNode n = (RootSetNode) element;
				return "📦 Набор: " + n.moduleName + " (" + n.tests.size() + " тестов)";
			}
			if (element instanceof TestNode) {
				return "🔬 " + ((TestNode) element).name;
			}
			if (element instanceof RootResultNode) {
				RootResultNode n = (RootResultNode) element;
				BridgeResult r = n.result;
				String status = r.isFailed() ? "FAILED" : "PASSED";
				return "📊 Прогон " + r.runId + " [" + status + "] (" + r.passedCount() + " ✓ / " + r.failedCount() + " ✗)";
			}
			if (element instanceof StepNode) {
				StepNode n = (StepNode) element;
				return "#" + n.step.id + " " + n.step.action + " — " + n.step.status.toUpperCase();
			}
			return String.valueOf(element);
		}

		@Override
		public Color getForeground(Object element) {
			Display d = Display.getCurrent();
			if (d == null) d = Display.getDefault();
			if (d == null || d.isDisposed()) return null;

			if (element instanceof EmptyPlaceholderNode) {
				return d.getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			if (element instanceof RootSetNode) {
				return d.getSystemColor(SWT.COLOR_DARK_BLUE);
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
	}
}
