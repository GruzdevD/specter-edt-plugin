import React, { useState } from 'react';
import { 
  Sparkles, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Play, 
  RotateCcw, 
  Filter, 
  Search, 
  ChevronRight, 
  ChevronDown, 
  Layers, 
  Eye, 
  Layout, 
  Check, 
  Copy, 
  Sun, 
  Moon, 
  Info, 
  Zap, 
  Sliders, 
  Terminal,
  Activity,
  Maximize2,
  Download,
  FileCode
} from 'lucide-react';
import { BslEditorSimulator } from './BslEditorSimulator';

interface TestCase {
  id: string;
  name: string;
  duration: number;
  status: 'passed' | 'failed' | 'running' | 'idle';
  steps: Array<{
    num: number;
    action: string;
    target: string;
    category: 'form' | 'input' | 'button' | 'assert';
    duration: number;
    status: 'passed' | 'failed';
    detail?: string;
  }>;
}

const SAMPLE_TESTS: TestCase[] = [
  {
    id: 'test-1',
    name: 'Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии',
    duration: 380,
    status: 'passed',
    steps: [
      { num: 1, action: 'ОткрытьФормуСписка', target: 'Справочник.Контрагенты', category: 'form', duration: 110, status: 'passed' },
      { num: 2, action: 'АктивизироватьСтроку', target: 'ИНН "7701234567"', category: 'input', duration: 45, status: 'passed' },
      { num: 3, action: 'ОткрытьКарточку', target: 'ООО "Вектор"', category: 'form', duration: 120, status: 'passed' },
      { num: 4, action: 'ПроверитьЗначение', target: 'Статус = "Действующий"', category: 'assert', duration: 35, status: 'passed' },
      { num: 5, action: 'ЗакрытьФорму', target: 'ФормаЭлемента', category: 'button', duration: 70, status: 'passed' }
    ]
  },
  {
    id: 'test-2',
    name: 'Тест_ПроведениеЗаказаПокупателя_РасчетСуммы',
    duration: 640,
    status: 'passed',
    steps: [
      { num: 1, action: 'СоздатьДокумент', target: 'Документ.ЗаказПокупателя', category: 'form', duration: 140, status: 'passed' },
      { num: 2, action: 'ЗаполнитьРеквизит', target: 'Контрагент = "ООО Ромашка"', category: 'input', duration: 60, status: 'passed' },
      { num: 3, action: 'ДобавитьСтрокуТЧ', target: 'Товары (Номенклатура, Кол-во, Цена)', category: 'input', duration: 180, status: 'passed' },
      { num: 4, action: 'НажатьКнопку', target: 'Команда "Провести"', category: 'button', duration: 190, status: 'passed' },
      { num: 5, action: 'ПроверитьСостояние', target: 'Документ.Проведен = Истина', category: 'assert', duration: 70, status: 'passed' }
    ]
  },
  {
    id: 'test-3',
    name: 'Тест_ВалидацияНекорректногоКПП_ВыводОшибки',
    duration: 210,
    status: 'passed',
    steps: [
      { num: 1, action: 'ОткрытьФорму', target: 'ФормаРедактированияРеквизитов', category: 'form', duration: 80, status: 'passed' },
      { num: 2, action: 'УстановитьТекст', target: 'КПП = "123"', category: 'input', duration: 40, status: 'passed' },
      { num: 3, action: 'ПроверитьОшибку', target: 'ТекстОшибки = "КПП должен содержать 9 цифр"', category: 'assert', duration: 90, status: 'passed' }
    ]
  }
];

export const DesignShowcase: React.FC = () => {
  const [designVersion, setDesignVersion] = useState<'improved' | 'legacy'>('improved');
  const [theme, setTheme] = useState<'dark' | 'light'>('light');
  const [workbenchTab, setWorkbenchTab] = useState<'views' | 'bsl-editor'>('bsl-editor');
  const [selectedTestId, setSelectedTestId] = useState<string>('test-1');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'passed' | 'failed'>('all');
  const [copiedReport, setCopiedReport] = useState(false);
  const [iconZoom, setIconZoom] = useState<'1x' | '2x' | '4x' | '8x'>('4x');
  const [copiedXml, setCopiedXml] = useState(false);

  // Состояние запуска теста прямо из BSL-редактора (YAxUnit стиль)
  const [runningEditorTest, setRunningEditorTest] = useState<string | null>(null);
  const [editorStepIndex, setEditorStepIndex] = useState<number>(0);
  const [editorTestFinished, setEditorTestFinished] = useState<boolean>(false);
  const [editorContextMenu, setEditorContextMenu] = useState<{ x: number; y: number; testName: string } | null>(null);

  const selectedTest = SAMPLE_TESTS.find(t => t.id === selectedTestId) || SAMPLE_TESTS[0];

  const handleRunFromEditor = (testId: string) => {
    setRunningEditorTest(testId);
    setEditorStepIndex(0);
    setEditorTestFinished(false);
    setSelectedTestId(testId);

    // Симулируем пошаговое выполнение Vanessa-действий
    const interval = setInterval(() => {
      setEditorStepIndex(prev => {
        if (prev >= 4) {
          clearInterval(interval);
          setEditorTestFinished(true);
          return prev;
        }
        return prev + 1;
      });
    }, 450);
  };

  const filteredTests = SAMPLE_TESTS.filter(t => {
    const matchesSearch = t.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = filterStatus === 'all' ? true : t.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const totalDuration = SAMPLE_TESTS.reduce((acc, t) => acc + t.duration, 0);
  const passedCount = SAMPLE_TESTS.filter(t => t.status === 'passed').length;
  const failedCount = SAMPLE_TESTS.filter(t => t.status === 'failed').length;

  const handleCopyReport = () => {
    navigator.clipboard.writeText(
      `Specter 1C:EDT Report\nModule: ОМ_ТестыКлиентскихСценариев\nPassed: ${passedCount}, Failed: ${failedCount}\nTotal Time: ${(totalDuration / 1000).toFixed(2)}s`
    );
    setCopiedReport(true);
    setTimeout(() => setCopiedReport(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner with Controls */}
      <div className="bg-white p-5 sm:p-6 rounded-2xl border border-slate-200/90 shadow-xs">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
                <Layout className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h2 className="text-base font-bold text-slate-900 tracking-tight">
                    Интерактивное сравнение UI: До и После рефакторинга
                  </h2>
                  <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    Визуальный стандарт EDT / SWT
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
                  Переключайтесь между <strong>«Старый сырой вид (До)»</strong> и <strong>«Улучшенный дизайн Specter (После)»</strong>, 
                  чтобы увидеть качественную разницу в эргономике, визуальной иерархии, акцентах и читаемости отчётов.
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            {/* Theme switcher for simulated EDT */}
            <div className="flex items-center bg-slate-100 p-1 rounded-xl text-xs font-semibold">
              <button
                onClick={() => setTheme('light')}
                className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg transition-all ${
                  theme === 'light' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Sun className="w-3.5 h-3.5 text-amber-500" />
                <span>Светлая EDT</span>
              </button>
              <button
                onClick={() => setTheme('dark')}
                className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg transition-all ${
                  theme === 'dark' ? 'bg-slate-800 text-white shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Moon className="w-3.5 h-3.5 text-indigo-400" />
                <span>Тёмная EDT</span>
              </button>
            </div>

            {/* Design Version Toggle */}
            <div className="flex items-center bg-slate-100 p-1 rounded-xl text-xs font-bold">
              <button
                id="toggle-improved"
                onClick={() => setDesignVersion('improved')}
                className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg transition-all ${
                  designVersion === 'improved'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Улучшенный дизайн (После)</span>
              </button>
              <button
                id="toggle-legacy"
                onClick={() => setDesignVersion('legacy')}
                className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg transition-all ${
                  designVersion === 'legacy'
                    ? 'bg-slate-700 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Базовый вид (До)</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main EDT Simulator Window */}
      <div className={`rounded-2xl border transition-colors duration-200 overflow-hidden shadow-md ${
        theme === 'dark' 
          ? 'bg-[#1e1e1e] border-slate-700 text-slate-100' 
          : 'bg-[#f8f9fa] border-slate-300 text-slate-800'
      }`}>
        {/* Eclipse / 1C:EDT Window Titlebar */}
        <div className={`px-4 py-2.5 flex items-center justify-between border-b text-xs select-none ${
          theme === 'dark'
            ? 'bg-[#252526] border-[#3c3c3c] text-slate-300'
            : 'bg-[#e9ecef] border-[#ced4da] text-slate-700'
        }`}>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 mr-2">
              <span className="w-3 h-3 rounded-full bg-[#ff5f56] inline-block"></span>
              <span className="w-3 h-3 rounded-full bg-[#ffbd2e] inline-block"></span>
              <span className="w-3 h-3 rounded-full bg-[#27c93f] inline-block"></span>
            </div>
            <span className="font-semibold">1C:Enterprise Development Tools (1C:EDT 2026.2)</span>
            <span className="opacity-40">|</span>
            <span className="font-mono text-[11px] opacity-75">Проект: УправлениеТорговлей • Среда UI-тестирования</span>
          </div>

          <div className="flex items-center gap-3 text-[11px]">
            <span className={`px-2 py-0.5 rounded font-mono ${
              designVersion === 'improved' 
                ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' 
                : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
            }`}>
              {designVersion === 'improved' ? '✨ Modern UX Mode' : '⚠️ Legacy Bare SWT'}
            </span>
          </div>
        </div>

        {/* Workbench Perspective Sub-Nav Tabs */}
        <div className={`px-4 py-2 border-b flex items-center justify-between text-xs font-semibold ${
          theme === 'dark' ? 'bg-[#2a2d2e] border-[#3c3c3c]' : 'bg-[#eef1f5] border-[#ced4da]'
        }`}>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setWorkbenchTab('bsl-editor')}
              className={`px-3 py-1.5 rounded-lg flex items-center gap-2 transition-all text-xs font-bold ${
                workbenchTab === 'bsl-editor'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <FileCode className="w-3.5 h-3.5" />
              <span>BSL Редактор (Gutter Маркеры & Запуск)</span>
              <span className="px-1.5 py-0.2 rounded text-[10px] bg-emerald-500/30 text-white border border-emerald-400/40">
                YAxUnit Style
              </span>
            </button>
            <button
              onClick={() => setWorkbenchTab('views')}
              className={`px-3 py-1.5 rounded-lg flex items-center gap-2 transition-all text-xs font-bold ${
                workbenchTab === 'views'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Layout className="w-3.5 h-3.5" />
              <span>Панели Views (Тесты & Результаты)</span>
            </button>
          </div>

          <div className="text-[11px] font-mono text-slate-500 hidden sm:block">
            {workbenchTab === 'bsl-editor' ? 'Поиск &Тест и префиксов Тест_* на полях BSL' : 'Дерево наборов модулей СП_Тесты_*'}
          </div>
        </div>

        {/* Workbench View Content */}
        {workbenchTab === 'bsl-editor' ? (
          <div className="p-3 sm:p-4">
            <BslEditorSimulator theme={theme} />
          </div>
        ) : (
        <div className="p-3 sm:p-4 grid grid-cols-1 lg:grid-cols-12 gap-3 sm:gap-4">
          
          {/* LEFT: TestsView (Панель «Тесты») */}
          <div className={`lg:col-span-5 rounded-xl border flex flex-col overflow-hidden shadow-xs ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-[#d0d7de]'
          }`}>
            {/* View Header */}
            <div className={`px-3.5 py-2.5 border-b flex items-center justify-between text-xs font-bold ${
              theme === 'dark' ? 'bg-[#2d2d2d] border-[#3c3c3c] text-slate-200' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-800'
            }`}>
              <div className="flex items-center gap-2">
                {designVersion === 'improved' ? (
                  <img 
                    src="/icons/specter-tests.png" 
                    alt="Specter Tests" 
                    className="w-4 h-4 shrink-0 rounded-xs shadow-xs" 
                  />
                ) : (
                  <span className="w-2.5 h-2.5 rounded-sm bg-blue-500"></span>
                )}
                <span>Панель «Тесты» (TestsView)</span>
              </div>
              <div className="flex items-center gap-1">
                {designVersion === 'improved' && (
                  <button
                    onClick={handleCopyReport}
                    title="Скопировать сводку тестов"
                    className={`p-1 rounded hover:bg-black/10 transition-colors ${theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-slate-600'}`}
                  >
                    {copiedReport ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                )}
                <span className="font-mono text-[10px] opacity-60">org.eclipse.ui.views</span>
              </div>
            </div>

            {/* View Body */}
            {designVersion === 'legacy' ? (
              /* --- LEGACY VIEW (До рефакторинга) --- */
              <div className="p-3 font-mono text-xs space-y-2 select-none flex-1 min-h-[360px]">
                <div className="text-slate-400 text-[11px] pb-2 border-b border-dashed border-slate-300 dark:border-slate-700">
                  // Базовый текстовый вывод дерева без структурирования:
                </div>
                <div className="text-blue-600 dark:text-blue-400 font-bold">
                  Набор: СП_ТестыКлиентскихСценариев (тестов: 3; запуск через мост СП_Тестирование)
                </div>
                <div className="pl-4 space-y-1 text-slate-700 dark:text-slate-300">
                  <div 
                    onClick={() => setSelectedTestId('test-1')} 
                    className={`cursor-pointer p-1 rounded ${selectedTestId === 'test-1' ? 'bg-blue-100 dark:bg-blue-900/40 font-bold' : ''}`}
                  >
                    🔬 Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии
                  </div>
                  <div 
                    onClick={() => setSelectedTestId('test-2')} 
                    className={`cursor-pointer p-1 rounded ${selectedTestId === 'test-2' ? 'bg-blue-100 dark:bg-blue-900/40 font-bold' : ''}`}
                  >
                    🔬 Тест_ПроведениеЗаказаПокупателя_РасчетСуммы
                  </div>
                  <div 
                    onClick={() => setSelectedTestId('test-3')} 
                    className={`cursor-pointer p-1 rounded ${selectedTestId === 'test-3' ? 'bg-blue-100 dark:bg-blue-900/40 font-bold' : ''}`}
                  >
                    🔬 Тест_ВалидацияНекорректногоКПП_ВыводОшибки
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-200 dark:border-slate-800">
                  <div className="text-emerald-600 dark:text-emerald-400 font-bold">
                    Прогон моста: passed (passed=3, failed=0)
                  </div>
                  <div className="text-slate-500 text-[11px] mt-1">
                    (Нет индикаторов времени, нет фильтра, нет прогресс-бара, текст монотонный)
                  </div>
                </div>
              </div>
            ) : (
              /* --- IMPROVED VIEW (После рефакторинга) --- */
              <div className="flex flex-col flex-1 min-h-[360px]">
                {/* Summary Metrics Bar */}
                <div className={`p-3 border-b space-y-2 ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafbfc] border-[#e1e4e8]'
                }`}>
                  <div className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <span className="font-bold flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Все тесты пройдены</span>
                      </span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        theme === 'dark' ? 'bg-emerald-950/80 text-emerald-300 border border-emerald-800' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      }`}>
                        3 / 3 (100%)
                      </span>
                    </div>

                    <span className="font-mono text-[11px] text-slate-500 flex items-center gap-1">
                      <Clock className="w-3 h-3 text-slate-400" />
                      {(totalDuration / 1000).toFixed(2)}s
                    </span>
                  </div>

                  {/* Visual Progress Line */}
                  <div className="w-full h-1.5 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden flex">
                    <div className="h-full bg-emerald-500 rounded-full w-full"></div>
                  </div>

                  {/* Search and Filters inside View */}
                  <div className="flex items-center gap-2 pt-1">
                    <div className={`flex-1 flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs ${
                      theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c] text-slate-200' : 'bg-white border-[#d0d7de] text-slate-700'
                    }`}>
                      <Search className="w-3 h-3 text-slate-400 shrink-0" />
                      <input 
                        type="text"
                        placeholder="Поиск по тестам..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="bg-transparent border-none outline-hidden text-[11px] w-full font-mono placeholder:text-slate-400"
                      />
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setFilterStatus('all')}
                        className={`px-2 py-1 rounded text-[10px] font-bold transition-colors ${
                          filterStatus === 'all' 
                            ? 'bg-indigo-600 text-white' 
                            : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
                        }`}
                      >
                        Все (3)
                      </button>
                      <button
                        onClick={() => setFilterStatus('passed')}
                        className={`px-2 py-1 rounded text-[10px] font-bold transition-colors ${
                          filterStatus === 'passed' 
                            ? 'bg-emerald-600 text-white' 
                            : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
                        }`}
                      >
                        Пройдено (3)
                      </button>
                    </div>
                  </div>
                </div>

                {/* Test Tree Items */}
                <div className="p-2 space-y-1.5 overflow-auto flex-1">
                  <div className="px-2 py-1 text-[11px] font-bold text-indigo-600 dark:text-indigo-400 flex items-center justify-between">
                    <div className="flex items-center gap-1.5 truncate">
                      <Layers className="w-3.5 h-3.5 shrink-0" />
                      <span className="truncate">ОМ_ТестыКлиентскихСценариев</span>
                    </div>
                    <span className="text-[10px] font-mono text-slate-400 shrink-0 font-normal">3 сценария</span>
                  </div>

                  {filteredTests.map((test) => {
                    const isSelected = selectedTestId === test.id;
                    return (
                      <div
                        key={test.id}
                        onClick={() => setSelectedTestId(test.id)}
                        className={`group p-2 rounded-xl cursor-pointer transition-all border ${
                          isSelected
                            ? theme === 'dark'
                              ? 'bg-indigo-950/40 border-indigo-500/50 shadow-xs'
                              : 'bg-indigo-50/80 border-indigo-200 shadow-xs'
                            : theme === 'dark'
                              ? 'hover:bg-[#2d2d2d] border-transparent'
                              : 'hover:bg-slate-100/70 border-transparent'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2 min-w-0">
                            <div className="p-1 rounded-md bg-emerald-100 dark:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 shrink-0">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                            </div>
                            <span className={`text-xs font-mono truncate ${isSelected ? 'font-bold text-indigo-900 dark:text-indigo-200' : 'text-slate-700 dark:text-slate-300'}`}>
                              {test.name}
                            </span>
                          </div>

                          <div className="flex items-center gap-1.5 shrink-0">
                            <span className="font-mono text-[10px] text-slate-400">
                              {test.duration}ms
                            </span>
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-emerald-100 dark:bg-emerald-900/80 text-emerald-800 dark:text-emerald-200 border border-emerald-200/60">
                              PASSED
                            </span>
                          </div>
                        </div>

                        {isSelected && (
                          <div className="mt-2 pt-2 border-t border-indigo-200/50 dark:border-indigo-800/40 flex items-center justify-between text-[10px] text-indigo-700 dark:text-indigo-300">
                            <span>Шагов выполнено: {test.steps.length}</span>
                            <span className="flex items-center gap-1">
                              Активен в ResultsView <ChevronRight className="w-3 h-3" />
                            </span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Tree Footer / Quick Action */}
                <div className={`p-2.5 border-t text-[11px] flex items-center justify-between ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-400' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-600'
                }`}>
                  <span className="flex items-center gap-1">
                    <Zap className="w-3 h-3 text-amber-500" />
                    <span>Быстрый запуск (F11 / BSL Bridge)</span>
                  </span>
                  <button className="px-2.5 py-1 rounded bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-[10px] shadow-xs transition-colors flex items-center gap-1">
                    <Play className="w-2.5 h-2.5 fill-current" />
                    <span>Перезапустить</span>
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* RIGHT: ResultsView (Панель «Результаты») */}
          <div className={`lg:col-span-7 rounded-xl border flex flex-col overflow-hidden shadow-xs ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-[#d0d7de]'
          }`}>
            {/* View Header */}
            <div className={`px-3.5 py-2.5 border-b flex items-center justify-between text-xs font-bold ${
              theme === 'dark' ? 'bg-[#2d2d2d] border-[#3c3c3c] text-slate-200' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-800'
            }`}>
              <div className="flex items-center gap-2">
                {designVersion === 'improved' ? (
                  <img 
                    src="/icons/specter-results.png" 
                    alt="Specter Results" 
                    className="w-4 h-4 shrink-0 rounded-xs shadow-xs" 
                  />
                ) : (
                  <span className="w-2.5 h-2.5 rounded-sm bg-indigo-500"></span>
                )}
                <span>Панель «Результаты» (ResultsView)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 font-bold">
                  run_984a1f (Согласован)
                </span>
                <span className="font-mono text-[10px] opacity-60">StyledText / SWT</span>
              </div>
            </div>

            {/* View Body */}
            {designVersion === 'legacy' ? (
              /* --- LEGACY VIEW (До рефакторинга) --- */
              <div className="p-3 font-mono text-xs space-y-2 select-none flex-1 min-h-[360px] bg-black text-green-400">
                <div className="text-slate-500 text-[10px]">
                  # Старый сырой монохромный терминальный вывод:
                </div>
                <div>Прогон моста: run_984a1f</div>
                <div>Статус: passed (passed=5, failed=0)</div>
                <div>Файл: /exchange/bridge-result-run_984a1f.json</div>
                <div className="text-slate-400">--------------------------------------------------</div>
                <div>#1 openList (Справочник.Контрагенты) - passed</div>
                <div>#2 activateRow (ИНН "7701234567") - passed</div>
                <div>#3 openCard (ООО "Вектор") - passed</div>
                <div>#4 assertValue (Статус = "Действующий") - passed</div>
                <div>#5 closeForm (ФормаЭлемента) - passed</div>
                <div className="text-slate-400">--------------------------------------------------</div>
                <div className="text-slate-500 text-[10px]">
                  (Отсутствует группировка по категориям действий, нет таймингов шагов, 
                  нет возможности раскрыть детали реквизитов, сплошной моноширинный текст)
                </div>
              </div>
            ) : (
              /* --- IMPROVED VIEW (После рефакторинга) --- */
              <div className="flex flex-col flex-1 min-h-[360px]">
                {/* Scenario Details Header */}
                <div className={`p-3 border-b flex flex-col sm:flex-row sm:items-center justify-between gap-2 ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafbfc] border-[#e1e4e8]'
                }`}>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                        {selectedTest.name}
                      </span>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-300">
                        УСПЕШНО ({selectedTest.steps.length} шагов)
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                      Исполнение через тонкий клиент 1C:Enterprise (режим run) • Общее время: <strong>{selectedTest.duration}ms</strong>
                    </p>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0 self-start sm:self-center">
                    <button 
                      onClick={handleCopyReport}
                      className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border text-[11px] font-semibold transition-colors ${
                        theme === 'dark' ? 'bg-[#2d2d2d] border-[#3c3c3c] text-slate-300 hover:text-white' : 'bg-white border-[#d0d7de] text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      <Copy className="w-3 h-3" />
                      <span>{copiedReport ? 'Скопировано' : 'Копировать'}</span>
                    </button>
                  </div>
                </div>

                {/* Structured Step Inspector */}
                <div className="p-3 space-y-2 overflow-auto flex-1 max-h-[330px]">
                  {selectedTest.steps.map((step) => {
                    const categoryColors = {
                      form: 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300 border-blue-200 dark:border-blue-800',
                      input: 'bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300 border-amber-200 dark:border-amber-800',
                      button: 'bg-purple-50 text-purple-700 dark:bg-purple-950/60 dark:text-purple-300 border-purple-200 dark:border-purple-800',
                      assert: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800'
                    };

                    const categoryLabels = {
                      form: '1С:Форма',
                      input: '1С:Реквизит',
                      button: '1С:Кнопка',
                      assert: '1С:Утверждение'
                    };

                    return (
                      <div 
                        key={step.num}
                        className={`p-2.5 rounded-xl border transition-colors flex items-center justify-between gap-3 text-xs ${
                          theme === 'dark' 
                            ? 'bg-[#1e1e1e]/60 border-[#333333] hover:border-[#444444]' 
                            : 'bg-slate-50/70 border-slate-200/80 hover:border-slate-300'
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <span className={`w-5 h-5 rounded-full flex items-center justify-center font-mono text-[10px] font-bold ${
                            theme === 'dark' ? 'bg-slate-800 text-slate-300' : 'bg-slate-200 text-slate-700'
                          }`}>
                            {step.num}
                          </span>

                          <div className="min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold border ${categoryColors[step.category]}`}>
                                {categoryLabels[step.category]}
                              </span>
                              <span className="font-mono font-bold text-slate-800 dark:text-slate-200 text-[11px]">
                                {step.action}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-500 dark:text-slate-400 font-mono truncate mt-0.5">
                              Цель: <span className="text-slate-700 dark:text-slate-300">{step.target}</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <span className="font-mono text-[10px] text-slate-400">
                            {step.duration}ms
                          </span>
                          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                            ✓ OK
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Footer status info */}
                <div className={`p-2.5 border-t text-[11px] flex items-center justify-between ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-400' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-600'
                }`}>
                  <span className="font-mono text-[10px]">
                    Синхронизация потоков: Display.asyncExec (UI-поток не блокируется)
                  </span>
                  <span className="text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1 text-[10px]">
                    <CheckCircle2 className="w-3 h-3" />
                    Результат проверен и доставлен в EDT
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
        )}
      </div>

      {/* Thematic Icons for Views (Тематические иконки плагина Specter) */}
      <div className="bg-white p-5 sm:p-6 rounded-2xl border border-slate-200/90 shadow-xs space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-50 text-indigo-700 rounded-xl border border-indigo-200">
              <Sparkles className="w-5 h-5 text-indigo-600" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                Тематические иконки Specter для заголовков панелей 1C:EDT
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                  v0.2.0 Asset Kit
                </span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Иконки отражают название <strong className="text-slate-700 font-semibold">Specter</strong> («Спектр / Призрак UI-тестирования 1С») и автоматически подключаются в <code className="font-mono text-slate-700">plugin.xml</code>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-center">
            <span className="text-xs text-slate-500 font-medium">Масштаб предпросмотра:</span>
            <div className="flex items-center bg-slate-100 p-0.5 rounded-lg text-xs font-mono">
              {(['1x', '2x', '4x', '8x'] as const).map(zoom => (
                <button
                  key={zoom}
                  onClick={() => setIconZoom(zoom)}
                  className={`px-2 py-1 rounded-md transition-colors ${
                    iconZoom === zoom 
                      ? 'bg-white text-slate-900 font-bold shadow-xs' 
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {zoom}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 2 Thematic Icons Showcase */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Icon 1: TestsView (Specter Tests) */}
          <div className="p-4 rounded-xl border border-slate-200/90 bg-slate-50/50 flex flex-col justify-between space-y-4">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 text-[11px] font-bold font-mono">
                  TestsView • Тесты
                </span>
                <span className="text-[11px] font-mono text-slate-400">
                  icons/specter-tests.png
                </span>
              </div>

              {/* Icon Visual Stage */}
              <div className="flex items-center justify-center p-6 rounded-xl bg-gradient-to-b from-slate-100 to-slate-200/80 border border-slate-200">
                <div className={`p-4 rounded-xl flex items-center justify-center transition-all ${
                  theme === 'dark' ? 'bg-[#252526] border border-[#3c3c3c]' : 'bg-white border border-slate-200 shadow-sm'
                }`}>
                  <img
                    src="/icons/specter-tests@2x.png"
                    alt="Specter Tests"
                    style={{
                      width: iconZoom === '1x' ? '16px' : iconZoom === '2x' ? '32px' : iconZoom === '4x' ? '64px' : '128px',
                      height: iconZoom === '1x' ? '16px' : iconZoom === '2x' ? '32px' : iconZoom === '4x' ? '64px' : '128px',
                      imageRendering: 'pixelated'
                    }}
                    className="transition-all duration-150"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <div className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                  <span>Смысл и концепция:</span>
                  <span className="text-indigo-600 font-semibold">«Призрак-инспектор тестов»</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Силуэт призрака (<strong>Specter</strong>) в спектральном фиолетово-синем градиенте с глазами-инспекторами и <strong>колбой лабораторного теста</strong> в правом нижнем углу. Подчёркивает автономное выполнение тестовых сценариев 1С без ручного вмешательства.
                </p>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-200/80 flex items-center justify-between text-xs">
              <span className="text-slate-500 font-mono text-[11px]">16x16px + 32x32px (@2x Retina)</span>
              <a
                href="/icons/specter-tests.png"
                download="specter-tests.png"
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 font-semibold shadow-2xs transition-colors text-[11px]"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Скачать PNG</span>
              </a>
            </div>
          </div>

          {/* Icon 2: ResultsView (Specter Results) */}
          <div className="p-4 rounded-xl border border-slate-200/90 bg-slate-50/50 flex flex-col justify-between space-y-4">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 text-[11px] font-bold font-mono">
                  ResultsView • Результаты
                </span>
                <span className="text-[11px] font-mono text-slate-400">
                  icons/specter-results.png
                </span>
              </div>

              {/* Icon Visual Stage */}
              <div className="flex items-center justify-center p-6 rounded-xl bg-gradient-to-b from-slate-100 to-slate-200/80 border border-slate-200">
                <div className={`p-4 rounded-xl flex items-center justify-center transition-all ${
                  theme === 'dark' ? 'bg-[#252526] border border-[#3c3c3c]' : 'bg-white border border-slate-200 shadow-sm'
                }`}>
                  <img
                    src="/icons/specter-results@2x.png"
                    alt="Specter Results"
                    style={{
                      width: iconZoom === '1x' ? '16px' : iconZoom === '2x' ? '32px' : iconZoom === '4x' ? '64px' : '128px',
                      height: iconZoom === '1x' ? '16px' : iconZoom === '2x' ? '32px' : iconZoom === '4x' ? '64px' : '128px',
                      imageRendering: 'pixelated'
                    }}
                    className="transition-all duration-150"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <div className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                  <span>Смысл и концепция:</span>
                  <span className="text-emerald-600 font-semibold">«Спектральный чек-лист и телеметрия»</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Фирменный аналитический планшет с неоновым спектральным фиксатором вверху, <strong>индикаторами успешных шагов 1С</strong> и зелеными метками валидации. Мгновенно считывается как панель детализированных результатов прогона.
                </p>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-200/80 flex items-center justify-between text-xs">
              <span className="text-slate-500 font-mono text-[11px]">16x16px + 32x32px (@2x Retina)</span>
              <a
                href="/icons/specter-results.png"
                download="specter-results.png"
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 font-semibold shadow-2xs transition-colors text-[11px]"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Скачать PNG</span>
              </a>
            </div>
          </div>
        </div>

        {/* Integration Code: plugin.xml & build.properties */}
        <div className="p-4 rounded-xl bg-slate-900 text-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <FileCode className="w-4 h-4 text-indigo-400" />
              <span className="font-bold text-white">Регистрация в plugin.xml плагина 1C:EDT</span>
            </div>
            <button
              onClick={() => {
                navigator.clipboard.writeText(`<view
      id="ru.ozon.uitp.e2e.views.TestsView"
      category="ru.ozon.uitp.e2e.views.category"
      name="Тесты"
      icon="icons/specter-tests.png"
      class="ru.ozon.uitp.e2e.views.TestsView"
      restorable="true">
</view>
<view
      id="ru.ozon.uitp.e2e.views.ResultsView"
      category="ru.ozon.uitp.e2e.views.category"
      name="Результаты"
      icon="icons/specter-results.png"
      class="ru.ozon.uitp.e2e.views.ResultsView"
      restorable="true">
</view>`);
                setCopiedXml(true);
                setTimeout(() => setCopiedXml(false), 2000);
              }}
              className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white font-mono text-[11px] flex items-center gap-1 transition-colors"
            >
              {copiedXml ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
              <span>{copiedXml ? 'Скопировано' : 'Копировать XML'}</span>
            </button>
          </div>
          <pre className="font-mono text-[11px] text-slate-300 overflow-x-auto p-2 rounded bg-black/40 leading-relaxed">
{`<!-- bundles/ru.ozon.uitp.e2e/plugin.xml -->
<view
      id="ru.ozon.uitp.e2e.views.TestsView"
      name="Тесты"
      icon="icons/specter-tests.png"
      class="ru.ozon.uitp.e2e.views.TestsView" />

<view
      id="ru.ozon.uitp.e2e.views.ResultsView"
      name="Результаты"
      icon="icons/specter-results.png"
      class="ru.ozon.uitp.e2e.views.ResultsView" />

<!-- bundles/ru.ozon.uitp.e2e/build.properties -->
bin.includes = META-INF/,.,plugin.xml,icons/`}
          </pre>
        </div>
      </div>

      {/* Breakdown Cards: 6 Key Visual & Architectural Upgrades */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-bold text-sm">
            1
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Сводный прогресс-бар и метрики (Summary Bar)
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> Только текстовая строка внизу экрана без шкалы.<br />
            <strong>После:</strong> Визуальная шкала выполнения, точный процент успешных прогонов, счетчик упавших тестов и общее время в секундах.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center font-bold text-sm">
            2
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Категоризация шагов 1С и читаемость
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> Сплошной неформатированный JSON или монохромная простыня лога.<br />
            <strong>После:</strong> Цветовые бейджи по типам операций (Форма, Реквизит, Кнопка, Утверждение) с выделением параметров.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center font-bold text-sm">
            3
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Тайминги и миллисекунды для каждого теста
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> Невозможно понять, какой шаг тормозит сценарий 1С.<br />
            <strong>После:</strong> Фиксация длительности каждого микро-шага (ms) для быстрой оптимизации медленных тестов.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center font-bold text-sm">
            4
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Безопасная палитра для светлой и тёмной темы
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> Жёстко заданные статические цвета SWT (SWT.COLOR_DARK_GREEN), вызывавшие выгорание контраста в тёмной теме.<br />
            <strong>После:</strong> Адаптивное получение цветов через <code className="font-mono text-slate-800">Display.getSystemColor()</code> с проверкой на null.
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center font-bold text-sm">
            5
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Фильтрация и быстрый поиск по сценариям
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> При наличии 50+ тестов приходилось долго скроллить всё дерево.<br />
            <strong>После:</strong> Мгновенный инкрементальный поиск по имени метода и фильтры (Только упавшие / Пройденные).
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-2">
          <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-700 flex items-center justify-center font-bold text-sm">
            6
          </div>
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">
            Плавные асинхронные индикаторы (Без зависаний)
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            <strong>До:</strong> Интерфейс 1C:EDT «замирал» и не реагировал на клики во время работы моста.<br />
            <strong>После:</strong> Перенос всей долгой работы в фоновый Eclipse Job с асинхронным обновлением через <code className="font-mono text-slate-800">Display.asyncExec()</code>.
          </p>
        </div>
      </div>
    </div>
  );
};
