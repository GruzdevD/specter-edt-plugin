package ru.ozon.uitp.e2e.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import ru.ozon.uitp.e2e.Activator;

/**
 * Страница настроек плагина Specter для 1C:EDT.
 * Наследует FieldEditorPreferencePage и реализует IWorkbenchPreferencePage.
 * Настраивает путь к тестам Vanessa Automation с сохранением в IPreferenceStore плагина.
 */
public class SpecterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "ru.ozon.uitp.e2e.preferences.SpecterPreferencePage";

	/** Ключ настройки для каталога тестов Vanessa Automation в IPreferenceStore */
	public static final String P_VANESSA_TESTS_PATH = "vanessaTestsPath";

	public SpecterPreferencePage() {
		super(GRID);
		// Привязываем страницу к IPreferenceStore нашего плагина:
		if (Activator.getDefault() != null) {
			setPreferenceStore(Activator.getDefault().getPreferenceStore());
		}
		setDescription("Параметры интеграции Specter и запуска тестовых сценариев Vanessa Automation в 1C:EDT");
	}

	@Override
	public void init(IWorkbench workbench) {
		// Гарантируем привязку IPreferenceStore при открытии диалога Preferences в воркбенче
		if (getPreferenceStore() == null && Activator.getDefault() != null) {
			setPreferenceStore(Activator.getDefault().getPreferenceStore());
		}
	}

	@Override
	protected void createFieldEditors() {
		// Поле выбора каталога "Путь к тестам Vanessa Automation"
		DirectoryFieldEditor vanessaPathEditor = new DirectoryFieldEditor(
				P_VANESSA_TESTS_PATH,
				"Путь к тестам Vanessa Automation:",
				getFieldEditorParent()
		);
		addField(vanessaPathEditor);
	}
}
