package ru.ozon.uitp.e2e.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import ru.ozon.uitp.e2e.Activator;

/**
 * Диалоговое окно Eclipse для выбора и конвертации распознанных тестов Vanessa Automation
 * в новые программные модули расширения 1C:EDT.
 * Позволяет указывать и выбирать произвольный каталог тестов непосредственно в диалоге.
 */
public class VanessaConvertDialog extends TitleAreaDialog {

	private final VanessaConversionManager manager = new VanessaConversionManager();
	private List<VanessaScenario> scenarios = new ArrayList<>();
	private CheckboxTableViewer tableViewer;
	private Text pathText;
	private Text previewText;
	private Label statusLabel;

	public VanessaConvertDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Конвертация тестов Vanessa Automation в модули СП_Тестирование");
		setMessage("Укажите каталог со сценариями .feature и выберите тесты для преобразования в BSL модули (1 тест = 1 CommonModule)");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		container.setLayout(layout);

		// 1. Поле пути к тестам Ванессы (редактируемое + кнопка Обзор)
		Label pathLabel = new Label(container, SWT.NONE);
		pathLabel.setText("Каталог тестов Vanessa:");

		pathText = new Text(container, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pathText.setMessage("Укажите путь к папке с .feature файлами...");
		String configuredPath = Activator.getVanessaTestsPath();
		pathText.setText(configuredPath != null ? configuredPath : "");

		// Запуск сканирования по нажатию Enter в поле ввода пути
		pathText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				performScan();
			}
		});

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Обзор...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dirDialog = new DirectoryDialog(getShell(), SWT.OPEN);
				dirDialog.setText("Выберите каталог тестов Vanessa Automation (.feature)");
				String cur = pathText.getText().trim();
				if (!cur.isEmpty() && new File(cur).exists()) {
					dirDialog.setFilterPath(cur);
				}
				String selected = dirDialog.open();
				if (selected != null && !selected.trim().isEmpty()) {
					pathText.setText(selected.trim());
					performScan();
				}
			}
		});

		Button scanButton = new Button(container, SWT.PUSH);
		scanButton.setText("Сканировать");
		scanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performScan();
			}
		});

		// 2. Список найденных тестов (Таблица с чекбоксами)
		Label listLabel = new Label(container, SWT.NONE);
		listLabel.setText("Обнаруженные тесты Vanessa Automation:");
		GridData listLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		listLabel.setLayoutData(listLabelData);

		Table table = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		tableData.heightHint = 180;
		table.setLayoutData(tableData);

		TableColumn colCheck = new TableColumn(table, SWT.NONE);
		colCheck.setText("");
		colCheck.setWidth(30);

		TableColumn colFeature = new TableColumn(table, SWT.LEFT);
		colFeature.setText("Функционал (.feature)");
		colFeature.setWidth(180);

		TableColumn colScenario = new TableColumn(table, SWT.LEFT);
		colScenario.setText("Сценарий");
		colScenario.setWidth(220);

		TableColumn colModule = new TableColumn(table, SWT.LEFT);
		colModule.setText("Целевой CommonModule");
		colModule.setWidth(220);

		TableColumn colSteps = new TableColumn(table, SWT.CENTER);
		colSteps.setText("Шагов");
		colSteps.setWidth(60);

		tableViewer = new CheckboxTableViewer(table);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new ScenarioLabelProvider());

		// 3. Кнопки Выбрать все / Снять все
		Composite btnBar = new Composite(container, SWT.NONE);
		btnBar.setLayout(new GridLayout(3, false));
		btnBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

		Button selectAllBtn = new Button(btnBar, SWT.PUSH);
		selectAllBtn.setText("Выбрать все");
		selectAllBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(true);
				updateSelectedStatus();
			}
		});

		Button deselectAllBtn = new Button(btnBar, SWT.PUSH);
		deselectAllBtn.setText("Снять выбор");
		deselectAllBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(false);
				updateSelectedStatus();
			}
		});

		statusLabel = new Label(btnBar, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusLabel.setText("Готов к сканированию");

		// 4. Предпросмотр генерируемого BSL кода
		Label previewLabel = new Label(container, SWT.NONE);
		previewLabel.setText("Предпросмотр генерируемого BSL кода (СП_Тестирование):");
		previewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

		previewText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		GridData previewData = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		previewData.heightHint = 120;
		previewText.setLayoutData(previewData);
		previewText.setText("// Выберите тест из списка выше для предпросмотра структуры BSL-модуля");

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = table.getSelectionIndex();
				if (index >= 0 && index < scenarios.size()) {
					VanessaScenario selected = scenarios.get(index);
					previewText.setText(manager.getConverter().generateBslModuleCode(selected));
				}
				updateSelectedStatus();
			}
		});

		// Автоматически запускаем поиск тестов при открытии окна
		performScan();

		return area;
	}

	private void performScan() {
		String targetPath = pathText.getText().trim();
		if (targetPath.isEmpty()) {
			String configuredPath = Activator.getVanessaTestsPath();
			if (configuredPath != null && !configuredPath.trim().isEmpty()) {
				targetPath = configuredPath.trim();
				pathText.setText(targetPath);
			}
		}

		if (targetPath.isEmpty()) {
			scenarios = new ArrayList<>();
			tableViewer.setInput(scenarios);
			statusLabel.setText("Укажите каталог тестов для поиска .feature сценариев");
			return;
		}

		File dir = new File(targetPath);
		if (!dir.exists() || !dir.isDirectory()) {
			scenarios = new ArrayList<>();
			tableViewer.setInput(scenarios);
			statusLabel.setText("Каталог не найден: " + targetPath);
			return;
		}

		scenarios = manager.scanDirectory(dir);
		tableViewer.setInput(scenarios);
		tableViewer.setAllChecked(true);
		updateSelectedStatus();
	}

	private void updateSelectedStatus() {
		Object[] checkedElements = tableViewer.getCheckedElements();
		int total = scenarios.size();
		int checked = checkedElements.length;
		statusLabel.setText("Найдено: " + total + " | Выбрано для конвертации: " + checked);
	}

	@Override
	protected void okPressed() {
		Object[] checkedElements = tableViewer.getCheckedElements();
		if (checkedElements.length == 0) {
			MessageDialog.openWarning(getShell(), "Конвертация тестов", "Не выбрано ни одного теста для конвертации.");
			return;
		}

		List<VanessaScenario> toConvert = new ArrayList<>();
		for (Object obj : checkedElements) {
			if (obj instanceof VanessaScenario) {
				VanessaScenario sc = (VanessaScenario) obj;
				sc.setSelected(true);
				toConvert.add(sc);
			}
		}

		try {
			File projectRoot = new File("extension/СП_Тестирование");
			int count = manager.convertSelectedScenarios(toConvert, projectRoot);
			MessageDialog.openInformation(getShell(), "Успешная конвертация",
					"Сконвертировано " + count + " тестов Vanessa в общие модули расширения СП_Тестирование!\n"
					+ "Модули добавлены в дерево проекта и зарегистрированы в Configuration.mdo.");
			super.okPressed();
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Ошибка конвертации", "Не удалось завершить конвертацию: " + e.getMessage());
		}
	}

	private static class ScenarioLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof VanessaScenario) {
				VanessaScenario sc = (VanessaScenario) element;
				switch (columnIndex) {
					case 0: return "";
					case 1: return sc.getFeatureName();
					case 2: return sc.getScenarioName();
					case 3: return sc.getTargetModuleName();
					case 4: return String.valueOf(sc.getStepCount());
				}
			}
			return "";
		}
	}
}

