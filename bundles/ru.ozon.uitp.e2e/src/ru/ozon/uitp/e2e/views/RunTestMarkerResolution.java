package ru.ozon.uitp.e2e.views;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.swt.graphics.Image;
import ru.ozon.uitp.e2e.Activator;

/**
 * Разрешение для быстрого запуска теста прямо по маркеру/лампочке на полях редактора BSL.
 */
public class RunTestMarkerResolution implements IMarkerResolution, IMarkerResolution2 {

	@Override
	public String getLabel() {
		return "Запустить этот тест (СП)";
	}

	@Override
	public String getDescription() {
		return "Выполняет запуск теста через живой мост СП_Тестирование и выводит отчет в панель результатов.";
	}

	@Override
	public Image getImage() {
		try {
			org.eclipse.jface.resource.ImageDescriptor desc = Activator.imageDescriptorFromPlugin(
					Activator.BUNDLE_ID, "icons/specter-tests.png");
			return desc != null ? desc.createImage() : null;
		} catch (Throwable t) {
			return null;
		}
	}

	@Override
	public void run(IMarker marker) {
		BslTestMarkerManager.runTestFromMarker(marker);
	}
}
