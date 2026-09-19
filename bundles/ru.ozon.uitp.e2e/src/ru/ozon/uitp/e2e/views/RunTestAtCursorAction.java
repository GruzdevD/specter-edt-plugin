package ru.ozon.uitp.e2e.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;

/**
 * Действие в контекстном меню редактора кода EDT: «Запустить тест СП».
 * Определяет имя метода под курсором или ближайший тест и запускает его.
 */
public class RunTestAtCursorAction implements IEditorActionDelegate {

	private IEditorPart activeEditor;

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.activeEditor = targetEditor;
	}

	@Override
	public void run(IAction action) {
		if (activeEditor == null) {
			return;
		}

		IFile file = ResourceUtil.getFile(activeEditor.getEditorInput());
		if (file == null) {
			return;
		}

		CommonModule cm = EditorModuleSupport.activeCommonModule(activeEditor);
		String moduleName = cm != null ? EditorModuleSupport.moduleName(cm) : file.getName().replaceAll("\\.[^.]+$", "");

		// Получаем позицию курсора
		ISelection sel = activeEditor.getSite().getSelectionProvider().getSelection();
		int selectedLine = -1;
		if (sel instanceof ITextSelection) {
			selectedLine = ((ITextSelection) sel).getStartLine() + 1;
		}

		// Ищем маркер на этой или ближайшей строке
		try {
			IMarker[] markers = file.findMarkers(BslTestMarkerManager.MARKER_TYPE, false, IResource.DEPTH_ZERO);
			IMarker matched = null;
			int minDistance = Integer.MAX_VALUE;

			for (IMarker m : markers) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				if (line == selectedLine) {
					matched = m;
					break;
				}
				if (selectedLine > 0 && line > 0 && Math.abs(line - selectedLine) < minDistance) {
					minDistance = Math.abs(line - selectedLine);
					matched = m;
				}
			}

			if (matched != null) {
				BslTestMarkerManager.runTestFromMarker(matched);
				return;
			}
		} catch (Throwable ignored) {
		}

		// Если маркер не найден, запускаем весь набор модуля
		BslTestMarkerManager.runSingleTest(moduleName, "");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
