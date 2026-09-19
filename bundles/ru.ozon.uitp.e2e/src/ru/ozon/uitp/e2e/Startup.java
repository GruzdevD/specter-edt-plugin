package ru.ozon.uitp.e2e;

import org.eclipse.ui.IStartup;
import ru.ozon.uitp.e2e.views.BslTestMarkerManager;

/**
 * Ранний старт плагина Specter в среде 1C:EDT.
 * Гарантирует немедленную активацию слушателей BSL-редакторов и расстановку маркеров тестов.
 */
public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		Activator.logInfo("Specter: Запуск модуля автообнаружения тестов BSL");
		BslTestMarkerManager.init();
	}
}
