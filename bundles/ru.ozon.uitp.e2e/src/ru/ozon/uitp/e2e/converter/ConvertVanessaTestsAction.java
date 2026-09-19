package ru.ozon.uitp.e2e.converter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Действие открытия мастера/диалога конвертации тестов Vanessa Automation в модули расширения 1C.
 */
public class ConvertVanessaTestsAction extends Action implements IWorkbenchWindowActionDelegate {

	public static final String ID = "ru.ozon.uitp.e2e.converter.ConvertVanessaTestsAction";
	private IWorkbenchWindow window;

	public ConvertVanessaTestsAction() {
		super("Конвертировать тесты Vanessa Automation...");
		setId(ID);
		setToolTipText("Распознать сценарии Vanessa Automation и конвертировать их в программные модули расширения СП_Тестирование");
	}

	@Override
	public void run() {
		Shell shell = window != null ? window.getShell() : Display.getDefault().getActiveShell();
		VanessaConvertDialog dialog = new VanessaConvertDialog(shell);
		dialog.open();
	}

	@Override
	public void run(IAction action) {
		run();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}
}
