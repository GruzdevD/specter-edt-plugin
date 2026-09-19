package ru.ozon.uitp.e2e;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Полноценный UI-активатор плагина Specter в соответствии со стандартами Eclipse RCP.
 */
public final class Activator extends AbstractUIPlugin {

	public static final String BUNDLE_ID = "ru.ozon.uitp.e2e";
	private static Activator plugin;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		ru.ozon.uitp.e2e.views.BslTestMarkerManager.init();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Возвращает сохраненный в IPreferenceStore путь к тестам Vanessa Automation.
	 */
	public static String getVanessaTestsPath() {
		if (plugin != null && plugin.getPreferenceStore() != null) {
			return plugin.getPreferenceStore().getString(
					ru.ozon.uitp.e2e.preferences.SpecterPreferencePage.P_VANESSA_TESTS_PATH);
		}
		return "";
	}

	public static void logInfo(String message) {
		if (plugin != null) {
			plugin.getLog().log(new Status(IStatus.INFO, BUNDLE_ID, message));
		}
	}

	public static void logError(String message, Throwable throwable) {
		if (plugin != null) {
			plugin.getLog().log(new Status(IStatus.ERROR, BUNDLE_ID, message, throwable));
		}
	}
}