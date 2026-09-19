package ru.ozon.uitp.e2e.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;

/**
 * Действие: «Запустить ВСЕ тесты текущего BSL-модуля (СП)».
 */
public class RunCurrentModuleTestsAction implements IEditorActionDelegate, IWorkbenchWindowActionDelegate {

	private IEditorPart activeEditor;

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

		// Запускаем весь набор
		BslTestMarkerManager.runSingleTest(moduleName, "");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
