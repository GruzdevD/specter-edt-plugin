package ru.ozon.uitp.e2e.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
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

		// Ищем маркер на этой строке или ближайший маркер выше курсора
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

		// Если одиночный маркер не найден, запускаем все тесты данного модуля
		BslTestMarkerManager.runSingleTest(moduleName, "");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
