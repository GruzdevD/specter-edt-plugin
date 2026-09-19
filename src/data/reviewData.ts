import { CodeIssue, ProjectFileInfo, ReviewMetric } from '../types';

export const REVIEW_METRICS: ReviewMetric[] = [
  {
    id: 'osgi',
    label: 'OSGi & MANIFEST.MF',
    value: '3 замечания',
    status: 'warning',
    description: 'Отсутствие завершающего переноса строки (RFC 1960), неэкспортированные пакеты, нехватка базовых бандлов.'
  },
  {
    id: 'ui_thread',
    label: 'UI Thread (SWT/Job API)',
    value: 'Критично',
    status: 'failed',
    description: 'Синхронный дисковый I/O в createPartControl и тулбаре, блокирующий опрос launch-менеджера, зависание UI-потока.'
  },
  {
    id: 'swt_jface',
    label: 'SWT/JFace Compliance',
    value: 'Чистый SWT (100%)',
    status: 'passed',
    description: 'Swing/AWT отсутствует полностью. Использованы нативные SWT Composite, StyledText, JFace TreeViewer.'
  },
  {
    id: 'runid_sync',
    label: 'Синхронизация runId',
    value: 'Фатальный баг',
    status: 'failed',
    description: 'В BridgeRunner генерировался новый runId, отличный от переданного в JSON. Тесты гарантированно зависали на 10 минут!'
  },
  {
    id: 'resources',
    label: 'Ресурсы и память',
    value: '2 дефекта',
    status: 'warning',
    description: 'Статические Color ссылки без учета жизненного цикла Display, O(N²) перевыделение StyleRange[], утечка ILaunchConfiguration.'
  }
];

export const CODE_ISSUES: CodeIssue[] = [
  {
    id: 'FATAL_RUNID_DESYNC',
    title: 'Фатальная рассинхронизация runId между генерацией команд и ожиданием отчёта',
    severity: 'CRITICAL',
    category: 'LOGIC_AND_SYNC',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/BridgeRunner.java',
    lineNumbers: '45-65',
    summary: 'В TestsView генерируется runId_A и зашивается в JSON. BridgeRunner.runAsync игнорирует его, генерирует runId_B и ждёт bridge-result-runId_B.json!',
    violation: 'Нарушение контракта взаимодействия с BSL-мостом 1С. Клиент 1С исполняет команды с runId_A и пишет файл отчёта bridge-result-runId_A.json, а плагин EDT бесконечно ждет файл bridge-result-runId_B.json до истечения таймаута в 10 минут.',
    impact: 'Любой запуск тестов («Запустить мост» или «Запустить набор») гарантированно зависает ровно на 10 минут (BridgeScenario.RESULT_TIMEOUT_MS = 600000ms), после чего падает по таймауту с ошибкой. Тесты никогда не завершаются успешно!',
    solution: 'Принимать runId в сигнатуре метода BridgeRunner.runAsync(String runId, String commandsJson, ...) либо извлекать его или генерировать в единой точке входа.',
    originalSnippet: `// В TestsView.java:
BridgeRunner.runAsync(BridgeScenario.commandsJson(LaunchMonitor.newRunId()), ...);

// В BridgeRunner.java:
public static void runAsync(final String commandsJson, ...) {
    Job job = new Job(...) {
        protected IStatus run(IProgressMonitor monitor) {
            File outDir = LaunchMonitor.outDir();
            String runId = LaunchMonitor.newRunId(); // ❌ ГЕНЕРИРУЕТ НОВЫЙ runId!
            LaunchMonitor.writeCommands(outDir, commandsJson); // а в commandsJson зашит СТАРЫЙ runId!
            ...
            File result = LaunchMonitor.waitForBridgeResult(outDir, runId, launch, ...); // ❌ Ищет несуществующий файл!
        }
    };
}`,
    correctedSnippet: `// В BridgeRunner.java:
public static void runAsync(final String runId, final String commandsJson,
        final Callback onResult, final ErrorCallback onError) {
    Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                File outDir = LaunchMonitor.outDir();
                // ✅ Записываем commandsJson и ждем РОВНО ТОТ ЖЕ runId:
                LaunchMonitor.writeCommands(outDir, commandsJson);
                LaunchMonitor.info("Specter view: командный файл моста записан runId=" + runId);
                ...
                File result = LaunchMonitor.waitForBridgeResult(
                        outDir, runId, launch, BridgeScenario.RESULT_TIMEOUT_MS, monitor);
                ...
            }
        }
    };
    job.schedule();
}`
  },
  {
    id: 'UI_BLOCKING_FILE_IO',
    title: 'Блокирующий синхронный файловый ввод/вывод на UI-потоке Eclipse/SWT',
    severity: 'CRITICAL',
    category: 'UI_THREAD_BLOCKING',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/TestsView.java',
    lineNumbers: '36, 68-72',
    summary: 'Вызов reloadLatestFromOutDir() прямо в createPartControl() и в обработчике тулбарной кнопки «Обновить».',
    violation: 'Строжайший архитектурный запрет EDT_PLUGIN_DEV: "Отсутствие любых блокировок UI-потока (все I/O операции должны быть в Job API, а обновление UI — через Display.asyncExec)".',
    impact: 'При открытии панели «Тесты» или нажатии «Обновить» метод listFiles() и чтение файлов с диска (Files.readAllBytes) выполняются в главном UI-потоке SWT. Если каталог обмена расположен на медленном/сетевом диске, весь интерфейс 1C:EDT зависает (Not Responding / пляшущее колесо).',
    solution: 'Перенести вычитку последнего отчёта в асинхронный Eclipse Job, а результат публиковать через Display.asyncExec().',
    originalSnippet: `// TestsView.java:
@Override
public void createPartControl(Composite parent) {
    ...
    BridgeResult br = BridgeResultStore.get().current();
    if (br == null) {
        br = BridgeResultStore.get().reloadLatestFromOutDir(); // ❌ Синхронный I/O в UI-потоке!
    }
    lastResult = br;
}

tb.add(new Action("Обновить") {
    @Override
    public void run() {
        BridgeResultStore.get().reloadLatestFromOutDir(); // ❌ Синхронный I/O по клику мыши!
    }
});`,
    correctedSnippet: `// TestsView.java:
@Override
public void createPartControl(Composite parent) {
    ...
    BridgeResult br = BridgeResultStore.get().current();
    if (br == null) {
        // ✅ Асинхронная загрузка в Job API без фриза интерфейса
        BridgeResultStore.get().reloadLatestFromOutDirAsync(loaded -> {
            if (!viewer.getControl().isDisposed()) {
                lastResult = loaded;
                rebuild();
            }
        });
    } else {
        lastResult = br;
        rebuild();
    }
}

tb.add(new Action("Обновить") {
    @Override
    public void run() {
        setHint("Поиск последнего отчёта моста...");
        BridgeResultStore.get().reloadLatestFromOutDirAsync(null);
    }
});`
  },
  {
    id: 'UNRESPONSIVE_JOB_CANCEL',
    title: 'Отсутствие поддержки отмены Job (IProgressMonitor.isCanceled)',
    severity: 'MAJOR',
    category: 'UI_THREAD_BLOCKING',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/launcher/LaunchMonitor.java',
    lineNumbers: '85-115',
    summary: 'Цикл waitForBridgeResult не опрашивает monitor.isCanceled(), из-за чего остановка Job в Eclipse Progress View игнорируется.',
    violation: 'Требование стандартов Eclipse RCP Job API: долгие циклы ожидания обязаны периодически проверять состояние отмены и немедленно освобождать поток.',
    impact: 'Пользователь жмет красную кнопку «Отмена» в Progress View EDT, но процесс продолжает работать и спать по 1.5 секунды до истечения 10 минут.',
    solution: 'Передавать IProgressMonitor в LaunchMonitor.waitForBridgeResult и прерывать ожидание при monitor.isCanceled().',
    originalSnippet: `public static File waitForBridgeResult(File outDir, String runId, ILaunch launch, long timeoutMs) {
    File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
    long startedAt = System.currentTimeMillis();
    while (System.currentTimeMillis() - startedAt < timeoutMs) {
        if (expected.isFile()) return expected;
        if (isTerminated(launch)) return null;
        try {
            Thread.sleep(POLL_MS); // ❌ Не проверяет monitor.isCanceled()!
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    return null;
}`,
    correctedSnippet: `public static File waitForBridgeResult(File outDir, String runId, ILaunch launch,
        long timeoutMs, IProgressMonitor monitor) {
    File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
    long startedAt = System.currentTimeMillis();
    while (System.currentTimeMillis() - startedAt < timeoutMs) {
        if (monitor != null && monitor.isCanceled()) {
            info("Ожидание моста отменено пользователем (runId=" + runId + ")");
            return null; // ✅ Немедленная отзывчивость на отмену
        }
        if (expected.isFile()) return expected;
        if (isTerminated(launch)) return null;
        try {
            Thread.sleep(POLL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    return null;
}`
  },
  {
    id: 'OSGI_MANIFEST_ERRORS',
    title: 'Ошибки структуры OSGi MANIFEST.MF (RFC 1960, недостающие зависимости и экспорт)',
    severity: 'MAJOR',
    category: 'OSGI_DEPENDENCIES',
    filePath: 'bundles/ru.ozon.uitp.e2e/META-INF/MANIFEST.MF',
    lineNumbers: '1-14',
    summary: 'Отсутствие финального переноса строки, пакеты views не экспортированы, отсутствуют org.eclipse.core.resources и org.eclipse.ui.workbench.',
    violation: 'Спецификация OSGi Core / JAR Manifest: файл манифеста ОБЯЗАН заканчиваться символом новой строки, иначе последний заголовок отбрасывается. Пакеты, предоставляющие публичные API/расширения, должны экспортироваться.',
    impact: 'В зависимости от версии OSGi Equinox последняя директива Export-Package может быть проигнорирована. Пакет views недоступен другим бандлам/тестам.',
    solution: 'Добавить trailing newline, экспортировать пакеты ru.ozon.uitp.e2e.views и ru.ozon.uitp.e2e, подключить необходимые бандлы org.eclipse.core.resources, org.eclipse.ui.workbench, org.eclipse.core.jobs.',
    originalSnippet: `Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Specter - EDT plugin (UI-тестирование 1С)
Bundle-SymbolicName: ru.ozon.uitp.e2e;singleton:=true
Bundle-Version: 0.1.0.qualifier
Bundle-Vendor: Ozon
Bundle-Activator: ru.ozon.uitp.e2e.Activator
Bundle-ActivationPolicy: lazy
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.core.runtime, org.eclipse.debug.core, org.eclipse.debug.ui, org.eclipse.ui, org.eclipse.ui.views, org.eclipse.jface, org.eclipse.swt, org.eclipse.ui.ide, com._1c.g5.v8.dt.launching.core, com._1c.g5.v8.dt.metadata, com._1c.g5.v8.dt.bsl.model
Export-Package: ru.ozon.uitp.e2e.launcher<NO_NEWLINE_AT_EOF>`,
    correctedSnippet: `Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Specter - EDT plugin (UI-тестирование 1С)
Bundle-SymbolicName: ru.ozon.uitp.e2e;singleton:=true
Bundle-Version: 0.2.0.qualifier
Bundle-Vendor: Ozon
Bundle-Activator: ru.ozon.uitp.e2e.Activator
Bundle-ActivationPolicy: lazy
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.core.resources,
 org.eclipse.core.jobs,
 org.eclipse.debug.core,
 org.eclipse.debug.ui,
 org.eclipse.ui,
 org.eclipse.ui.workbench,
 org.eclipse.ui.views,
 org.eclipse.jface,
 org.eclipse.swt,
 org.eclipse.ui.ide,
 com._1c.g5.v8.dt.launching.core,
 com._1c.g5.v8.dt.metadata,
 com._1c.g5.v8.dt.bsl.model
Export-Package: ru.ozon.uitp.e2e,
 ru.ozon.uitp.e2e.launcher,
 ru.ozon.uitp.e2e.views
`
  },
  {
    id: 'SWT_STYLED_TEXT_SYNTAX_BUG',
    title: 'Ошибочный расчет смещений в StyleRange и O(N²) перевыделение массивов в ResultsView',
    severity: 'MAJOR',
    category: 'SWT_JFACE_COMPLIANCE',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/ResultsView.java',
    lineNumbers: '75-105',
    summary: 'Формула sr.start = lineStart + colondIdx - 3 подсвечивает случайные символы до тире, а метод append копирует массив на каждый шаг.',
    violation: 'Нарушение корректного позиционирования SWT StyledText StyleRange. При несовпадении длины строки возможно падение с IllegalArgumentException/IndexOutOfBounds.',
    impact: 'В отчёте результатов подсвечивается не статус PASSED/FAILED, а название действия ("openList"). При 100+ шагах O(N²) копирование массива ухудшает производительность.',
    solution: 'Искать статус по точному индексу в строке (indexOf("FAILED") / indexOf("PASSED")), рассчитывать start = lineStart + statusIdx, использовать List<StyleRange>.',
    originalSnippet: `int colondIdx = line.indexOf("—");
if (colondIdx < 0) continue;
boolean failed = line.contains("FAILED");
if (!failed && !line.contains("PASSED")) continue;

int lineStart = offsetOf(sb, li);
StyleRange sr = new StyleRange();
sr.start = lineStart + colondIdx - 3; // ❌ Подсвечивает текст ДО тире!
sr.length = 6; // ❌ Фиксированная длина 6, хотя PASSED=6, FAILED=6, но сдвиг сломан!
sr.foreground = failed ? red : green;
ranges = append(ranges, sr); // ❌ O(N²) постоянное копирование массива!`,
    correctedSnippet: `List<StyleRange> rangesList = new ArrayList<>();
int lineStart = 0;
for (String line : lines) {
    int statusIdx = line.indexOf("FAILED");
    boolean failed = true;
    int len = 6;
    if (statusIdx < 0) {
        statusIdx = line.indexOf("PASSED");
        failed = false;
        len = 6;
    }
    if (statusIdx >= 0) {
        StyleRange sr = new StyleRange();
        sr.start = lineStart + statusIdx; // ✅ Точный оффсет слова PASSED или FAILED
        sr.length = len;
        sr.foreground = failed ? red : green;
        sr.fontStyle = SWT.BOLD;
        rangesList.add(sr);
    }
    lineStart += line.length() + 1; // переход на следующую строку (\n)
}
text.setStyleRanges(rangesList.toArray(new StyleRange[0]));`
  },
  {
    id: 'SWT_COLOR_STATIC_DISPOSAL',
    title: 'Хранение статических SWT Color объектов в загрузчике классов',
    severity: 'MODERATE',
    category: 'RESOURCE_LIFECYCLE',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/TestsView.java',
    lineNumbers: '185-188',
    summary: 'Инициализация Display.getDefault().getSystemColor(...) в статических полях private static final Color GREEN/RED/BLUE.',
    violation: 'Правила управления ресурсами SWT/JFace: статические поля инициализируются в момент загрузки класса. Если класс загружен до Display или Display пересоздан, возникает SWTException.',
    impact: 'Потенциальный краш SWT при смене темы, перезапуске воркбенча или тестировании без активного Display.',
    solution: 'Получать цвета динамически через Display.getCurrent() / Display.getDefault() в методах провайдера или использовать JFace ColorRegistry.',
    originalSnippet: `private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
    // ❌ Опасно: инициализируется при загрузке класса!
    private static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
    private static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private static final Color BLUE = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
}`,
    correctedSnippet: `private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
    @Override
    public Color getForeground(Object element) {
        Display d = Display.getCurrent();
        if (d == null) d = Display.getDefault();
        if (d == null || d.isDisposed()) return null;

        if (element instanceof RootSetNode) {
            return d.getSystemColor(SWT.COLOR_BLUE); // ✅ Безопасный запрос системного цвета
        }
        if (element instanceof RootResultNode) {
            return ((RootResultNode) element).result.isFailed()
                    ? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
        }
        if (element instanceof StepNode) {
            return ((StepNode) element).step.isFailed()
                    ? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
        }
        return null;
    }
}`
  },
  {
    id: 'LAUNCH_CONFIG_ACCUMULATION',
    title: 'Утечка и засорение диска копиями ILaunchConfiguration',
    severity: 'MODERATE',
    category: 'RESOURCE_LIFECYCLE',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/launcher/BridgeLaunchHelper.java',
    lineNumbers: '85-95',
    summary: 'Метод wc.doSave() сохраняет каждый запуск на диск в .metadata workspace без очистки или проверки существующего.',
    violation: 'Лучшие практики Eclipse Launching Framework: рабочие конфигурации запуска должны повторно использоваться или помечаться как временные.',
    impact: 'После 50 прогонов тестов каталог workspace/.metadata/.plugins/org.eclipse.debug.core/.launches забивается мусорными копиями конфигураций.',
    solution: 'Переиспользовать существующую копию конфигурации с префиксом "UITP - ", либо удалять её после завершения процесса.',
    originalSnippet: `ILaunchConfigurationWorkingCopy wc = source.copy(PREFIX + source.getName());
wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION, ...);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);
ILaunchConfiguration copy = wc.doSave(); // ❌ Бесконечно сохраняет новые файлы`,
    correctedSnippet: `String targetName = PREFIX + source.getName();
ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
ILaunchConfiguration existing = null;
for (ILaunchConfiguration c : lm.getLaunchConfigurations()) {
    if (targetName.equals(c.getName())) {
        existing = c;
        break;
    }
}
ILaunchConfigurationWorkingCopy wc = (existing != null)
        ? existing.getWorkingCopy()
        : source.copy(targetName);
wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION, ...);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);
ILaunchConfiguration copy = wc.doSave(); // ✅ Повторно использует одну и ту же конфигурацию`
  },
  {
    id: 'HARDCODED_CONFIG_NAME',
    title: 'Жестко зашитое русскоязычное имя исходной launch-конфигурации',
    severity: 'OPTIMIZATION',
    category: 'LOGIC_AND_SYNC',
    filePath: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/launcher/BridgeLaunchHelper.java',
    lineNumbers: '26, 120-135',
    summary: 'SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ". Если конфигурация названа иначе, плагин завершается ошибкой.',
    violation: 'Принцип переносимости инструментов разработчика.',
    impact: 'Любой разработчик или проект, у которого конфигурация названа "Тонкий клиент", "1C:Клиент" или на английском, получает отказ без возможности настройки.',
    solution: 'Поддержать системное свойство -Duitp.e2e.launchConfig=<name>, а если не задано — искать "Тонкий клиент АУФ", либо брать первую доступную конфигурацию типа RuntimeClient с выводом понятного сообщения.',
    originalSnippet: `public static final String SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ";
...
for (ILaunchConfiguration cfg : lm.getLaunchConfigurations(rtc)) {
    if (SOURCE_CONFIGURATION_NAME.equals(cfg.getName())) {
        return cfg;
    }
}
throw new CoreException(error("Не найдена launch-конфигурация «" + SOURCE_CONFIGURATION_NAME + "»"));`,
    correctedSnippet: `String preferred = System.getProperty("uitp.e2e.launchConfig");
if (preferred != null && !preferred.isBlank()) {
    for (ILaunchConfiguration cfg : lm.getLaunchConfigurations(rtc)) {
        if (preferred.trim().equals(cfg.getName())) return cfg;
    }
}
// Ищем стандартную или первую подходящую:
ILaunchConfiguration first = null;
for (ILaunchConfiguration cfg : lm.getLaunchConfigurations(rtc)) {
    if (SOURCE_CONFIGURATION_NAME.equals(cfg.getName())) return cfg;
    if (first == null) first = cfg;
}
if (first != null) return first;
throw new CoreException(error("В проекте нет ни одной конфигурации типа 1C RuntimeClient"));`
  }
];

export const PROJECT_FILES: ProjectFileInfo[] = [
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/BridgeRunner.java',
    title: 'BridgeRunner.java',
    description: 'Оркестратор фонового прогона моста в Eclipse Job API',
    status: 'critical_fix',
    issuesCount: 2,
    diffSummary: 'Устранена критическая рассинхронизация runId, добавлена передача IProgressMonitor для поддержки кнопки Cancel в Eclipse, безопасное логирование.',
    originalCode: `package ru.ozon.uitp.e2e.views;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.BridgeLaunchHelper;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

public final class BridgeRunner {
	public interface Callback {
		void onResult(BridgeResult result);
	}
	public interface ErrorCallback {
		void onError(String message);
	}
	private BridgeRunner() {
	}

	public static void runAsync(final String commandsJson,
			final Callback onResult, final ErrorCallback onError) {
		Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					File outDir = LaunchMonitor.outDir();
					String runId = LaunchMonitor.newRunId();
					LaunchMonitor.writeCommands(outDir, commandsJson);
					LaunchMonitor.info("Specter view: командный файл моста записан runId=" + runId);

					ILaunch launch = BridgeLaunchHelper.launchClient("run");
					if (launch == null) {
						return reportError(onError, "Клиент передан на запуск, но ILaunch не найден "
								+ "(запуск мог не стартовать)");
					}
					LaunchMonitor.info("Specter view: тонкий клиент запущен: "
							+ launch.getLaunchConfiguration().getName());

					File result = LaunchMonitor.waitForBridgeResult(
							outDir, runId, launch, BridgeScenario.RESULT_TIMEOUT_MS);
					if (result == null) {
						int exit = LaunchMonitor.exitCodeOf(launch);
						String reason = exit != Integer.MIN_VALUE
								? "контролируемый процесс завершился с кодом " + exit
										+ " без файла результата"
								: "таймаут ожидания результата моста";
						LaunchMonitor.info("Specter view: результат не получен: " + reason);
						return reportError(onError, "Результат моста не получен: " + reason);
					}

					String body = LaunchMonitor.readFileSafe(result);
					BridgeResult br = BridgeResult.parse(body, result);
					if (br == null) {
						return reportError(onError, "Результат моста прочитан, но не разобран ("
								+ result.getAbsolutePath() + ")");
					}
					LaunchMonitor.info("Specter view: мост завершился status=" + br.status
							+ " steps=" + br.steps.size());

					org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
						BridgeResultStore.get().set(br);
						if (onResult != null) {
							onResult.onResult(br);
						}
					});
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return reportError(onError, "Не удалось запустить клиент АУФ: " + e.getMessage());
				}
			}
		};
		job.schedule();
	}

	private static IStatus reportError(final ErrorCallback onError, final String message) {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
			if (onError != null) {
				onError.onError(message);
			}
		});
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}
}`,
    fixedCode: `package ru.ozon.uitp.e2e.views;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.Activator;
import ru.ozon.uitp.e2e.launcher.BridgeLaunchHelper;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Исправленный оркестратор запуска живого моста в фоновом Eclipse Job.
 * 
 * Ключевые исправления:
 * 1. Синхронизирован runId: принимается точно тот runId, который сформирован для команд!
 * 2. Передан IProgressMonitor: отмена в Progress View немедленно прерывает ожидание.
 * 3. Безопасное обновление UI через Display.asyncExec с проверкой на завершение воркбенча.
 */
public final class BridgeRunner {

	public interface Callback {
		void onResult(BridgeResult result);
	}

	public interface ErrorCallback {
		void onError(String message);
	}

	private BridgeRunner() {
	}

	/**
	 * Запускает сценарий моста асинхронно в Eclipse Job.
	 *
	 * @param runId        единый уникальный идентификатор прогона
	 * @param commandsJson сформированное тело команд с этим же runId
	 * @param onResult     колбэк успеха (вызывается на UI-потоке)
	 * @param onError      колбэк ошибки (вызывается на UI-потоке)
	 */
	public static void runAsync(final String runId, final String commandsJson,
			final Callback onResult, final ErrorCallback onError) {
		Job job = new Job("Specter: запуск тонкого клиента АУФ (живой мост)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Выполнение сценария 1С через UI-мост...", 100);
					File outDir = LaunchMonitor.outDir();

					// Записываем команды со строго согласованным runId
					LaunchMonitor.writeCommands(outDir, commandsJson);
					LaunchMonitor.info("Specter view: командный файл моста записан runId=" + runId);
					monitor.worked(15);

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					// Запуск клиента через хелпер
					ILaunch launch = BridgeLaunchHelper.launchClient("run", monitor);
					if (launch == null) {
						return reportError(onError, "Клиент передан на запуск, но ILaunch не найден "
								+ "(запуск мог не стартовать)");
					}
					LaunchMonitor.info("Specter view: тонкий клиент запущен: "
							+ launch.getLaunchConfiguration().getName());
					monitor.worked(20);

					// Ожидаем результат именно для согласованного runId с поддержкой отмены
					File result = LaunchMonitor.waitForBridgeResult(
							outDir, runId, launch, BridgeScenario.RESULT_TIMEOUT_MS, monitor);

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					if (result == null) {
						int exit = LaunchMonitor.exitCodeOf(launch);
						String reason = exit != Integer.MIN_VALUE
								? "контролируемый процесс завершился с кодом " + exit
										+ " без файла результата"
								: "таймаут ожидания результата моста (" + (BridgeScenario.RESULT_TIMEOUT_MS / 1000) + " сек)";
						LaunchMonitor.info("Specter view: результат не получен: " + reason);
						return reportError(onError, "Результат моста не получен: " + reason);
					}
					monitor.worked(50);

					String body = LaunchMonitor.readFileSafe(result);
					BridgeResult br = BridgeResult.parse(body, result);
					if (br == null) {
						return reportError(onError, "Результат моста прочитан, но не разобран ("
								+ result.getAbsolutePath() + ")");
					}
					LaunchMonitor.info("Specter view: мост завершился status=" + br.status
							+ " steps=" + br.steps.size());

					// Публикуем результат на UI-потоке
					dispatchToUI(() -> {
						BridgeResultStore.get().set(br);
						if (onResult != null) {
							onResult.onResult(br);
						}
					});
					monitor.worked(15);
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return reportError(onError, "Не удалось запустить клиент АУФ: " + e.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private static IStatus reportError(final ErrorCallback onError, final String message) {
		dispatchToUI(() -> {
			if (onError != null) {
				onError.onError(message);
			}
		});
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}

	private static void dispatchToUI(Runnable runnable) {
		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(runnable);
		}
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/TestsView.java',
    title: 'TestsView.java',
    description: 'Панель дерева тестов и сценариев SWT / JFace',
    status: 'critical_fix',
    issuesCount: 3,
    diffSummary: 'Убран синхронный I/O из createPartControl и тулбара «Обновить», синхронизирована передача runId, исправлено статическое хранение SWT Color.',
    originalCode: `// Ключевые фрагменты TestsView.java с дефектами:
@Override
public void createPartControl(Composite parent) {
    ...
    BridgeResult br = BridgeResultStore.get().current();
    if (br == null) {
        br = BridgeResultStore.get().reloadLatestFromOutDir(); // ❌ Синхронный I/O в UI!
    }
    lastResult = br;
    refreshActiveSet();
    rebuild();
}

private void createToolbarActions() {
    ...
    tb.add(new Action("Обновить") {
        @Override
        public void run() {
            BridgeResultStore.get().reloadLatestFromOutDir(); // ❌ Синхронный I/O по кнопке!
        }
    });
}

private void runScenario() {
    setHint("Запускаю тонкий клиент АУФ (сценарий-канон R1)…");
    // ❌ Генерирует runId, который теряется и не передается в runAsync:
    BridgeRunner.runAsync(BridgeScenario.commandsJson(LaunchMonitor.newRunId()),
            r -> setHint("Прогон завершён: " + r.status + ...),
            err -> setHint("Ошибка: " + err));
}

private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
    // ❌ Статические цвета привязываются к первому Display и текут:
    private static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
    private static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private static final Color BLUE = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
}`,
    fixedCode: `package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Панель «Тесты» (Specter): дерево запуска и активный набор BSL.
 * Все I/O операции строго асинхронны, ресурсы SWT безопасно утилизируются.
 */
public class TestsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.TestsView";

	private TreeViewer viewer;
	private Action runSetAction;
	private RootSetNode setNode;
	private BridgeResult lastResult;
	private String hintText;

	private final BridgeResultStore.Listener storeListener = this::refreshFromResult;
	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			if (partRef != null && "com._1c.g5.v8.dt.bsl.ui.BslEditor".equals(partRef.getId())) {
				refreshActiveSet();
			}
		}
		@Override public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override public void partClosed(IWorkbenchPartReference partRef) { refreshActiveSet(); }
		@Override public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override public void partOpened(IWorkbenchPartReference partRef) {}
		@Override public void partHidden(IWorkbenchPartReference partRef) {}
		@Override public void partVisible(IWorkbenchPartReference partRef) {}
		@Override public void partInputChanged(IWorkbenchPartReference partRef) { refreshActiveSet(); }
	};

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new NodeContentProvider());
		viewer.setLabelProvider(new NodeLabelProvider());

		createToolbarActions();
		BridgeResultStore.get().addListener(storeListener);

		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().addPartListener(partListener);
			}
		} catch (RuntimeException e) {
			// игнорируем во вспомогательных контекстах
		}

		// Безопасное получение текущего результата без синхронного дискового I/O:
		BridgeResult br = BridgeResultStore.get().current();
		if (br == null) {
			setHint("Поиск последнего прогона...");
			BridgeResultStore.get().reloadLatestFromOutDirAsync(loaded -> {
				if (viewer != null && !viewer.getControl().isDisposed()) {
					lastResult = loaded;
					hintText = null;
					refreshActiveSet();
					rebuild();
				}
			});
		} else {
			lastResult = br;
			refreshActiveSet();
			rebuild();
		}
	}

	private void createToolbarActions() {
		IToolBarManager tb = getViewSite().getActionBars().getToolBarManager();
		tb.add(new Action("Запустить мост") {
			@Override
			public void run() {
				runScenario();
			}
		});
		runSetAction = new Action("Запустить набор") {
			@Override
			public void run() {
				runActiveSet();
			}
		};
		runSetAction.setEnabled(false);
		tb.add(runSetAction);
		tb.add(new Action("Обновить") {
			@Override
			public void run() {
				setHint("Обновление данных из каталога обмена...");
				BridgeResultStore.get().reloadLatestFromOutDirAsync(r -> {
					if (viewer != null && !viewer.getControl().isDisposed()) {
						setHint(r != null ? "Отчёт обновлен" : "Отчёты не найдены");
						rebuild();
					}
				});
			}
		});
		tb.add(new Action("Развернуть") {
			@Override
			public void run() {
				viewer.expandAll();
			}
		});
		tb.add(new Action("Свернуть") {
			@Override
			public void run() {
				viewer.collapseAll();
			}
		});
	}

	/** Живой прогон сценария-канона R1 с согласованным runId */
	private void runScenario() {
		setHint("Запускаю тонкий клиент АУФ (сценарий-канон R1)…");
		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.commandsJson(runId);
		BridgeRunner.runAsync(runId, json,
				r -> setHint("Прогон завершён: " + r.status + ", passed=" + r.passedCount()
						+ ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	/** Запуск набора с согласованным runId */
	private void runActiveSet() {
		if (setNode == null) return;
		String moduleName = setNode.moduleName;
		String test = setNode.selectedTest == null ? "" : setNode.selectedTest;
		String what = test.isEmpty() ? "весь набор" : "тест " + test;
		setHint("Запускаю набор " + moduleName + " (" + what + ")…");

		String runId = LaunchMonitor.newRunId();
		String json = BridgeScenario.runSetJson(runId, moduleName, test);
		BridgeRunner.runAsync(runId, json,
				r -> setHint("Набор " + moduleName + ": " + r.status
						+ ", passed=" + r.passedCount() + ", failed=" + r.failedCount()),
				err -> setHint("Ошибка: " + err));
	}

	private void refreshActiveSet() {
		CommonModule cm = EditorModuleSupport.activeCommonModule(EditorModuleSupport.activeEditor());
		String name = cm == null ? null : EditorModuleSupport.moduleName(cm);
		if (name != null && EditorModuleSupport.isTestSetName(name)) {
			List<String> tests = EditorModuleSupport.testNames(cm);
			setNode = new RootSetNode(name, tests);
			if (!tests.isEmpty()) {
				setNode.selectedTest = tests.get(0);
			}
			if (runSetAction != null) {
				runSetAction.setEnabled(true);
				runSetAction.setToolTipText("Запустить набор " + name);
			}
		} else {
			setNode = null;
			if (runSetAction != null) {
				runSetAction.setEnabled(false);
			}
		}
		rebuild();
	}

	private void refreshFromResult(BridgeResult result) {
		lastResult = result;
		rebuild();
	}

	private void setHint(String text) {
		this.hintText = text;
		rebuild();
	}

	private void rebuild() {
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		boolean hasAny = setNode != null || lastResult != null;
		if (!hasAny && hintText == null) {
			hintText = "Ничего не открыто. Нажми «Запустить мост» или открой тестовый набор в редакторе.";
		}
		viewer.setInput(this);
		viewer.refresh();
		if (setNode != null || lastResult != null) {
			viewer.expandAll();
		}
	}

	@Override
	public void setFocus() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.getControl().setFocus();
		}
	}

	@Override
	public void dispose() {
		BridgeResultStore.get().removeListener(storeListener);
		try {
			if (getSite() != null && getSite().getPage() != null) {
				getSite().getPage().removePartListener(partListener);
			}
		} catch (RuntimeException e) {
			// игнорируем
		}
		super.dispose();
	}

	// Модели узлов TreeViewer
	static final class RootSetNode {
		final String moduleName;
		final List<TestNode> tests = new ArrayList<>();
		String selectedTest;
		RootSetNode(String moduleName, List<String> testNames) {
			this.moduleName = moduleName;
			for (String t : testNames) tests.add(new TestNode(t));
		}
	}

	static final class TestNode {
		final String name;
		TestNode(String name) { this.name = name; }
	}

	static final class RootResultNode {
		final BridgeResult result;
		final List<StepNode> steps = new ArrayList<>();
		RootResultNode(BridgeResult result) {
			this.result = result;
			for (BridgeResult.Step s : result.steps) steps.add(new StepNode(s));
		}
	}

	static final class StepNode {
		final BridgeResult.Step step;
		StepNode(BridgeResult.Step step) { this.step = step; }
	}

	private static final class NodeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			TestsView v = (TestsView) inputElement;
			List<Object> roots = new ArrayList<>();
			if (v.setNode != null) roots.add(v.setNode);
			if (v.lastResult != null) roots.add(new RootResultNode(v.lastResult));
			if (roots.isEmpty()) roots.add(new HintNode(v.hintText == null ? "" : v.hintText));
			return roots.toArray();
		}
		@Override
		public Object[] getChildren(Object el) {
			if (el instanceof RootSetNode) return ((RootSetNode) el).tests.toArray();
			if (el instanceof RootResultNode) return ((RootResultNode) el).steps.toArray();
			return new Object[0];
		}
		@Override public Object getParent(Object el) { return null; }
		@Override public boolean hasChildren(Object el) {
			return el instanceof RootSetNode || el instanceof RootResultNode;
		}
		@Override public void inputChanged(Viewer v, Object o, Object n) {}
	}

	static final class HintNode {
		final String text;
		HintNode(String text) { this.text = text; }
	}

	private static final class NodeLabelProvider extends LabelProvider implements IColorProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof HintNode) return ((HintNode) element).text;
			if (element instanceof RootSetNode) {
				RootSetNode n = (RootSetNode) element;
				return "Набор: " + n.moduleName + "   (тестов: " + n.tests.size()
						+ "; запуск через мост СП_Тестирование)";
			}
			if (element instanceof TestNode) return "🔬 " + ((TestNode) element).name;
			if (element instanceof RootResultNode) {
				RootResultNode n = (RootResultNode) element;
				BridgeResult r = n.result;
				return "Прогон моста — " + statusText(r.status)
						+ "   [" + r.passedCount() + " ✓ / " + r.failedCount() + " ✗], шагов: " + r.steps.size();
			}
			if (element instanceof StepNode) {
				StepNode n = (StepNode) element;
				return "#" + n.step.id + "  " + n.step.action + "  —  " + statusText(n.step.status);
			}
			return String.valueOf(element);
		}

		@Override
		public Color getForeground(Object element) {
			Display d = Display.getCurrent();
			if (d == null) d = Display.getDefault();
			if (d == null || d.isDisposed()) return null;

			if (element instanceof RootSetNode) {
				return d.getSystemColor(SWT.COLOR_BLUE);
			}
			if (element instanceof RootResultNode) {
				return ((RootResultNode) element).result.isFailed()
						? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
			}
			if (element instanceof StepNode) {
				return ((StepNode) element).step.isFailed()
						? d.getSystemColor(SWT.COLOR_RED) : d.getSystemColor(SWT.COLOR_DARK_GREEN);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		private String statusText(String status) {
			if ("passed".equals(status)) return "PASSED";
			if ("failed".equals(status)) return "FAILED";
			return String.valueOf(status);
		}
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/BridgeResultStore.java',
    title: 'BridgeResultStore.java',
    description: 'Центральное реактивное хранилище результатов прогона',
    status: 'modified',
    issuesCount: 1,
    diffSummary: 'Добавлен асинхронный метод reloadLatestFromOutDirAsync на базе Eclipse Job API для предотвращения зависания UI-потока.',
    originalCode: `public final class BridgeResultStore {
    ...
    // ❌ Только синхронная вычитка с блокировкой потока вызова:
    public BridgeResult reloadLatestFromOutDir() {
        try {
            File outDir = LaunchMonitor.outDir();
            if (outDir == null || !outDir.isDirectory()) return null;
            File[] files = outDir.listFiles((dir, name) ->
                    name.startsWith("bridge-result-") && name.endsWith(".json"));
            if (files == null || files.length == 0) return null;
            ...
            String body = LaunchMonitor.readFileSafe(latest);
            BridgeResult r = BridgeResult.parse(body, latest);
            if (r != null) set(r);
            return r;
        } catch (CoreException e) {
            return null;
        }
    }
}`,
    fixedCode: `package ru.ozon.uitp.e2e.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.launcher.LaunchMonitor;

/**
 * Потокобезопасный стор последнего результата прогона моста.
 * Поддерживает синхронное чтение и асинхронную подгрузку в фоновом Job.
 */
public final class BridgeResultStore {

	public interface Listener {
		void resultUpdated(BridgeResult result);
	}

	private static final BridgeResultStore INSTANCE = new BridgeResultStore();
	private final List<Listener> listeners = new ArrayList<>();
	private volatile BridgeResult current;

	private BridgeResultStore() {
	}

	public static BridgeResultStore get() {
		return INSTANCE;
	}

	public BridgeResult current() {
		return current;
	}

	public void set(BridgeResult result) {
		this.current = result;
		List<Listener> copy;
		synchronized (listeners) {
			copy = new ArrayList<>(listeners);
		}
		for (Listener l : copy) {
			l.resultUpdated(result);
		}
	}

	public void addListener(Listener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public void removeListener(Listener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	/**
	 * Асинхронно вычитывает последний результат в фоне, предотвращая подвисание UI.
	 */
	public void reloadLatestFromOutDirAsync(Consumer<BridgeResult> onComplete) {
		Job job = new Job("Specter: чтение последнего отчёта моста") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				BridgeResult result = reloadLatestFromOutDir();
				Display display = Display.getDefault();
				if (display != null && !display.isDisposed()) {
					display.asyncExec(() -> {
						if (onComplete != null) {
							onComplete.accept(result);
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * Синхронная вычитка файла. Рекомендуется вызывать только из фоновых Job!
	 */
	public BridgeResult reloadLatestFromOutDir() {
		try {
			File outDir = LaunchMonitor.outDir();
			if (outDir == null || !outDir.isDirectory()) {
				return null;
			}
			File[] files = outDir.listFiles((dir, name) ->
					name.startsWith("bridge-result-") && name.endsWith(".json"));
			if (files == null || files.length == 0) {
				return null;
			}
			File latest = files[0];
			for (File f : files) {
				if (f.lastModified() > latest.lastModified()) {
					latest = f;
				}
			}
			String body = LaunchMonitor.readFileSafe(latest);
			BridgeResult r = BridgeResult.parse(body, latest);
			if (r != null) {
				set(r);
			}
			return r;
		} catch (CoreException e) {
			return null;
		}
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/ResultsView.java',
    title: 'ResultsView.java',
    description: 'Панель детализации шагов теста на SWT StyledText',
    status: 'modified',
    issuesCount: 2,
    diffSummary: 'Исправлена формула оффсета StyleRange (устранен сдвиг подсветки), заменено O(N²) перевыделение массива на List<StyleRange>.',
    originalCode: `// Фрагмент с ошибками в ResultsView.java:
String[] lines = sb.toString().split("\\n", -1);
StyleRange[] ranges = new StyleRange[0];
for (int li = 0; li < lines.length; li++) {
    String line = lines[li];
    int colondIdx = line.indexOf("—");
    if (colondIdx < 0) continue;
    boolean failed = line.contains("FAILED");
    if (!failed && !line.contains("PASSED")) continue;
    int lineStart = offsetOf(sb, li);
    StyleRange sr = new StyleRange();
    sr.start = lineStart + colondIdx - 3; // ❌ Подсвечивает символы перед тире!
    sr.length = 6;
    sr.foreground = failed ? red : green;
    sr.fontStyle = SWT.BOLD;
    ranges = append(ranges, sr); // ❌ Медленное копирование массива System.arraycopy
}
text.setStyleRanges(ranges);`,
    fixedCode: `package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Панель «Результаты» (Specter): пошаговый лог сценария моста на SWT StyledText.
 * Корректный расчет StyleRange и линейная O(N) генерация разметки.
 */
public class ResultsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.ResultsView";
	private final BridgeResultStore.Listener storeListener = this::showResult;
	private StyledText text;

	@Override
	public void createPartControl(Composite parent) {
		text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setEditable(false);
		BridgeResultStore.get().addListener(storeListener);
		showResult(BridgeResultStore.get().current());
	}

	@Override
	public void setFocus() {
		if (text != null && !text.isDisposed()) {
			text.setFocus();
		}
	}

	@Override
	public void dispose() {
		BridgeResultStore.get().removeListener(storeListener);
		super.dispose();
	}

	private void showResult(BridgeResult r) {
		if (text == null || text.isDisposed()) {
			return;
		}
		if (r == null || "hint".equals(r.status)) {
			text.setText("Результат моста не найден. Запусти сценарий на панели «Тесты».");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Прогон моста: ").append(r.runId).append('\\n');
		sb.append("Статус: ").append(statusText(r.status));
		sb.append("   (passed ").append(r.passedCount()).append(" / failed ").append(r.failedCount()).append(')');
		if (r.source != null) {
			sb.append("\\nФайл: ").append(r.source.getAbsolutePath());
		}
		sb.append("\\n\\nШаги:\\n");
		for (BridgeResult.Step s : r.steps) {
			sb.append("  #").append(s.id).append("  ").append(s.action)
					.append("  —  ").append(statusText(s.status));
			if (!s.detail.isEmpty()) {
				sb.append("\\n      ").append(s.detail);
			}
			sb.append('\\n');
		}

		String fullText = sb.toString();
		text.setText(fullText);

		Color green = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);

		String[] lines = fullText.split("\\n", -1);
		List<StyleRange> ranges = new ArrayList<>();
		int lineStart = 0;

		for (String line : lines) {
			int statusIdx = line.indexOf("FAILED");
			boolean failed = true;
			int len = 6;
			if (statusIdx < 0) {
				statusIdx = line.indexOf("PASSED");
				failed = false;
				len = 6;
			}
			if (statusIdx >= 0) {
				StyleRange sr = new StyleRange();
				sr.start = lineStart + statusIdx; // Точное смещение слова статуса
				sr.length = len;
				sr.foreground = failed ? red : green;
				sr.fontStyle = SWT.BOLD;
				ranges.add(sr);
			}
			lineStart += line.length() + 1;
		}

		text.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	private String statusText(String status) {
		if ("passed".equals(status)) return "PASSED";
		if ("failed".equals(status)) return "FAILED";
		return String.valueOf(status);
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/launcher/LaunchMonitor.java',
    title: 'LaunchMonitor.java',
    description: 'Мониторинг процесса 1С и файлов обмена BSL-моста',
    status: 'modified',
    issuesCount: 1,
    diffSummary: 'Добавлена поддержка отмены (IProgressMonitor.isCanceled()), надежная проверка завершения процесса.',
    originalCode: `public static File waitForBridgeResult(File outDir, String runId, ILaunch launch, long timeoutMs) {
    File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
    long startedAt = System.currentTimeMillis();
    while (System.currentTimeMillis() - startedAt < timeoutMs) {
        if (expected.isFile()) return expected;
        if (isTerminated(launch)) return null;
        try {
            Thread.sleep(POLL_MS); // ❌ Бесконечно ждет при нажатии Cancel в Progress View
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    return null;
}`,
    fixedCode: `package ru.ozon.uitp.e2e.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import ru.ozon.uitp.e2e.Activator;

/**
 * Контроллер обмена и слежения за прогоном моста.
 * Обеспечивает антимаскировку по уникальному runId и поддержку отмены через IProgressMonitor.
 */
public final class LaunchMonitor {

	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";
	private static final String COMMANDS_FILE = "bridge-commands.json";
	private static final String RESULT_PREFIX = "bridge-result-";
	private static final String RESULT_SUFFIX = ".json";
	private static final long POLL_MS = 1000L;

	private LaunchMonitor() {
	}

	public static File outDir() throws CoreException {
		String out = System.getProperty(OUT_DIR_PROPERTY);
		if (out == null || out.isBlank()) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Не задано системное свойство -D" + OUT_DIR_PROPERTY
							+ " (каталог обмена моста). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		File dir = new File(out.trim());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public static String newRunId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public static void writeCommands(File outDir, String content) throws CoreException {
		File tmp = new File(outDir, COMMANDS_FILE + ".tmp");
		File target = new File(outDir, COMMANDS_FILE);
		try {
			Files.write(tmp.toPath(), content.getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.BUNDLE_ID,
					"Не удалось записать командный файл моста в " + target.getAbsolutePath(), e));
		}
	}

	/**
	 * Ожидает появления файла результата для конкретного runId с поддержкой отмены.
	 */
	public static File waitForBridgeResult(File outDir, String runId, ILaunch launch,
			long timeoutMs, IProgressMonitor monitor) {
		File expected = new File(outDir, RESULT_PREFIX + runId + RESULT_SUFFIX);
		long startedAt = System.currentTimeMillis();

		while (System.currentTimeMillis() - startedAt < timeoutMs) {
			if (monitor != null && monitor.isCanceled()) {
				info("Ожидание моста прервано пользователем (runId=" + runId + ")");
				return null;
			}
			if (expected.isFile()) {
				return expected;
			}
			if (isTerminated(launch)) {
				return null;
			}
			try {
				Thread.sleep(POLL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		return null;
	}

	public static int exitCodeOf(ILaunch launch) {
		if (launch == null) return Integer.MIN_VALUE;
		IProcess[] procs = launch.getProcesses();
		if (procs == null) return Integer.MIN_VALUE;
		for (IProcess p : procs) {
			if (p.isTerminated()) {
				try {
					return p.getExitValue();
				} catch (org.eclipse.debug.core.DebugException e) {
					// игнорируем
				}
			}
		}
		return Integer.MIN_VALUE;
	}

	private static boolean isTerminated(ILaunch launch) {
		if (launch == null) return false;
		IProcess[] procs = launch.getProcesses();
		if (procs == null || procs.length == 0) return launch.isTerminated();
		return Stream.of(procs).allMatch(IProcess::isTerminated);
	}

	public static String readFileSafe(File f) {
		if (f == null || !f.isFile()) return "";
		try {
			return Files.readString(f.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	public static void info(String msg) {
		ILog log = Platform.getLog(Platform.getBundle(Activator.BUNDLE_ID));
		if (log != null) {
			log.info(msg);
		}
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/launcher/BridgeLaunchHelper.java',
    title: 'BridgeLaunchHelper.java',
    description: 'Управление launch-конфигурациями 1C:EDT RuntimeClient',
    status: 'modified',
    issuesCount: 2,
    diffSummary: 'Устранено засорение workspace дублирующимися launch-конфигурациями, добавлена гибкая конфигурация имени -Duitp.e2e.launchConfig, поддержка отмены при поиске.',
    originalCode: `// Фрагменты с жестко зашитыми именами и утечкой файлов конфигурации:
public static final String SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ";
...
ILaunchConfigurationWorkingCopy wc = source.copy(PREFIX + source.getName());
wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION, ...);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);
ILaunchConfiguration copy = wc.doSave(); // ❌ Сохраняет новую копию каждый запуск!`,
    fixedCode: `package ru.ozon.uitp.e2e.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import com._1c.g5.v8.dt.launching.core.ILaunchConfigurationAttributes;
import ru.ozon.uitp.e2e.Activator;

/**
 * Хелпер программного запуска тонкого клиента 1С из EDT.
 * Оптимизирован для повторного использования копии конфигурации и гибкого сопоставления.
 */
public final class BridgeLaunchHelper {

	public static final String RUNTIME_CLIENT_TYPE_ID =
			"com._1c.g5.v8.dt.launching.core.RuntimeClient";
	public static final String DEFAULT_SOURCE_CONFIGURATION_NAME = "Тонкий клиент АУФ";
	public static final String LAUNCH_CONFIG_PROPERTY = "uitp.e2e.launchConfig";

	public static final String STARTUP_OPTION = "SPECTER_START_BRIDGE";
	public static final String STARTUP_OPTION_DELIM = "|";
	public static final String OUT_DIR_PARAM = "outDir=";

	public static final String OUT_DIR_PROPERTY = "uitp.e2e.outDir";
	public static final String USER_NAME_PROPERTY = "uitp.e2e.launchUser";
	public static final String USER_PASSWORD_PROPERTY = "uitp.e2e.launchPassword";

	private static final String PREFIX = "UITP - ";

	private BridgeLaunchHelper() {
	}

	private static IStatus error(String message) {
		return new Status(IStatus.ERROR, Activator.BUNDLE_ID, message);
	}

	private static String requiredProperty(String property, String what) throws CoreException {
		String value = System.getProperty(property);
		if (value == null || value.isBlank()) {
			throw new CoreException(error("Не задано системное свойство -D" + property
					+ " (" + what + "). Укажи его в среде EDT (eclipse.ini / -D)."));
		}
		return value.trim();
	}

	public static ILaunch launchClient(String mode, IProgressMonitor monitor) throws CoreException {
		ILaunchConfiguration source = findSourceConfiguration();

		String outDir = requiredProperty(OUT_DIR_PROPERTY, "каталог обмена моста");
		String user = requiredProperty(USER_NAME_PROPERTY, "пользователь ИБ для автовхода");
		String password = requiredProperty(USER_PASSWORD_PROPERTY, "пароль ИБ для автовхода");

		String targetName = PREFIX + source.getName();
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();

		// Ищем существующую копию, чтобы не размножать конфигурации в workspace:
		ILaunchConfiguration existing = null;
		for (ILaunchConfiguration c : lm.getLaunchConfigurations()) {
			if (targetName.equals(c.getName())) {
				existing = c;
				break;
			}
		}

		ILaunchConfigurationWorkingCopy wc = (existing != null)
				? existing.getWorkingCopy()
				: source.copy(targetName);

		wc.setAttribute(ILaunchConfigurationAttributes.STARTUP_OPTION,
				STARTUP_OPTION + STARTUP_OPTION_DELIM + OUT_DIR_PARAM + outDir);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_NAME, user);
		wc.setAttribute(ILaunchConfigurationAttributes.LAUNCH_USER_PASSWORD, password);

		ILaunchConfiguration copy = wc.doSave();
		String want = copy.getName();

		DebugUITools.launch(copy, mode);

		// Ждём появления ILaunch с контролем отмены
		for (int i = 0; i < 15; i++) {
			if (monitor != null && monitor.isCanceled()) {
				return null;
			}
			for (ILaunch l : lm.getLaunches()) {
				String name = l.getLaunchConfiguration() != null
						? l.getLaunchConfiguration().getName() : null;
				if (want.equals(name)) {
					return l;
				}
			}
			try {
				Thread.sleep(200L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return null;
	}

	public static ILaunchConfiguration findSourceConfiguration() throws CoreException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType rtc = lm.getLaunchConfigurationType(RUNTIME_CLIENT_TYPE_ID);
		if (rtc == null) {
			throw new CoreException(error("Нет EDT-типа конфигурации RuntimeClient: " + RUNTIME_CLIENT_TYPE_ID));
		}

		ILaunchConfiguration[] configs = lm.getLaunchConfigurations(rtc);
		if (configs == null || configs.length == 0) {
			throw new CoreException(error("В проекте нет ни одной конфигурации типа RuntimeClient (1С)"));
		}

		// 1. Проверяем системное свойство
		String custom = System.getProperty(LAUNCH_CONFIG_PROPERTY);
		if (custom != null && !custom.isBlank()) {
			for (ILaunchConfiguration cfg : configs) {
				if (custom.trim().equals(cfg.getName())) return cfg;
			}
		}

		// 2. Ищем дефолтное имя «Тонкий клиент АУФ»
		for (ILaunchConfiguration cfg : configs) {
			if (DEFAULT_SOURCE_CONFIGURATION_NAME.equals(cfg.getName())) {
				return cfg;
			}
		}

		// 3. Fallback на первую доступную
		return configs[0];
	}
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/META-INF/MANIFEST.MF',
    title: 'MANIFEST.MF',
    description: 'Конфигурация бандла OSGi и зависимости плагина',
    status: 'modified',
    issuesCount: 1,
    diffSummary: 'Добавлен обязательный перенос строки в конце файла (RFC 1960), экспортированы все публичные пакеты, добавлены runtime-зависимости.',
    originalCode: `Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Specter - EDT plugin (UI-тестирование 1С)
Bundle-SymbolicName: ru.ozon.uitp.e2e;singleton:=true
Bundle-Version: 0.1.0.qualifier
Bundle-Vendor: Ozon
Bundle-Activator: ru.ozon.uitp.e2e.Activator
Bundle-ActivationPolicy: lazy
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.core.runtime, org.eclipse.debug.core, org.eclipse.debug.ui, org.eclipse.ui, org.eclipse.ui.views, org.eclipse.jface, org.eclipse.swt, org.eclipse.ui.ide, com._1c.g5.v8.dt.launching.core, com._1c.g5.v8.dt.metadata, com._1c.g5.v8.dt.bsl.model
Export-Package: ru.ozon.uitp.e2e.launcher`,
    fixedCode: `Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Specter - EDT plugin (UI-тестирование 1С)
Bundle-SymbolicName: ru.ozon.uitp.e2e;singleton:=true
Bundle-Version: 0.2.0.qualifier
Bundle-Vendor: Ozon
Bundle-Activator: ru.ozon.uitp.e2e.Activator
Bundle-ActivationPolicy: lazy
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.core.resources,
 org.eclipse.core.jobs,
 org.eclipse.debug.core,
 org.eclipse.debug.ui,
 org.eclipse.ui,
 org.eclipse.ui.workbench,
 org.eclipse.ui.views,
 org.eclipse.jface,
 org.eclipse.swt,
 org.eclipse.ui.ide,
 com._1c.g5.v8.dt.launching.core,
 com._1c.g5.v8.dt.metadata,
 com._1c.g5.v8.dt.bsl.model
Export-Package: ru.ozon.uitp.e2e,
 ru.ozon.uitp.e2e.launcher,
 ru.ozon.uitp.e2e.views
`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/Activator.java',
    title: 'Activator.java',
    description: 'Жизненный цикл плагина Eclipse UI',
    status: 'modified',
    issuesCount: 1,
    diffSummary: 'Наследует AbstractUIPlugin вместо голого BundleActivator, предоставляет статический getDefault() и методы логирования.',
    originalCode: `package ru.ozon.uitp.e2e;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {
	public static final String BUNDLE_ID = "ru.ozon.uitp.e2e";
	@Override
	public void start(BundleContext context) {}
	@Override
	public void stop(BundleContext context) {}
}`,
    fixedCode: `package ru.ozon.uitp.e2e;

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
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

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
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/preferences/SpecterPreferencePage.java',
    title: 'SpecterPreferencePage.java',
    description: 'Страница настроек плагина 1C:EDT (org.eclipse.ui.preferencePages)',
    status: 'modified',
    issuesCount: 0,
    diffSummary: 'Создана страница настроек SpecterPreferencePage, наследующая FieldEditorPreferencePage, с полем DirectoryFieldEditor «Путь к тестам Vanessa Automation» и сохранением в IPreferenceStore.',
    originalCode: `// Страница настроек отсутствовала в плагине`,
    fixedCode: `package ru.ozon.uitp.e2e.preferences;

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
}`
  },
  {
    path: 'bundles/ru.ozon.uitp.e2e/plugin.xml',
    title: 'plugin.xml',
    description: 'Манифест расширений Eclipse плагина (Views, Markers, Preference Pages)',
    status: 'modified',
    issuesCount: 0,
    diffSummary: 'Зарегистрирована точка расширения org.eclipse.ui.preferencePages с классом ru.ozon.uitp.e2e.preferences.SpecterPreferencePage.',
    originalCode: `<!-- Без точки расширения preferencePages -->`,
    fixedCode: `<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
   <!-- Представления Views (TestsView, ExtensionTestsView, ResultsView) -->
   ...
   <!-- Страница настроек плагина Specter (Путь к тестам Vanessa Automation) -->
   <extension
         point="org.eclipse.ui.preferencePages">
      <page
            id="ru.ozon.uitp.e2e.preferences.SpecterPreferencePage"
            name="Specter"
            class="ru.ozon.uitp.e2e.preferences.SpecterPreferencePage">
      </page>
   </extension>
</plugin>`
  }
];
