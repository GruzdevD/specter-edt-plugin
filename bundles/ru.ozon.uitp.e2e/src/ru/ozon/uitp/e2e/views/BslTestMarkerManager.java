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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
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
 * Менеджер обнаружения тестов в окне редактора кода EDT (YAxUnit-паттерн).
 * 
 * Находит объявления тестов (процедур и функций) в открытом BSL-редакторе и
 * устанавливает маркеры/кнопки запуска на вертикальной линейке (Gutter Ruler)
 * прямо напротив начала строки объявления теста.
 * 
 * Поддерживает:
 * 1. Экспортные процедуры тестовых модулей (по метамодели CommonModule).
 * 2. Текстовый анализ аннотаций (&Тест, //@test) и префиксов (Тест_*, Test_*).
 * 3. Одиночный запуск конкретного теста по клику на иконку или через контекстное меню.
 */
public final class BslTestMarkerManager {

	public static final String MARKER_TYPE = "ru.ozon.uitp.e2e.testMarker";
	public static final String ATTR_TEST_NAME = "testName";
	public static final String ATTR_MODULE_NAME = "moduleName";

	private static final Pattern TEST_METHOD_PATTERN = Pattern.compile(
			"(?i)^\\s*(Процедура|Функция)\\s+([a-zA-Zа-яА-Я0-9_]+)\\s*\\(.*", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern TEST_ANNOTATION_PATTERN = Pattern.compile(
			"(?i)^\\s*(&Тест|//\\s*@test).*", Pattern.UNICODE_CHARACTER_CLASS);

	private static boolean initialized = false;

	private BslTestMarkerManager() {
	}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		try {
			Display.getDefault().asyncExec(() -> {
				try {
					IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (win != null) {
						IWorkbenchPage page = win.getActivePage();
						if (page != null) {
							page.addPartListener(partListener);
							IEditorPart active = page.getActiveEditor();
							if (active != null) {
								updateEditorMarkers(active);
							}
						}
					}
				} catch (Throwable t) {
					Activator.logError("Не удалось инициализировать слушатель редакторов BSL", t);
				}
			});
		} catch (Throwable t) {
			// Вне графического интерфейса
		}
	}

	private static final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			// Маркеры привязаны к ресурсу или очищаются
		}

		@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override public void partHidden(IWorkbenchPartReference partRef) {}
		@Override public void partVisible(IWorkbenchPartReference partRef) {}
		@Override public void partInputChanged(IWorkbenchPartReference partRef) {
			checkAndUpdate(partRef);
		}

		private void checkAndUpdate(IWorkbenchPartReference partRef) {
			if (partRef != null && partRef.getPart(false) instanceof IEditorPart) {
				updateEditorMarkers((IEditorPart) partRef.getPart(false));
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

		try {
			// Сначала удаляем старые маркеры тестов из этого файла
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);

			CommonModule cm = EditorModuleSupport.activeCommonModule(editor);
			String modName = cm != null ? EditorModuleSupport.moduleName(cm) : file.getName().replaceAll("\\.[^.]+$", "");
			boolean isTestModule = cm != null ? EditorModuleSupport.isTestSetName(modName) : true;

			List<TestMethodInfo> tests = discoverTestsInFile(file, cm, isTestModule);

			for (TestMethodInfo t : tests) {
				IMarker marker = file.createMarker(MARKER_TYPE);
				Map<String, Object> attrs = new HashMap<>();
				attrs.put(IMarker.LINE_NUMBER, t.lineNumber);
				attrs.put(IMarker.MESSAGE, "Запустить тест СП: " + t.name + " (" + modName + ")");
				attrs.put(ATTR_TEST_NAME, t.name);
				attrs.put(ATTR_MODULE_NAME, modName);
				attrs.put(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				attrs.put(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				marker.setAttributes(attrs);
			}

		} catch (CoreException e) {
			Activator.logError("Ошибка создания маркеров тестов в редакторе: " + file.getFullPath(), e);
		}
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

		LaunchMonitor.info("EDT Gutter: Запуск одиночного теста " + moduleName + "." + testName + " runId=" + runId);

		BridgeRunner.runAsync(runId, commands,
				result -> {
					BridgeResultStore.get().set(result);
					LaunchMonitor.info("EDT Gutter: Тест " + testName + " завершён: " + result.status);
				},
				error -> {
					LaunchMonitor.info("EDT Gutter: Ошибка запуска теста " + testName + ": " + error);
				});
	}

	private static List<TestMethodInfo> discoverTestsInFile(IFile file, CommonModule cm, boolean isTestModule) {
		List<TestMethodInfo> result = new ArrayList<>();

		// 1. Попытка извлечь методы через метамодель EDT (если доступна)
		if (cm != null) {
			try {
				Module m = cm.getModule();
				if (m != null) {
					for (Method method : m.getMethods()) {
						String mName = method.getName();
						if (isTestModule || isTestProcName(mName)) {
							// Приблизительный номер строки найдем через текстовый парсер для точности позиционирования
						}
					}
				}
			} catch (Throwable ignored) {
			}
		}

		// 2. Текстовое сканирование исходного кода файла для точного определения номера строки
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
					String procName = m.group(2);
					boolean isExport = line.contains("Экспорт") || line.contains("Export");

					if (hasTestAnnotation || (isTestModule && isExport) || isTestProcName(procName)) {
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
				|| lower.startsWith("test");
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
