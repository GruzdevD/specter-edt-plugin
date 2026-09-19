package ru.ozon.uitp.e2e.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.converter.VanessaConvertDialog;

/**
 * Страница настроек плагина Specter для 1C:EDT.
 * Наследует FieldEditorPreferencePage и реализует IWorkbenchPreferencePage.
 * Настраивает путь к тестам Vanessa Automation и предоставляет интерфейс запуска мастера конвертации.
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

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);

		// Создаем область "Конвертация найденных тестов в модули расширения"
		Composite fieldEditorParent = getFieldEditorParent();
		createConverterSection(fieldEditorParent);

		return contents;
	}

	private void createConverterSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Конвертация найденных тестов в модули расширения");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.verticalIndent = 15;
		group.setLayoutData(gd);
		group.setLayout(new GridLayout(1, false));

		Label infoLabel = new Label(group, SWT.WRAP);
		infoLabel.setText("Плагин автоматически сканирует каталог сценариев Vanessa (.feature) и выполняет интеллектуальную трансляцию шагов в исполняемые методы BSL расширения СП_Тестирование (общие модули).");
		GridData infoGd = new GridData(GridData.FILL_HORIZONTAL);
		infoGd.widthHint = 450;
		infoLabel.setLayoutData(infoGd);

		Button convertBtn = new Button(group, SWT.PUSH);
		convertBtn.setText("Распознать и конвертировать...");
		GridData btnGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		btnGd.verticalIndent = 8;
		convertBtn.setLayoutData(btnGd);

		convertBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Применяем текущие введенные настройки перед открытием диалога
				performApply();
				VanessaConvertDialog dialog = new VanessaConvertDialog(getShell());
				dialog.open();
			}
		});
	}
}

