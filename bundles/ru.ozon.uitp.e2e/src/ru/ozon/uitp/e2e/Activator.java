package ru.ozon.uitp.e2e;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator бандла. На этом этапе служебный (константа ID + название), реальная
 * логика вынесена в {@link ru.ozon.uitp.e2e.launcher.BridgeLaunchHelper}.
 */
public final class Activator implements BundleActivator {

	/** Идентификатор бандла (Bundle-SymbolicName). */
	public static final String BUNDLE_ID = "ru.ozon.uitp.e2e";

	@Override
	public void start(BundleContext context) {
		// no-op: ленивая инициализация не требуется
	}

	@Override
	public void stop(BundleContext context) {
		// no-op
	}
}
