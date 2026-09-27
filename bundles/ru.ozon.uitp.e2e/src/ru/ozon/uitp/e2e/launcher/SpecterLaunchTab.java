package ru.ozon.uitp.e2e.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

/**
 * Главная вкладка конфигурации запуска Specter (по образцу YAxUnit LaunchConfigurationTab):
 * выбор базовой конфигурации запуска EDT (RuntimeClient) + порт TESTMANAGER.
 * Все остальные параметры клиента (приложение, ИБ, пользователь) наследуются
 * от базовой конфигурации — настраивать их здесь не нужно.
 */
public class SpecterLaunchTab extends AbstractLaunchConfigurationTab {

	private Combo baseConfigCombo;
	private Spinner testingPortSpinner;
	private Label hintLabel;

	private final List<String> baseConfigNames = new ArrayList<>();

	@Override
	public void createControl(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		setControl(root);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(root);

		Group baseGroup = new Group(root, SWT.NONE);
		baseGroup.setText("Базовая конфигурация запуска EDT");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(baseGroup);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(baseGroup);

		Label baseLabel = new Label(baseGroup, SWT.NONE);
		baseLabel.setText("Конфигурация запуска EDT:");
		GridDataFactory.swtDefaults().applyTo(baseLabel);

		baseConfigCombo = new Combo(baseGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(baseConfigCombo);
		fillBaseConfigs("");
		baseConfigCombo.addModifyListener(e -> updateLaunchConfigurationDialog());
		baseConfigCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		hintLabel = new Label(baseGroup, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(hintLabel);
		hintLabel.setText("Приложение, инфобаза и пользователь берутся из базовой конфигурации. "
				+ "Specter запускает её клон «Specter: <имя>» с маркером моста "
				+ "SPECTER_START_BRIDGE и режимом клиент-тестирования.");

		Group testGroup = new Group(root, SWT.NONE);
		testGroup.setText("Клиент-тестирование (канал B)");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(testGroup);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(testGroup);

		Label portLabel = new Label(testGroup, SWT.NONE);
		portLabel.setText("Порт TESTMANAGER (0 = выключить):");
		GridDataFactory.swtDefaults().applyTo(portLabel);

		testingPortSpinner = new Spinner(testGroup, SWT.BORDER);
		testingPortSpinner.setMinimum(0);
		testingPortSpinner.setMaximum(65535);
		testingPortSpinner.setSelection(4811);
		GridDataFactory.swtDefaults().applyTo(testingPortSpinner);
		testingPortSpinner.addModifyListener(e -> updateLaunchConfigurationDialog());
	}

	/** Заполняет список базовых конфигураций (RuntimeClient); приоритет — конфигурации проекта. */
	private void fillBaseConfigs(String preferred) {
		baseConfigNames.clear();
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		org.eclipse.debug.core.ILaunchConfigurationType rtc = lm.getLaunchConfigurationType(
				BridgeLaunchHelper.RUNTIME_CLIENT_TYPE_ID);
		if (rtc != null) {
			try {
				for (ILaunchConfiguration c : lm.getLaunchConfigurations(rtc)) {
					// Клоны Specter в базовые не предлагаем — иначе рекурсия.
					if (c.getName().startsWith(SpecterLaunchAttributes.CLONE_PREFIX)) {
						continue;
					}
					baseConfigNames.add(c.getName());
				}
			} catch (CoreException e) {
				setErrorMessage("Не удалось прочитать конфигурации запуска: " + e.getMessage());
			}
		}
		baseConfigCombo.setItems(baseConfigNames.toArray(new String[0]));
		if (preferred != null && !preferred.isBlank()) {
			int idx = baseConfigNames.indexOf(preferred);
			if (idx >= 0) {
				baseConfigCombo.select(idx);
			}
		} else if (baseConfigNames.size() == 1) {
			baseConfigCombo.select(0);
		} else {
			// Подсказка по умолчанию: конфигурация по имени из BridgeLaunchHelper.
			int idx = baseConfigNames.indexOf(BridgeLaunchHelper.DEFAULT_SOURCE_CONFIGURATION_NAME);
			if (idx >= 0) {
				baseConfigCombo.select(idx);
			}
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (!baseConfigNames.isEmpty()) {
			config.setAttribute(SpecterLaunchAttributes.BASE_LAUNCH_CONFIG, baseConfigNames.get(0));
		}
		config.setAttribute(SpecterLaunchAttributes.TESTING_PORT, BridgeLaunchHelper.DEFAULT_TESTING_PORT);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		String base = safeAttribute(config, SpecterLaunchAttributes.BASE_LAUNCH_CONFIG, ""); //$NON-NLS-1$
		fillBaseConfigs(base);
		if (!base.isBlank()) {
			int idx = baseConfigNames.indexOf(base);
			if (idx >= 0) {
				baseConfigCombo.select(idx);
			} else {
				baseConfigCombo.setText(base); // база могла быть удалена — покажем как есть
			}
		}
		testingPortSpinner.setSelection(safeInt(config, SpecterLaunchAttributes.TESTING_PORT,
				BridgeLaunchHelper.DEFAULT_TESTING_PORT));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (baseConfigCombo.getSelectionIndex() >= 0) {
			config.setAttribute(SpecterLaunchAttributes.BASE_LAUNCH_CONFIG,
					baseConfigCombo.getItem(baseConfigCombo.getSelectionIndex()));
		} else {
			config.setAttribute(SpecterLaunchAttributes.BASE_LAUNCH_CONFIG, baseConfigCombo.getText().trim());
		}
		config.setAttribute(SpecterLaunchAttributes.TESTING_PORT, testingPortSpinner.getSelection());
	}

	@Override
	public String getName() {
		return "Specter";
	}

	@Override
	public boolean isValid(ILaunchConfiguration config) {
		String base = safeAttribute(config, SpecterLaunchAttributes.BASE_LAUNCH_CONFIG, ""); //$NON-NLS-1$
		if (base.isBlank()) {
			setErrorMessage("Выбери базовую конфигурацию запуска EDT");
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	private static String safeAttribute(ILaunchConfiguration c, String key, String def) {
		try {
			return c.getAttribute(key, def);
		} catch (CoreException e) {
			return def;
		}
	}

	private static int safeInt(ILaunchConfiguration c, String key, int def) {
		try {
			return c.getAttribute(key, def);
		} catch (CoreException e) {
			return def;
		}
	}
}
