package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Панель «Тесты расширения» (Specter Extension Tests):
 * Отображает все известные тестовые наборы и методы расширения СП_Тестирование,
 * с возможностью вызывать их запуск прямо из панели (поштучно, по модулям или целиком).
 */
public class ExtensionTestsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.ExtensionTestsView";

	private TreeViewer viewer;
	private Action runSelectedAction;
	private Action runModuleAction;
	private Action runAllAction;
	private Action refreshAction;

	private final List<ModuleNode> modules = new ArrayList<>();
	private Image specterLogoImage;
	private String statusMessage = "Готов к запуску тестов расширения СП_Тестирование";

	private final BridgeResultStore.Listener storeListener = r -> {
		Display.getDefault().asyncExec(() -> {
			if (viewer != null && !viewer.getControl().isDisposed()) {
				updateStatusFromResult(r);
				viewer.refresh();
			}
		});
	};

	private final org.eclipse.core.resources.IResourceChangeListener workspaceChangeListener = event -> {
		org.eclipse.core.resources.IResourceDelta delta = event.getDelta();
		if (delta == null) return;
		try {
			delta.accept(d -> {
				org.eclipse.core.resources.IResource res = d.getResource();
				if (res instanceof org.eclipse.core.resources.IFile) {
					String name = res.getName().toLowerCase();
					if (name.endsWith(".bsl") || name.endsWith(".os")) {
						Display.getDefault().asyncExec(() -> {
							if (viewer != null && !viewer.getControl().isDisposed()) {
								populateDefaultExtensionModules();
								viewer.refresh();
							}
						});
						return false;
					}
				}
				return true;
			});
		} catch (Throwable ignored) {}
	};

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		// Инициализируем эмблему Specter для фонового брендинга
		try {
			org.eclipse.jface.resource.ImageDescriptor desc = Activator.imageDescriptorFromPlugin(
				Activator.BUNDLE_ID, "icons/specter-tests@2x.png");
			if (desc == null) {
				desc = Activator.imageDescriptorFromPlugin(Activator.BUNDLE_ID, "icons/specter-tests.png");
			}
			if (desc != null) {
				specterLogoImage = desc.createImage();
				setTitleImage(specterLogoImage);
			}
		} catch (Throwable ignored) {
		}

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ExtensionTreeContentProvider());
		viewer.setLabelProvider(new ExtensionTreeStyledLabelProvider());

		// Кастомная отрисовка эмблемы Specter в бэкграунде
		viewer.getTree().addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (modules.isEmpty() && specterLogoImage != null && !specterLogoImage.isDisposed()) {
					drawSpecterWatermark(e.gc, viewer.getTree().getClientArea());
				}
			}
		});

		populateDefaultExtensionModules();

		createActions();
		createContextMenu();
		hookDoubleClick();

		BridgeResultStore.get().addListener(storeListener);
		try {
			org.eclipse.core.resources.ResourcesPlugin.getWorkspace().addResourceChangeListener(
				workspaceChangeListener,
				org.eclipse.core.resources.IResourceChangeEvent.POST_CHANGE
			);
		} catch (Throwable ignored) {}

		viewer.setInput(this);
		viewer.expandAll();
	}

	private void drawSpecterWatermark(GC gc, Rectangle bounds) {
		if (specterLogoImage == null || specterLogoImage.isDisposed()) return;
		try {
			int imgW = specterLogoImage.getBounds().width;
			int imgH = specterLogoImage.getBounds().height;

			// Отрисовка по центру
			int x = (bounds.width - imgW) / 2;
			int y = Math.max(20, (bounds.height - imgH) / 2 - 40);

			gc.setAlpha(45);
			gc.drawImage(specterLogoImage, x, y);
			gc.setAlpha(255);

			// Красивый информационный текст под эмблемой
			Display d = (Display) gc.getDevice();
			gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_GRAY));
			String title = "Specter: Тесты расширения СП_Тестирование";
			int titleW = gc.stringExtent(title).x;
			gc.drawString(title, (bounds.width - titleW) / 2, y + imgH + 15, true);

			gc.setForeground(d.getSystemColor(SWT.COLOR_GRAY));
			String sub = "Выберите тест или нажмите «Запустить все тесты» на панели инструментов";
			int subW = gc.stringExtent(sub).x;
			gc.drawString(sub, (bounds.width - subW) / 2, y + imgH + 35, true);
		} catch (Throwable ignored) {
		}
	}

	private void populateDefaultExtensionModules() {
		modules.clear();

		// Динамическое сканирование всех файлов воркспейса Eclipse/EDT по правилам движка
		try {
			org.eclipse.core.resources.IWorkspaceRoot root = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot();
			root.accept(new org.eclipse.core.resources.IResourceVisitor() {
				@Override
				public boolean visit(org.eclipse.core.resources.IResource res) throws org.eclipse.core.runtime.CoreException {
					if (res instanceof org.eclipse.core.resources.IFile) {
						org.eclipse.core.resources.IFile file = (org.eclipse.core.resources.IFile) res;
						String fileName = file.getName().toLowerCase();
						if (fileName.endsWith(".bsl") || fileName.endsWith(".os")) {
							String modName = BslTestMarkerManager.resolveModuleName(file, null);
							// Исключаем служебные модули движка СП_
							if (EditorModuleSupport.isTestSetName(modName)) {
								List<BslTestMarkerManager.TestMethodInfo> tests = discoverTestsInFile(file, modName);
								if (!tests.isEmpty()) {
									ModuleNode modNode = new ModuleNode(modName, file.getProject().getName() + " (" + tests.size() + " тестов)");
									for (BslTestMarkerManager.TestMethodInfo t : tests) {
										modNode.tests.add(new TestItemNode(modNode, t.name, "Строка " + t.lineNumber));
									}
									modules.add(modNode);
								}
							}
						}
					}
					return true;
				}
			});
		} catch (Throwable t) {
			Activator.logError("Ошибка динамического обнаружения тестов в ExtensionTestsView", t);
		}
	}

	private static final java.util.regex.Pattern TEST_METHOD_PATTERN = java.util.regex.Pattern.compile(
			"(?i)^\\s*(?:(?:Процедура|Функция|Procedure|Function)\\s+([a-zA-Zа-яА-Я0-9_]+))\\s*(?:\\(.*)?",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);

	private static final java.util.regex.Pattern TEST_ANNOTATION_PATTERN = java.util.regex.Pattern.compile(
			"(?i)^\\s*(?:&Тест|&Test|//\\s*@test|//\\s*@тест|//\\s*Тест:|//\\s*Test:).*",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);

	private static List<BslTestMarkerManager.TestMethodInfo> discoverTestsInFile(org.eclipse.core.resources.IFile file, String modName) {
		List<BslTestMarkerManager.TestMethodInfo> result = new ArrayList<>();
		try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(file.getContents(true), java.nio.charset.StandardCharsets.UTF_8))) {
			String line;
			int lineNum = 0;
			boolean hasTestAnnotation = false;

			while ((line = reader.readLine()) != null) {
				lineNum++;
				String trimmed = line.trim();

				// Пропускаем пустые строки и комментарии (но проверяем аннотации)
				if (trimmed.isEmpty()) {
					continue;
				}

				if (TEST_ANNOTATION_PATTERN.matcher(line).matches()) {
					hasTestAnnotation = true;
					continue;
				}

				java.util.regex.Matcher m = TEST_METHOD_PATTERN.matcher(line);
				if (m.matches()) {
					String procName = m.group(1);
					boolean isExport = line.contains("Экспорт") || line.contains("Export")
							|| line.toLowerCase().contains("экспорт") || line.toLowerCase().contains("export");

					// YAxUnit / Specter строгий критерий: тест должен иметь аннотацию &Тест/&Test ИЛИ начинаться с префикса теста (Тест_/Test_)
					// Исключаем случайные экспортные методы вспомогательных процедур
					if (isExport && (hasTestAnnotation || isTestProcName(procName))) {
						result.add(new BslTestMarkerManager.TestMethodInfo(procName, lineNum));
					}
					hasTestAnnotation = false;
				} else {
					if (!trimmed.startsWith("//") && !trimmed.startsWith("&")) {
						hasTestAnnotation = false;
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return result;
	}

	private static boolean isTestProcName(String name) {
		if (name == null) return false;
		String lower = name.toLowerCase();
		return lower.startsWith("тест_")
				|| lower.startsWith("тест")
				|| lower.startsWith("test_")
				|| lower.startsWith("test")
				|| lower.startsWith("проверить_")
				|| lower.startsWith("проверить")
				|| lower.startsWith("check_")
				|| lower.startsWith("check");
	}

	private void createActions() {
		IToolBarManager tb = getViewSite().getActionBars().getToolBarManager();

		runSelectedAction = new Action("Запустить выбранный тест") {
			@Override
			public void run() {
				runSelectedTest();
			}
		};
		runSelectedAction.setToolTipText("Запустить выбранный тест через мост СП_Тестирование");
		try {
			runSelectedAction.setImageDescriptor(Activator.imageDescriptorFromPlugin(Activator.BUNDLE_ID, "icons/specter-tests.png"));
		} catch (Throwable ignored) {}
		tb.add(runSelectedAction);

		runModuleAction = new Action("Запустить модуль") {
			@Override
			public void run() {
				runSelectedModule();
			}
		};
		runModuleAction.setToolTipText("Запустить все тесты выбранного модуля");
		tb.add(runModuleAction);

		runAllAction = new Action("Запустить ВСЕ тесты расширения") {
			@Override
			public void run() {
				runAllExtensionTests();
			}
		};
		runAllAction.setToolTipText("Запустить все наборы тестов расширения СП_Тестирование");
		tb.add(runAllAction);

		tb.add(new Separator());

		refreshAction = new Action("Обновить список") {
			@Override
			public void run() {
				populateDefaultExtensionModules();
				viewer.refresh();
				viewer.expandAll();
			}
		};
		refreshAction.setToolTipText("Пересканировать тесты расширения и обновить статусы");
		tb.add(refreshAction);

		tb.add(new Action("Развернуть всё") {
			@Override public void run() { viewer.expandAll(); }
		});
		tb.add(new Action("Свернуть всё") {
			@Override public void run() { viewer.collapseAll(); }
		});
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
			Object first = sel.getFirstElement();
			if (first instanceof TestItemNode) {
				manager.add(runSelectedAction);
			} else if (first instanceof ModuleNode) {
				manager.add(runModuleAction);
			}
			manager.add(new Separator());
			manager.add(runAllAction);
			manager.add(refreshAction);
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookDoubleClick() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
				Object obj = sel.getFirstElement();
				if (obj instanceof TestItemNode) {
					runSelectedTest();
				} else if (obj instanceof ModuleNode) {
					runSelectedModule();
				}
			}
		});
	}

	/** Запуск выбранного одиночного теста */
	private void runSelectedTest() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		Object obj = sel.getFirstElement();
		if (!(obj instanceof TestItemNode)) return;

		TestItemNode test = (TestItemNode) obj;
		test.status = "running";
		viewer.refresh(test);

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.runSetJson(runId, test.parent.moduleName, test.name);
		LaunchMonitor.info("ExtensionTestsView: Запуск одиночного теста " + test.parent.moduleName + "." + test.name + " runId=" + runId);

		BridgeRunner.runAsync(runId, json,
			result -> {
				test.status = result.status;
				viewer.refresh(test);
			},
			err -> {
				test.status = "failed";
				viewer.refresh(test);
			}
		);
	}

	/** Запуск всех тестов выбранного модуля */
	private void runSelectedModule() {
		IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
		Object obj = sel.getFirstElement();
		ModuleNode mod = null;
		if (obj instanceof ModuleNode) {
			mod = (ModuleNode) obj;
		} else if (obj instanceof TestItemNode) {
			mod = ((TestItemNode) obj).parent;
		}
		if (mod == null) return;

		for (TestItemNode t : mod.tests) {
			t.status = "running";
		}
		viewer.refresh(mod);

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.runSetJson(runId, mod.moduleName, "");
		LaunchMonitor.info("ExtensionTestsView: Запуск набора " + mod.moduleName + " runId=" + runId);

		final ModuleNode finalMod = mod;
		BridgeRunner.runAsync(runId, json,
			result -> {
				for (TestItemNode t : finalMod.tests) {
					t.status = result.status;
				}
				viewer.refresh(finalMod);
			},
			err -> {
				for (TestItemNode t : finalMod.tests) {
					t.status = "failed";
				}
				viewer.refresh(finalMod);
			}
		);
	}

	/** Запуск всех наборов расширения */
	private void runAllExtensionTests() {
		for (ModuleNode m : modules) {
			for (TestItemNode t : m.tests) {
				t.status = "running";
			}
		}
		viewer.refresh();

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.commandsJson(runId);
		LaunchMonitor.info("ExtensionTestsView: Запуск всех тестов расширения runId=" + runId);

		BridgeRunner.runAsync(runId, json,
			result -> {
				for (ModuleNode m : modules) {
					for (TestItemNode t : m.tests) {
						t.status = result.status;
					}
				}
				viewer.refresh();
			},
			err -> {
				for (ModuleNode m : modules) {
					for (TestItemNode t : m.tests) {
						t.status = "failed";
					}
				}
				viewer.refresh();
			}
		);
	}

	private void updateStatusFromResult(BridgeResult r) {
		if (r == null) return;
		statusMessage = "Последний прогон " + r.runId + ": " + r.status.toUpperCase()
				+ " (" + r.passedCount() + " ✓ / " + r.failedCount() + " ✗)";
	}

	@Override
	public void setFocus() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.getControl().setFocus();
		}
	}

	@Override
	public void dispose() {
		try {
			org.eclipse.core.resources.ResourcesPlugin.getWorkspace().removeResourceChangeListener(workspaceChangeListener);
		} catch (Throwable ignored) {}
		BridgeResultStore.get().removeListener(storeListener);
		if (specterLogoImage != null && !specterLogoImage.isDisposed()) {
			specterLogoImage.dispose();
		}
		super.dispose();
	}

	// Структура узлов
	public static final class ModuleNode {
		public final String moduleName;
		public final String description;
		public final List<TestItemNode> tests = new ArrayList<>();
		public ModuleNode(String name, String desc) {
			this.moduleName = name;
			this.description = desc;
		}
	}

	public static final class TestItemNode {
		public final ModuleNode parent;
		public final String name;
		public final String description;
		public String status = "idle"; // idle, running, passed, failed
		public TestItemNode(ModuleNode parent, String name, String desc) {
			this.parent = parent;
			this.name = name;
			this.description = desc;
		}
	}

	private static final class ExtensionTreeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object input) {
			return ((ExtensionTestsView) input).modules.toArray();
		}
		@Override
		public Object[] getChildren(Object el) {
			if (el instanceof ModuleNode) return ((ModuleNode) el).tests.toArray();
			return new Object[0];
		}
		@Override public Object getParent(Object el) {
			if (el instanceof TestItemNode) return ((TestItemNode) el).parent;
			return null;
		}
		@Override public boolean hasChildren(Object el) {
			return el instanceof ModuleNode && !((ModuleNode) el).tests.isEmpty();
		}
		@Override public void inputChanged(Viewer v, Object o, Object n) {}
	}

	private static final class ExtensionTreeLabelProvider extends LabelProvider implements IColorProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ModuleNode) {
				ModuleNode m = (ModuleNode) element;
				return "📦 " + m.moduleName + "  [" + m.tests.size() + " тестов] — " + m.description;
			}
			if (element instanceof TestItemNode) {
				TestItemNode t = (TestItemNode) element;
				String badge = "";
				if ("passed".equals(t.status)) badge = " [✓ PASSED]";
				else if ("failed".equals(t.status)) badge = " [✗ FAILED]";
				else if ("running".equals(t.status)) badge = " [⟳ ИСПОЛНЯЕТСЯ...]";
				return "🔬 " + t.name + badge + " — " + t.description;
			}
			return String.valueOf(element);
		}

		@Override
		public Color getForeground(Object element) {
			Display d = Display.getCurrent();
			if (d == null) d = Display.getDefault();
			if (d == null || d.isDisposed()) return null;

			if (element instanceof ModuleNode) {
				return d.getSystemColor(SWT.COLOR_DARK_BLUE);
			}
			if (element instanceof TestItemNode) {
				TestItemNode t = (TestItemNode) element;
				if ("passed".equals(t.status)) return d.getSystemColor(SWT.COLOR_DARK_GREEN);
				if ("failed".equals(t.status)) return d.getSystemColor(SWT.COLOR_RED);
				if ("running".equals(t.status)) return d.getSystemColor(SWT.COLOR_DARK_MAGENTA);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}
}
