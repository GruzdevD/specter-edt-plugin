package ru.ozon.uitp.e2e.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import ru.ozon.uitp.e2e.Activator;

/**
 * Действие в контекстном меню редактора и на панели инструментов EDT: «Запустить тест СП».
 * Находит тест под курсором или ближайший тест в открытом модуле и запускает его.
 */
public class RunTestAtCursorAction implements IEditorActionDelegate, IWorkbenchWindowActionDelegate {

	private IEditorPart activeEditor;

	public RunTestAtCursorAction() {
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.activeEditor = targetEditor;
	}

	@Override
	public void init(IWorkbenchWindow window) {
		if (window != null && window.getActivePage() != null) {
			this.activeEditor = window.getActivePage().getActiveEditor();
		}
	}

	@Override
	public void dispose() {
		this.activeEditor = null;
	}

	@Override
	public void run(IAction action) {
		IEditorPart editor = activeEditor;
		if (editor == null) {
			editor = EditorModuleSupport.activeEditor();
		}
		if (editor == null && PlatformUI.isWorkbenchRunning()) {
			IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (win != null && win.getActivePage() != null) {
				editor = win.getActivePage().getActiveEditor();
			}
		}

		if (editor == null) {
			return;
		}

		IFile file = ResourceUtil.getFile(editor.getEditorInput());
		if (file == null) {
			return;
		}

		CommonModule cm = EditorModuleSupport.activeCommonModule(editor);
		String moduleName = BslTestMarkerManager.resolveModuleName(file, cm);

		// Получаем номер строки курсора
		int selectedLine = -1;
		if (editor.getSite() != null && editor.getSite().getSelectionProvider() != null) {
			ISelection sel = editor.getSite().getSelectionProvider().getSelection();
			if (sel instanceof ITextSelection) {
				selectedLine = ((ITextSelection) sel).getStartLine() + 1;
			}
		}

		IMarker matched = null;
		try {
			matched = resolveNearestTestMarker(file, selectedLine);
		} catch (CoreException e) {
			// НЕ маскируем API-сбой и НЕ уходим в фолбек «запустить весь модуль» при внутренней
			// ошибке (это было бы неожиданным 40-минутным прогоном вместо одиночного теста).
			// Фиксируем первопричину в Error Log и прерываем операцию.
			Activator.logError("Ошибка поиска теста под курсором в " + file.getFullPath(), e);
			return;
		}

		if (matched != null) {
			BslTestMarkerManager.runTestFromMarker(matched);
			return;
		}

		// Подходящего маркера под курсором нет — запускаем набор модуля целиком
		// (осознанный UX-фолбек: BslTestMarkerManager.runSingleTest(moduleName, "") = все тесты).
		BslTestMarkerManager.runSingleTest(moduleName, "");
	}

	/**
	 * Выбирает тестовый маркер под/рядом с курсором. Приоритет (по убыванию):
	 *  1) маркер ТОЧНО на строке курсора;
	 *  2) ближайший маркер ВЫШЕ курсора — тело теста идёт ниже его сигнатуры, поэтому курсор
	 *     внутри тела текущего теста должен запускать именно этот тест, а не следующий (эвристика
	 *     «минимального модуля дистанции до любого маркера» ошибочно запускала СЛЕДУЮЩИЙ тест,
	 *     когда курсор стоял у нижней границы тела предыдущего);
	 *  3) иначе — ближайший маркер ниже курсора.
	 */
	private static IMarker resolveNearestTestMarker(IFile file, int selectedLine) throws CoreException {
		IMarker[] markers = file.findMarkers(BslTestMarkerManager.MARKER_TYPE, false, IResource.DEPTH_ZERO);
		if (markers.length == 0) {
			return null;
		}

		IMarker exact = null;
		IMarker bestAbove = null;
		int bestAboveDist = Integer.MAX_VALUE;
		IMarker bestBelow = null;
		int bestBelowDist = Integer.MAX_VALUE;

		for (IMarker m : markers) {
			int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
			if (line <= 0) {
				continue;
			}
			if (selectedLine == line) {
				exact = m;
				break; // точное попадание — лучший возможный исход
			}
			if (selectedLine <= 0) {
				return m; // строка курсора неизвестна — берём первый маркер
			}
			if (line < selectedLine) {
				int d = selectedLine - line;
				if (d < bestAboveDist) {
					bestAboveDist = d;
					bestAbove = m;
				}
			} else {
				int d = line - selectedLine;
				if (d < bestBelowDist) {
					bestBelowDist = d;
					bestBelow = m;
				}
			}
		}
		if (exact != null) {
			return exact;
		}
		if (bestAbove != null) {
			return bestAbove;
		}
		return bestBelow;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
