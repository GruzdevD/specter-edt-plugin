package ru.ozon.uitp.e2e.views;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;

import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Менеджер обнаружения тестов в окне редактора кода 1C:EDT (YAxUnit / Specter паттерн).
 * 
 * Находит объявления тестов (процедур и функций) в открытом BSL-редакторе и
 * устанавливает маркеры запуска на вертикальной линейке (Gutter Ruler)
 * прямо напротив начала строки объявления теста.
 */
public final class BslTestMarkerManager {

	public static final String MARKER_TYPE = "ru.ozon.uitp.e2e.testMarker";
	public static final String ATTR_TEST_NAME = "testName";
	public static final String ATTR_MODULE_NAME = "moduleName";

	private static final Pattern TEST_METHOD_PATTERN = Pattern.compile(
			"(?i)^\\s*(?:(?:Процедура|Функция|Procedure|Function)\\s+([a-zA-Zа-яА-Я0-9_]+))\\s*(?:\\(.*)?",
			Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern TEST_ANNOTATION_PATTERN = Pattern.compile(
			"(?i)^\\s*(?:&Тест|&Test|//\\s*@test|//\\s*@тест|//\\s*Тест:|//\\s*Test:).*",
			Pattern.UNICODE_CHARACTER_CLASS);

	private static boolean initialized = false;

	private BslTestMarkerManager() {
	}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		// 1. Слушатель изменений файлов в Workspace (сохранение / правка BSL-файлов)
		try {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
					IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.POST_BUILD);
		} catch (Throwable t) {
			Activator.logError("Не удалось подключить ResourceChangeListener для BSL", t);
		}

		// 2. Слушатель открытия / активации редакторов в UI
		try {
			Display display = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : Display.getDefault();
			if (display != null && !display.isDisposed()) {
				display.asyncExec(() -> {
					try {
						IWorkbench wb = PlatformUI.getWorkbench();
						if (wb != null) {
							wb.addWindowListener(windowListener);
							for (IWorkbenchWindow win : wb.getWorkbenchWindows()) {
								hookWindow(win);
							}
						}
					} catch (Throwable t) {
						Activator.logError("Не удалось инициализировать слушатели окон EDT", t);
					}
				});
			}
		} catch (Throwable t) {
			// Режим без GUI
		}
	}

	private static void hookWindow(IWorkbenchWindow win) {
		if (win == null) {
			return;
		}
		for (IWorkbenchPage page : win.getPages()) {
			page.addPartListener(partListener);
			for (IEditorReference editorRef : page.getEditorReferences()) {
				IEditorPart editor = editorRef.getEditor(false);
				if (editor != null) {
					updateEditorMarkers(editor);
				}
			}
		}
	}

	private static final IWindowListener windowListener = new IWindowListener() {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			hookWindow(window);
		}

		@Override public void windowClosed(IWorkbenchWindow window) {}
		@Override public void windowActivated(IWorkbenchWindow window) {
			hookWindow(window);
		}
		@Override public void windowDeactivated(IWorkbenchWindow window) {}
	};

	private static final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		@Override public void partClosed(IWorkbenchPartReference partRef) {}
		@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}
		@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override public void partHidden(IWorkbenchPartReference partRef) {}
		@Override public void partVisible(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		private void checkAndUpdate(IWorkbenchPartReference partRef) {
			if (partRef != null) {
				org.eclipse.ui.IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof IEditorPart) {
					updateEditorMarkers((IEditorPart) part);
				}
			}
		}
	};

	private static final IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta d) throws CoreException {
						IResource res = d.getResource();
						if (res instanceof IFile) {
							String name = res.getName().toLowerCase();
							if (name.endsWith(".bsl") || name.endsWith(".os")) {
								updateFileMarkers((IFile) res, null);
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
				Activator.logError("Ошибка обновления маркеров по ресурсам", e);
			}
		}
	};

	/**
	 * Сканирует открытый редактор и размещает маркеры запуска тестов на строках объявлений.
	 */
	public static void updateEditorMarkers(IEditorPart editor) {
		if (editor == null) {
			return;
		}

		IEditorInput input = editor.getEditorInput();
		IFile file = ResourceUtil.getFile(input);
		if (file == null || !file.exists()) {
			return;
		}

		String fileName = file.getName().toLowerCase();
		if (!fileName.endsWith(".bsl") && !fileName.endsWith(".os")) {
			return;
		}

		updateFileMarkers(file, editor);
	}

	public static void updateFileMarkers(IFile file, IEditorPart editor) {
		if (file == null || !file.exists()) {
			return;
		}

		try {
			// Удаляем старые маркеры тестов из этого файла
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);

			CommonModule cm = editor != null ? EditorModuleSupport.activeCommonModule(editor) : null;
			String modName = resolveModuleName(file, cm);
			boolean isTestModule = isTestModule(file, modName);

			List<TestMethodInfo> tests = discoverTestsInFile(file, cm, isTestModule);

			for (TestMethodInfo t : tests) {
				IMarker marker = file.createMarker(MARKER_TYPE);
				Map<String, Object> attrs = new HashMap<>();
				attrs.put(IMarker.LINE_NUMBER, t.lineNumber);
				attrs.put(IMarker.MESSAGE, "▶ Запустить тест СП: " + t.name + " (" + modName + ")");
				attrs.put(ATTR_TEST_NAME, t.name);
				attrs.put(ATTR_MODULE_NAME, modName);
				attrs.put(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				attrs.put(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				marker.setAttributes(attrs);
			}

		} catch (CoreException e) {
			Activator.logError("Ошибка создания маркеров тестов в: " + file.getFullPath(), e);
		}
	}

	/**
	 * Определяет имя модуля из метамодели 1C:EDT или из пути к файлу.
	 * В EDT путь к общему модулю: src/CommonModules/<ИмяМодуля>/Module.bsl
	 */
	public static String resolveModuleName(IFile file, CommonModule cm) {
		if (cm != null) {
			String name = EditorModuleSupport.moduleName(cm);
			if (name != null && !name.trim().isEmpty()) {
				return name.trim();
			}
		}

		if (file == null) {
			return "СП_Тестирование";
		}

		String fileName = file.getName();
		if (fileName.equalsIgnoreCase("Module.bsl") || fileName.equalsIgnoreCase("Модуль.bsl")
				|| fileName.equalsIgnoreCase("Module.os")) {
			if (file.getParent() != null) {
				return file.getParent().getName();
			}
		}

		return fileName.replaceAll("\\.[^.]+$", "");
	}

	public static boolean isTestModule(IFile file, String moduleName) {
		if (EditorModuleSupport.isTestSetName(moduleName)) {
			return true;
		}
		if (file != null) {
			String fullPath = file.getFullPath().toString().toLowerCase();
			if (fullPath.contains("/тест") || fullPath.contains("/test") || fullPath.contains("сп_")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Запуск одиночного теста по маркеру на линейке редактора.
	 */
	public static void runTestFromMarker(IMarker marker) {
		if (marker == null || !marker.exists()) {
			return;
		}
		try {
			String testName = (String) marker.getAttribute(ATTR_TEST_NAME);
			String moduleName = (String) marker.getAttribute(ATTR_MODULE_NAME);
			if (testName != null && moduleName != null) {
				runSingleTest(moduleName, testName);
			}
		} catch (CoreException e) {
			Activator.logError("Ошибка чтения атрибутов тестового маркера", e);
		}
	}

	/**
	 * Запуск одного теста из модуля через живой мост СП_Тестирование.
	 */
	public static void runSingleTest(String moduleName, String testName) {
		String runId = LaunchMonitor.newRunId();
		String commands = BridgeScenario.runSetJson(runId, moduleName, testName);

		LaunchMonitor.info("EDT: Запуск теста " + moduleName + "." + (testName.isEmpty() ? "*" : testName) + " runId=" + runId);

		BridgeRunner.runAsync(runId, commands,
				result -> {
					BridgeResultStore.get().set(result);
					LaunchMonitor.info("EDT: Тест " + (testName.isEmpty() ? moduleName : testName) + " завершён: " + result.status);
				},
				error -> {
					LaunchMonitor.info("EDT: Ошибка запуска теста: " + error);
				});
	}

	private static List<TestMethodInfo> discoverTestsInFile(IFile file, CommonModule cm, boolean isTestModule) {
		List<TestMethodInfo> result = new ArrayList<>();

		// Текстовое сканирование исходного кода файла для точного определения номера строки
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(true), "UTF-8"))) {
			String line;
			int lineNum = 0;
			boolean hasTestAnnotation = false;

			while ((line = reader.readLine()) != null) {
				lineNum++;

				if (TEST_ANNOTATION_PATTERN.matcher(line).matches()) {
					hasTestAnnotation = true;
					continue;
				}

				Matcher m = TEST_METHOD_PATTERN.matcher(line);
				if (m.matches()) {
					String procName = m.group(1);
					boolean isExport = line.contains("Экспорт") || line.contains("Export")
							|| line.toLowerCase().contains("экспорт") || line.toLowerCase().contains("export");

					if (hasTestAnnotation || (isTestModule && isExport) || isTestProcName(procName) || isTestModule) {
						result.add(new TestMethodInfo(procName, lineNum));
					}
					hasTestAnnotation = false;
				} else {
					if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
						hasTestAnnotation = false;
					}
				}
			}
		} catch (Throwable t) {
			Activator.logError("Ошибка разбора BSL-файла для обнаружения тестов: " + file.getName(), t);
		}

		return result;
	}

	private static boolean isTestProcName(String name) {
		if (name == null) {
			return false;
		}
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

	public static class TestMethodInfo {
		public final String name;
		public final int lineNumber;

		public TestMethodInfo(String name, int lineNumber) {
			this.name = name;
			this.lineNumber = lineNumber;
		}
	}
}
