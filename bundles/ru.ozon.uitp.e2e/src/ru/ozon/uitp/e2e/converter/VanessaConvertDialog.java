package ru.ozon.uitp.e2e.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import ru.ozon.uitp.e2e.Activator;

/**
 * Диалоговое окно Eclipse для выбора и конвертации распознанных тестов Vanessa Automation
 * в новые программные модули расширения 1C:EDT.
 * Позволяет выбирать целевой проект расширения из текущего Workspace или задавать произвольный каталог.
 */
public class VanessaConvertDialog extends TitleAreaDialog {

	private final VanessaConversionManager manager = new VanessaConversionManager();
	private List<VanessaScenario> scenarios = new ArrayList<>();
	private final List<IProject> workspaceProjects = new ArrayList<>();

	private CheckboxTableViewer tableViewer;
	private Text pathText;
	private Combo projectCombo;
	private Label projectLocationLabel;
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
		setMessage("Укажите каталог тестов .feature, выберите целевой проект расширения EDT и сценарии для создания BSL-модулей");
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

		// 1. Поле пути к тестам Ванессы (редактируемое + кнопка Обзор + Сканировать)
		Label pathLabel = new Label(container, SWT.NONE);
		pathLabel.setText("Каталог тестов Vanessa:");

		pathText = new Text(container, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pathText.setMessage("Укажите путь к папке со сценариями .feature...");
		String configuredPath = Activator.getVanessaTestsPath();
		pathText.setText(configuredPath != null ? configuredPath : "");

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

		// 2. Секция выбора целевого проекта расширения EDT
		createTargetProjectSection(container);

		// 3. Список найденных тестов (Таблица с чекбоксами)
		Label listLabel = new Label(container, SWT.NONE);
		listLabel.setText("Обнаруженные тесты Vanessa Automation:");
		GridData listLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		listLabel.setLayoutData(listLabelData);

		Table table = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		tableData.heightHint = 160;
		table.setLayoutData(tableData);

		TableColumn colCheck = new TableColumn(table, SWT.NONE);
		colCheck.setText("");
		colCheck.setWidth(30);

		TableColumn colFeature = new TableColumn(table, SWT.LEFT);
		colFeature.setText("Функционал (.feature)");
		colFeature.setWidth(170);

		TableColumn colScenario = new TableColumn(table, SWT.LEFT);
		colScenario.setText("Сценарий");
		colScenario.setWidth(210);

		TableColumn colModule = new TableColumn(table, SWT.LEFT);
		colModule.setText("Целевой CommonModule");
		colModule.setWidth(220);

		TableColumn colSteps = new TableColumn(table, SWT.CENTER);
		colSteps.setText("Шагов");
		colSteps.setWidth(60);

		tableViewer = new CheckboxTableViewer(table);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new ScenarioLabelProvider());

		// 4. Кнопки Выбрать все / Снять все
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

		// 5. Предпросмотр генерируемого BSL кода
		Label previewLabel = new Label(container, SWT.NONE);
		previewLabel.setText("Предпросмотр генерируемого BSL кода (СП_Тестирование):");
		previewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

		previewText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		GridData previewData = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		previewData.heightHint = 110;
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

	private void createTargetProjectSection(Composite parent) {
		Group projectGroup = new Group(parent, SWT.NONE);
		projectGroup.setText("Целевой проект расширения 1C:EDT");
		GridData groupData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		projectGroup.setLayoutData(groupData);
		GridLayout groupLayout = new GridLayout(2, false);
		groupLayout.marginWidth = 10;
		groupLayout.marginHeight = 8;
		projectGroup.setLayout(groupLayout);

		Label comboLabel = new Label(projectGroup, SWT.NONE);
		comboLabel.setText("Проект расширения:");

		projectCombo = new Combo(projectGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
		projectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label locHintLabel = new Label(projectGroup, SWT.NONE);
		locHintLabel.setText("Расположение:");

		projectLocationLabel = new Label(projectGroup, SWT.NONE);
		projectLocationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectLocationLabel.setText("Определяется автоматически...");

		// Заполняем список открытых проектов Workspace
		populateWorkspaceProjects();

		projectCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateProjectLocationHint();
			}
		});
	}

	private void updateProjectLocationHint() {
		int idx = projectCombo.getSelectionIndex();
		if (idx >= 0 && idx < workspaceProjects.size()) {
			IProject proj = workspaceProjects.get(idx);
			if (proj.getLocation() != null) {
				projectLocationLabel.setText(proj.getLocation().toOSString() + " (src/CommonModules)");
			} else {
				projectLocationLabel.setText(proj.getFullPath().toOSString());
			}
		} else {
			projectLocationLabel.setText("—");
		}
	}

	private void populateWorkspaceProjects() {
		workspaceProjects.clear();
		projectCombo.removeAll();

		int preferredIndex = -1;
		try {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			if (projects != null) {
				for (IProject p : projects) {
					if (p.isOpen()) {
						workspaceProjects.add(p);
						String name = p.getName();
						projectCombo.add(name);

						// Ищем проект с расширением СП_Тестирование или аналогичным
						String lower = name.toLowerCase();
						if (lower.contains("сп_") || lower.contains("тестирован") || lower.contains("extension") || lower.contains("расширен")) {
							if (preferredIndex == -1) {
								preferredIndex = workspaceProjects.size() - 1;
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			Activator.logError("Не удалось получить проекты Workspace Eclipse", t);
		}

		if (!workspaceProjects.isEmpty()) {
			if (preferredIndex == -1) {
				preferredIndex = 0;
			}
			projectCombo.select(preferredIndex);
			updateProjectLocationHint();
		} else {
			projectCombo.add("[В Workspace нет открытых проектов расширений 1С]");
			projectCombo.select(0);
			projectLocationLabel.setText("Откройте проект расширения 1С в EDT");
		}
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

		int selectedIdx = projectCombo != null ? projectCombo.getSelectionIndex() : -1;
		if (selectedIdx < 0 || selectedIdx >= workspaceProjects.size()) {
			MessageDialog.openError(getShell(), "Ошибка выбора проекта", 
					"Не выбран целевой проект расширения из списка Workspace Eclipse.\n"
					+ "Пожалуйста, откройте проект расширения 1C:EDT в рабочей области.");
			return;
		}

		IProject selectedProject = workspaceProjects.get(selectedIdx);
		if (selectedProject.getLocation() == null) {
			MessageDialog.openError(getShell(), "Ошибка выбора проекта", 
					"Не удалось определить физический каталог проекта: " + selectedProject.getName());
			return;
		}

		File projectRoot = selectedProject.getLocation().toFile();
		if (!projectRoot.exists()) {
			MessageDialog.openError(getShell(), "Ошибка каталога проекта", 
					"Каталог проекта не существует на диске:\n" + projectRoot.getAbsolutePath());
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
			int count = manager.convertSelectedScenarios(toConvert, projectRoot);

			// Принудительно обновляем ресурсы Workspace в Eclipse/1C:EDT для отображения новых модулей в дереве
			try {
				if (selectedProject.isOpen()) {
					selectedProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				}
				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			} catch (Throwable t) {
				Activator.logError("Предупреждение при обновлении Workspace", t);
			}

			MessageDialog.openInformation(getShell(), "Успешная конвертация",
					"Сконвертировано " + count + " тестов Vanessa в общие модули расширения СП_Тестирование!\n\n"
					+ "Целевой проект: " + selectedProject.getName() + " (" + projectRoot.getAbsolutePath() + ")\n"
					+ "Папка модулей: " + new File(projectRoot, "src/CommonModules").getAbsolutePath() + "\n\n"
					+ "Дерево проекта 1C:EDT автоматически обновлено.");
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


