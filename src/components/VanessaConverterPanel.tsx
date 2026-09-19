import React, { useState } from 'react';
import { 
  FolderOpen, 
  Sparkles, 
  ArrowRight, 
  Check, 
  CheckSquare, 
  Square, 
  Search, 
  Filter, 
  FileCode, 
  Play, 
  Copy, 
  Layers, 
  Cpu, 
  CheckCircle2, 
  RefreshCw, 
  Code2, 
  Zap, 
  FolderPlus, 
  Boxes,
  ShieldCheck,
  Tag,
  AlertCircle
} from 'lucide-react';
import { INITIAL_VANESSA_TESTS, VanessaTestItem } from '../data/vanessaTestsData';

interface VanessaConverterPanelProps {
  theme: 'dark' | 'light';
  vanessaPath?: string;
  onNavigateToRunner?: (testModuleName?: string) => void;
  onNavigateToSettings?: () => void;
}

export const VanessaConverterPanel: React.FC<VanessaConverterPanelProps> = ({
  theme,
  vanessaPath = '/workspace/tests/vanessa-automation/features',
  onNavigateToRunner,
  onNavigateToSettings
}) => {
  const [tests, setTests] = useState<VanessaTestItem[]>(INITIAL_VANESSA_TESTS);
  const [selectedIds, setSelectedIds] = useState<string[]>(INITIAL_VANESSA_TESTS.map(t => t.id));
  const [activeTestId, setActiveTestId] = useState<string>(INITIAL_VANESSA_TESTS[0].id);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTag, setSelectedTag] = useState<string>('all');
  const [previewTab, setPreviewTab] = useState<'bsl' | 'feature' | 'mdo' | 'mapping'>('bsl');
  const [isConverting, setIsConverting] = useState(false);
  const [conversionProgress, setConversionProgress] = useState(0);
  const [conversionSuccess, setConversionSuccess] = useState(false);
  const [convertedCount, setConvertedCount] = useState(0);
  const [copied, setCopied] = useState(false);

  const activeTest = tests.find(t => t.id === activeTestId) || tests[0];

  // Extract all unique tags
  const allTags = Array.from(new Set(tests.flatMap(t => t.tags)));

  // Filter tests
  const filteredTests = tests.filter(test => {
    const matchesSearch = 
      test.scenarioName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.featureName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.targetModuleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.tags.some(t => t.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesTag = selectedTag === 'all' || test.tags.includes(selectedTag);

    return matchesSearch && matchesTag;
  });

  const toggleSelect = (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setSelectedIds(prev => 
      prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
    );
  };

  const selectAll = () => {
    setSelectedIds(filteredTests.map(t => t.id));
  };

  const deselectAll = () => {
    setSelectedIds([]);
  };

  const handleConvert = () => {
    if (selectedIds.length === 0) return;
    
    setIsConverting(true);
    setConversionProgress(0);

    const interval = setInterval(() => {
      setConversionProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsConverting(false);
          setConversionSuccess(true);
          setConvertedCount(selectedIds.length);
          // Update status in state
          setTests(current => 
            current.map(t => selectedIds.includes(t.id) ? { ...t, status: 'converted' } : t)
          );
          return 100;
        }
        return prev + 25;
      });
    }, 250);
  };

  const handleCopyCode = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-4 animate-in fade-in duration-300">
      {/* Top Banner: Path & Recognition Status */}
      <div className={`p-4 rounded-xl border flex flex-col md:flex-row items-start md:items-center justify-between gap-3 shadow-xs ${
        theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-slate-200'
      }`}>
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1 rounded-md bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300">
              <Zap className="w-4 h-4" />
            </span>
            <h2 className="text-sm font-bold text-slate-900 dark:text-white">
              Конвертер тестов Vanessa Automation → Модули расширения (СП_Тестирование)
            </h2>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
              1 тест = 1 CommonModule
            </span>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
            <span>Каталог тестов:</span>
            <code className="px-2 py-0.5 rounded font-mono text-[11px] bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border border-slate-200 dark:border-slate-700">
              {vanessaPath}
            </code>
            {onNavigateToSettings && (
              <button 
                onClick={onNavigateToSettings}
                className="text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline font-semibold"
              >
                (Изменить в Preferences)
              </button>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 self-stretch md:self-auto justify-end">
          <button
            onClick={() => {
              setTests(INITIAL_VANESSA_TESTS);
              setSelectedIds(INITIAL_VANESSA_TESTS.map(t => t.id));
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold border flex items-center gap-1.5 transition-colors ${
              theme === 'dark'
                ? 'bg-[#2d2d2d] hover:bg-[#3c3c3c] border-[#4c4c4c] text-slate-300'
                : 'bg-slate-50 hover:bg-slate-100 border-slate-300 text-slate-700'
            }`}
          >
            <RefreshCw className="w-3.5 h-3.5 text-indigo-500" />
            <span>Пересканировать</span>
          </button>

          <button
            onClick={handleConvert}
            disabled={selectedIds.length === 0 || isConverting}
            className={`px-4 py-1.5 rounded-lg text-xs font-bold flex items-center gap-2 transition-all shadow-xs ${
              selectedIds.length > 0 && !isConverting
                ? 'bg-purple-600 hover:bg-purple-700 text-white cursor-pointer active:scale-98'
                : 'bg-slate-200 dark:bg-slate-800 text-slate-400 cursor-not-allowed'
            }`}
          >
            <Boxes className="w-4 h-4" />
            <span>
              {isConverting 
                ? `Конвертация (${conversionProgress}%)...` 
                : `Конвертировать (${selectedIds.length})`}
            </span>
          </button>
        </div>
      </div>

      {/* Conversion Success Notification */}
      {conversionSuccess && (
        <div className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/70 border border-emerald-200 dark:border-emerald-800 text-emerald-900 dark:text-emerald-100 text-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 animate-in fade-in duration-200">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0" />
            <div>
              <span className="font-bold">Успешно сконвертировано {convertedCount} сценариев!</span>
              <p className="text-[11px] text-emerald-700 dark:text-emerald-300">
                Новые общие модули созданы в <code className="font-mono">src/CommonModules/</code> и зарегистрированы в <code className="font-mono">Configuration.mdo</code>.
              </p>
            </div>
          </div>
          {onNavigateToRunner && (
            <button
              onClick={() => onNavigateToRunner(activeTest.targetModuleName)}
              className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-xs transition-colors shrink-0"
            >
              <Play className="w-3.5 h-3.5" />
              <span>Запустить в Specter Runner</span>
            </button>
          )}
        </div>
      )}

      {/* Main Dual-Column Interface */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: Discovered Tests List (5 cols) */}
        <div className={`lg:col-span-5 rounded-xl border flex flex-col shadow-xs overflow-hidden ${
          theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-white border-slate-200'
        }`}>
          {/* Filter & Search Header */}
          <div className={`p-3 border-b space-y-2.5 ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-slate-50 border-slate-200'
          }`}>
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                <FolderOpen className="w-3.5 h-3.5 text-amber-500" />
                <span>Распознанные тесты Vanessa ({filteredTests.length})</span>
              </span>

              <div className="flex items-center gap-1.5 text-xs">
                <button
                  onClick={selectAll}
                  className="px-2 py-0.5 rounded text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950"
                >
                  Все
                </button>
                <span className="text-slate-300 dark:text-slate-600">|</span>
                <button
                  onClick={deselectAll}
                  className="px-2 py-0.5 rounded text-[11px] font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-800"
                >
                  Снять
                </button>
              </div>
            </div>

            {/* Search Box */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Поиск по сценарию, тегу или модулю..."
                className={`w-full pl-8 pr-3 py-1.5 text-xs rounded-lg border outline-hidden transition-all ${
                  theme === 'dark'
                    ? 'bg-[#1e1e1e] border-[#3c3c3c] text-white focus:border-indigo-500'
                    : 'bg-white border-slate-300 text-slate-900 focus:border-indigo-500'
                }`}
              />
            </div>

            {/* Tags Pills */}
            <div className="flex flex-wrap gap-1">
              <button
                onClick={() => setSelectedTag('all')}
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold transition-all ${
                  selectedTag === 'all'
                    ? 'bg-purple-600 text-white'
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-300'
                }`}
              >
                Все теги
              </button>
              {allTags.map(tag => (
                <button
                  key={tag}
                  onClick={() => setSelectedTag(tag)}
                  className={`px-2 py-0.5 rounded-full text-[10px] font-mono transition-all ${
                    selectedTag === tag
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-300 dark:border-slate-700 hover:border-indigo-400'
                  }`}
                >
                  @{tag}
                </button>
              ))}
            </div>
          </div>

          {/* Test Cards List */}
          <div className="divide-y divide-slate-100 dark:divide-slate-800 max-h-[520px] overflow-y-auto">
            {filteredTests.map((test) => {
              const isSelected = selectedIds.includes(test.id);
              const isActive = activeTestId === test.id;

              return (
                <div
                  key={test.id}
                  onClick={() => setActiveTestId(test.id)}
                  className={`p-3 transition-all cursor-pointer flex items-start gap-2.5 select-none ${
                    isActive
                      ? theme === 'dark'
                        ? 'bg-indigo-950/40 border-l-4 border-indigo-500'
                        : 'bg-indigo-50/70 border-l-4 border-indigo-600'
                      : theme === 'dark'
                      ? 'hover:bg-[#252526]'
                      : 'hover:bg-slate-50'
                  }`}
                >
                  {/* Checkbox */}
                  <button
                    onClick={(e) => toggleSelect(test.id, e)}
                    className="mt-0.5 text-slate-400 hover:text-indigo-600 transition-colors"
                  >
                    {isSelected ? (
                      <CheckSquare className="w-4 h-4 text-purple-600 dark:text-purple-400" />
                    ) : (
                      <Square className="w-4 h-4" />
                    )}
                  </button>

                  <div className="flex-1 min-w-0 space-y-1">
                    <div className="flex items-center justify-between gap-1">
                      <span className="text-xs font-bold text-slate-900 dark:text-white truncate">
                        {test.scenarioName}
                      </span>
                      {test.status === 'converted' && (
                        <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 shrink-0">
                          Сконвертирован
                        </span>
                      )}
                    </div>

                    <div className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                      {test.featureFile}
                    </div>

                    <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                      <span className="px-1.5 py-0.2 rounded text-[10px] font-mono bg-purple-50 dark:bg-purple-950 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800">
                        {test.targetModuleName}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        • {test.stepsCount} шагов
                      </span>
                      <div className="flex gap-1 ml-auto">
                        {test.tags.map(t => (
                          <span key={t} className="text-[9px] font-mono text-slate-400 dark:text-slate-500">
                            @{t}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Bottom Summary Bar */}
          <div className={`p-2.5 border-t text-xs flex items-center justify-between text-slate-500 dark:text-slate-400 ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-slate-50 border-slate-200'
          }`}>
            <span>Выбрано: <strong>{selectedIds.length}</strong> из {tests.length}</span>
            <span className="text-[11px]">Движок: <strong>СП_Тестирование</strong></span>
          </div>
        </div>

        {/* Right Column: Code Generator & Side-by-Side Dual-Editor (7 cols) */}
        <div className={`lg:col-span-7 rounded-xl border flex flex-col shadow-xs overflow-hidden ${
          theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-white border-slate-200'
        }`}>
          {/* Preview Navigation Tabs */}
          <div className={`p-2.5 border-b flex flex-wrap items-center justify-between gap-2 ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-slate-50 border-slate-200'
          }`}>
            <div className="flex items-center gap-1 bg-slate-200/70 dark:bg-slate-800 p-0.5 rounded-lg text-xs font-semibold">
              <button
                onClick={() => setPreviewTab('bsl')}
                className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                  previewTab === 'bsl'
                    ? 'bg-white dark:bg-slate-700 text-indigo-600 dark:text-indigo-400 shadow-xs font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <Code2 className="w-3.5 h-3.5" />
                <span>Генерируемый Module.bsl</span>
              </button>

              <button
                onClick={() => setPreviewTab('feature')}
                className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                  previewTab === 'feature'
                    ? 'bg-white dark:bg-slate-700 text-purple-600 dark:text-purple-400 shadow-xs font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <FileCode className="w-3.5 h-3.5" />
                <span>Исходный .feature</span>
              </button>

              <button
                onClick={() => setPreviewTab('mdo')}
                className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                  previewTab === 'mdo'
                    ? 'bg-white dark:bg-slate-700 text-emerald-600 dark:text-emerald-400 shadow-xs font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                <span>{activeTest.targetModuleName}.mdo</span>
              </button>

              <button
                onClick={() => setPreviewTab('mapping')}
                className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 ${
                  previewTab === 'mapping'
                    ? 'bg-white dark:bg-slate-700 text-amber-600 dark:text-amber-400 shadow-xs font-bold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <Cpu className="w-3.5 h-3.5" />
                <span>Карта шагов API</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  const textToCopy = 
                    previewTab === 'bsl' ? activeTest.generatedBslCode :
                    previewTab === 'feature' ? activeTest.featureSource :
                    `<?xml version="1.0" encoding="UTF-8"?>\n<mdclass:CommonModule xmlns:mdclass="http://g5.1c.ru/v8/dt/metadata/mdclass">\n  <name>${activeTest.targetModuleName}</name>\n  <clientManagedApplication>true</clientManagedApplication>\n  <clientOrdinaryApplication>true</clientOrdinaryApplication>\n</mdclass:CommonModule>`;
                  handleCopyCode(textToCopy);
                }}
                className={`p-1.5 rounded-lg border text-xs font-semibold flex items-center gap-1 transition-colors ${
                  theme === 'dark'
                    ? 'bg-[#2d2d2d] hover:bg-[#3c3c3c] border-[#4c4c4c] text-slate-300'
                    : 'bg-white hover:bg-slate-100 border-slate-300 text-slate-700'
                }`}
                title="Копировать код"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                <span className="text-[11px]">{copied ? 'Скопировано' : 'Копировать'}</span>
              </button>
            </div>
          </div>

          {/* Code Body */}
          <div className="flex-1 p-4 font-mono text-xs overflow-auto max-h-[480px]">
            {previewTab === 'bsl' && (
              <pre className={`leading-relaxed ${
                theme === 'dark' ? 'text-emerald-300' : 'text-slate-800'
              }`}>
                {activeTest.generatedBslCode}
              </pre>
            )}

            {previewTab === 'feature' && (
              <div className="space-y-2">
                <div className="text-[11px] text-slate-400 font-sans pb-1 border-b border-slate-700/40 flex items-center justify-between">
                  <span>Файл: {activeTest.featureFile}</span>
                  <span>Язык: Gherkin / Vanessa Automation</span>
                </div>
                <pre className={`leading-relaxed ${
                  theme === 'dark' ? 'text-amber-300' : 'text-slate-800'
                }`}>
                  {activeTest.featureSource}
                </pre>
              </div>
            )}

            {previewTab === 'mdo' && (
              <pre className={`leading-relaxed ${
                theme === 'dark' ? 'text-sky-300' : 'text-slate-800'
              }`}>
{`<?xml version="1.0" encoding="UTF-8"?>
<mdclass:CommonModule xmlns:mdclass="http://g5.1c.ru/v8/dt/metadata/mdclass" uuid="b4f2c901-81d3-4f9e-a892-38d0172bf891">
  <name>${activeTest.targetModuleName}</name>
  <synonym>
    <key>ru</key>
    <value>Тест: ${activeTest.scenarioName}</value>
  </synonym>
  <comment>Сконвертировано из Vanessa Automation в движок СП_Тестирование</comment>
  <clientManagedApplication>true</clientManagedApplication>
  <clientOrdinaryApplication>true</clientOrdinaryApplication>
  <server>true</server>
</mdclass:CommonModule>`}
              </pre>
            )}

            {previewTab === 'mapping' && (
              <div className="space-y-3 font-sans">
                <div className="text-xs font-bold text-slate-900 dark:text-white">
                  Таблица соответствия шагов Vanessa → Методы СП_Тестирование:
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
                  <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 space-y-1">
                    <div className="font-bold text-amber-700 dark:text-amber-300">Vanessa Automation (Gherkin)</div>
                    <div className="text-[11px] font-mono text-slate-700 dark:text-slate-300">Дано Я открываю форму "..."</div>
                    <div className="text-[11px] font-mono text-slate-700 dark:text-slate-300">Когда Я нажимаю кнопку "..."</div>
                    <div className="text-[11px] font-mono text-slate-700 dark:text-slate-300">И Я заполняю поле "..." текстом "..."</div>
                    <div className="text-[11px] font-mono text-slate-700 dark:text-slate-300">Тогда Я жду появления элемента "..."</div>
                    <div className="text-[11px] font-mono text-slate-700 dark:text-slate-300">И Поле "..." содержит "..."</div>
                  </div>

                  <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/30 space-y-1">
                    <div className="font-bold text-indigo-700 dark:text-indigo-300">Движок СП_Тестирование (BSL)</div>
                    <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">СП_ТестовыйКлиент.ОткрытьФорму(...)</div>
                    <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">СП_ДействияКлиент.НажатьКнопку(...)</div>
                    <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">СП_ДействияКлиент.ЗаполнитьПоле(...)</div>
                    <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">СП_ОжиданияКлиент.ЖдатьПоявленияЭлемента(...)</div>
                    <div className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400">СП_Утверждения.УтверждениеРавенство(...)</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
