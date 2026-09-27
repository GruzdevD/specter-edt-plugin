package ru.ozon.uitp.e2e.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;

/**
 * Группа вкладок конфигурации запуска Specter: одна главная вкладка.
 * Сложные вкладки EDT (приложение/ИБ/параметры) не дублируем —
 * всё наследуется от базовой конфигурации запуска.
 */
public class SpecterLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		setTabs(new SpecterLaunchTab[] { new SpecterLaunchTab() });
	}
}
