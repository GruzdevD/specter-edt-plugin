import React, { useState } from 'react';
import { 
  Play, 
  RotateCcw, 
  Layers, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Search, 
  Filter, 
  Sparkles, 
  ChevronRight, 
  ChevronDown,
  Terminal,
  Zap,
  Folder,
  Check,
  AppWindow,
  FileCode,
  ShieldCheck,
  Eye,
  Layout
} from 'lucide-react';

export interface ExtensionTestItem {
  id: string;
  name: string;
  description: string;
  durationMs: number;
  status: 'idle' | 'running' | 'passed' | 'failed';
  stepsCount: number;
}

export interface ExtensionModuleItem {
  id: string;
  moduleName: string;
  description: string;
  category: string;
  tests: ExtensionTestItem[];
}

const INITIAL_EXTENSION_MODULES: ExtensionModuleItem[] = [
  {
    id: 'mod-client-scenarios',
    moduleName: 'СП_ТестыКлиентскихСценариев',
    description: 'Клиентские сценарии управляемых форм и элементов интерфейса 1С',
    category: 'UI & Vanessa',
    tests: [
      {
        id: 't-client-1',
        name: 'Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии',
        description: 'Проверка полей формы, ИНН, КПП и доступности командных кнопок',
        durationMs: 380,
        status: 'passed',
        stepsCount: 5
      },
      {
        id: 't-client-2',
        name: 'Тест_ПроведениеЗаказаПокупателя_РасчетСуммы',
        description: 'Добавление строк в ТЧ Товары, расчёт скидки и нажатие команды Провести',
        durationMs: 640,
        status: 'passed',
        stepsCount: 5
      },
      {
        id: 't-client-3',
        name: 'Тест_ВалидацияНекорректногоКПП_ВыводОшибки',
        description: 'Ввод неверного формата КПП и проверка вывода сообщения об ошибке',
        durationMs: 210,
        status: 'passed',
        stepsCount: 3
      },
      {
        id: 't-client-4',
        name: 'Тест_ДинамическаяКартаАктивнойФормы_Инспектор',
        description: 'Снятие карты всех контролов текущей формы через модуль СП_ИнспекторКлиент',
        durationMs: 145,
        status: 'passed',
        stepsCount: 4
      }
    ]
  },
  {
    id: 'mod-forms',
    moduleName: 'СП_ТестыФорм',
    description: 'Сценарии обработки СП_УправлениеФормами и проверки свойств',
    category: 'Управление формами',
    tests: [
      {
        id: 't-form-1',
        name: 'Тест_ОткрытиеКарточки_ПроверкаЭлементов',
        description: 'Проверка видимости и доступности ключевых реквизитов формы карточки',
        durationMs: 290,
        status: 'passed',
        stepsCount: 4
      },
      {
        id: 't-form-2',
        name: 'Тест_ЗаполнениеПолей_БлокировкаКнопок',
        description: 'Тестирование автоблокировки команд при наличии несохраненных изменений',
        durationMs: 310,
        status: 'passed',
        stepsCount: 4
      },
      {
        id: 't-form-3',
        name: 'Тест_ПереходПоВкладкам_Валидация',
        description: 'Переключение страниц панели формы и проверка установки фокуса ввода',
        durationMs: 195,
        status: 'passed',
        stepsCount: 3
      }
    ]
  },
  {
    id: 'mod-asserts',
    moduleName: 'СП_ТестыУтверждений',
    description: 'Набор системных проверок и проверок модуля СП_Утверждения',
    category: 'Ассерт-движок',
    tests: [
      {
        id: 't-assert-1',
        name: 'Тест_АссертРавно_ЧислаИСтроки',
        description: 'Проверка равенства типов и строк без маскировки ошибок',
        durationMs: 45,
        status: 'passed',
        stepsCount: 3
      },
      {
        id: 't-assert-2',
        name: 'Тест_АссертИстина_Условия',
        description: 'Проверка булевых выражений и логических операторов',
        durationMs: 38,
        status: 'passed',
        stepsCount: 3
      },
      {
        id: 't-assert-3',
        name: 'Тест_АссертМодифицированность_СбросФлага',
        description: 'Контроль флага Модифицированность формы после сохранения',
        durationMs: 60,
        status: 'passed',
        stepsCount: 2
      }
    ]
  },
  {
    id: 'mod-bridge',
    moduleName: 'СП_ТестыИнтеграции',
    description: 'Проверка протокола связи плагина EDT с тонким клиентом 1С',
    category: 'Клиент-серверный обмен',
    tests: [
      {
        id: 't-bridge-1',
        name: 'Тест_СквознойЗапуск_Антимаскировка',
        description: 'Передача runId и валидация схемы JSON-результата без подавления сбоев',
        durationMs: 520,
        status: 'passed',
        stepsCount: 6
      },
      {
        id: 't-bridge-2',
        name: 'Тест_ПарсингКомандJson_СогласованныйRunId',
        description: 'Парсинг массива команд сценария 1С и контроль статуса завершения',
        durationMs: 110,
        status: 'passed',
        stepsCount: 2
      }
    ]
  }
];

interface Props {
  theme?: 'dark' | 'light';
  onSelectTestForDetails?: (testName: string, moduleName: string) => void;
}

export const ExtensionTestsPanel: React.FC<Props> = ({ 
  theme = 'light',
  onSelectTestForDetails 
}) => {
  const [modules, setModules] = useState<ExtensionModuleItem[]>(INITIAL_EXTENSION_MODULES);
  const [expandedModules, setExpandedModules] = useState<Record<string, boolean>>({
    'mod-client-scenarios': true,
    'mod-forms': true,
    'mod-asserts': true,
    'mod-bridge': true
  });
  const [selectedTestId, setSelectedTestId] = useState<string>('t-client-1');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [isRunningGlobal, setIsRunningGlobal] = useState(false);
  const [lastExecutedInfo, setLastExecutedInfo] = useState<string | null>(null);

  const toggleModuleExpand = (id: string) => {
    setExpandedModules(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const totalTests = modules.reduce((acc, m) => acc + m.tests.length, 0);
  const passedTests = modules.reduce((acc, m) => acc + m.tests.filter(t => t.status === 'passed').length, 0);
  const failedTests = modules.reduce((acc, m) => acc + m.tests.filter(t => t.status === 'failed').length, 0);

  // Запуск одиночного теста
  const runSingleTest = (moduleId: string, testId: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    
    setModules(prev => prev.map(m => {
      if (m.id !== moduleId) return m;
      return {
        ...m,
        tests: m.tests.map(t => t.id === testId ? { ...t, status: 'running' as const } : t)
      };
    }));

    const targetMod = modules.find(m => m.id === moduleId);
    const targetTest = targetMod?.tests.find(t => t.id === testId);
    setLastExecutedInfo(`Запущен тест: ${targetMod?.moduleName}.${targetTest?.name}`);

    setTimeout(() => {
      setModules(prev => prev.map(m => {
        if (m.id !== moduleId) return m;
        return {
          ...m,
          tests: m.tests.map(t => t.id === testId ? { ...t, status: 'passed' as const, durationMs: Math.round(150 + Math.random() * 300) } : t)
        };
      }));
      setLastExecutedInfo(`Успешно завершён: ${targetMod?.moduleName}.${targetTest?.name} (✓ PASSED)`);
    }, 800);
  };

  // Запуск целого модуля
  const runModule = (moduleId: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();

    setModules(prev => prev.map(m => {
      if (m.id !== moduleId) return m;
      return {
        ...m,
        tests: m.tests.map(t => ({ ...t, status: 'running' as const }))
      };
    }));

    const targetMod = modules.find(m => m.id === moduleId);
    setLastExecutedInfo(`Запущен модуль: ${targetMod?.moduleName} (${targetMod?.tests.length} тестов)`);

    setTimeout(() => {
      setModules(prev => prev.map(m => {
        if (m.id !== moduleId) return m;
        return {
          ...m,
          tests: m.tests.map(t => ({ ...t, status: 'passed' as const, durationMs: Math.round(120 + Math.random() * 250) }))
        };
      }));
      setLastExecutedInfo(`Модуль выполнен: ${targetMod?.moduleName} (все тесты пройдены)`);
    }, 1200);
  };

  // Запуск ВСЕХ тестов расширения
  const runAllExtensionTests = () => {
    setIsRunningGlobal(true);
    setLastExecutedInfo('Запущены ВСЕ тесты расширения СП_Тестирование (сквозной прогон тестов 1С)');

    setModules(prev => prev.map(m => ({
      ...m,
      tests: m.tests.map(t => ({ ...t, status: 'running' as const }))
    })));

    setTimeout(() => {
      setModules(prev => prev.map(m => ({
        ...m,
        tests: m.tests.map(t => ({ ...t, status: 'passed' as const, durationMs: Math.round(80 + Math.random() * 200) }))
      })));
      setIsRunningGlobal(false);
      setLastExecutedInfo(`Все тесты расширения (${totalTests} шт) успешно выполнены! (100% PASSED)`);
    }, 1800);
  };

  const filteredModules = modules.map(m => {
    const matchesCategory = filterCategory === 'all' || m.category === filterCategory;
    if (!matchesCategory) return null;
    const matchingTests = m.tests.filter(t => 
      t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.description.toLowerCase().includes(searchQuery.toLowerCase())
    );
    if (searchQuery && matchingTests.length === 0) return null;
    return {
      ...m,
      tests: searchQuery ? matchingTests : m.tests
    };
  }).filter(Boolean) as ExtensionModuleItem[];

  return (
    <div className={`rounded-xl border flex flex-col overflow-hidden shadow-xs ${
      theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-[#d0d7de]'
    }`}>
      {/* View Header */}
      <div className={`px-4 py-3 border-b flex flex-wrap items-center justify-between gap-3 text-xs ${
        theme === 'dark' ? 'bg-[#2d2d2d] border-[#3c3c3c] text-slate-200' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-800'
      }`}>
        <div className="flex items-center gap-2.5">
          <img 
            src="/icons/specter-tests.png" 
            alt="Specter Extension Tests" 
            className="w-4 h-4 shrink-0 rounded-xs shadow-xs" 
          />
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold">Панель «Тесты расширения (СП)»</span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 dark:bg-indigo-950/80 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
                Новая панель EDT
              </span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 font-normal">
              Все тестовые наборы и сценарии, найденные в расширении СП_Тестирование
            </p>
          </div>
        </div>

        {/* Global Action Buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={runAllExtensionTests}
            disabled={isRunningGlobal}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold text-white shadow-xs transition-all ${
              isRunningGlobal 
                ? 'bg-amber-600 animate-pulse cursor-wait' 
                : 'bg-emerald-600 hover:bg-emerald-700 active:scale-95'
            }`}
          >
            <Play className={`w-3.5 h-3.5 fill-current ${isRunningGlobal ? 'animate-spin' : ''}`} />
            <span>{isRunningGlobal ? 'Выполняется прогон...' : 'Запустить ВСЕ тесты'}</span>
          </button>
          
          <button
            onClick={() => {
              setModules(INITIAL_EXTENSION_MODULES);
              setLastExecutedInfo('Список тестов обновлен из метаданных расширения');
            }}
            title="Обновить список тестов расширения"
            className={`p-1.5 rounded-lg border text-slate-600 dark:text-slate-300 hover:bg-black/5 dark:hover:bg-white/5 transition-colors ${
              theme === 'dark' ? 'border-[#3c3c3c]' : 'border-[#d0d7de]'
            }`}
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Toolbar & Filter Bar */}
      <div className={`p-3 border-b space-y-2.5 ${
        theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafbfc] border-[#e1e4e8]'
      }`}>
        {/* Status bar */}
        <div className="flex items-center justify-between text-xs flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <span className="font-bold flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
              <span>Тестов в расширении: {totalTests}</span>
            </span>
            <span className="text-slate-400">|</span>
            <span className="text-[11px] text-emerald-600 font-semibold">{passedTests} пройдено</span>
            {failedTests > 0 && (
              <span className="text-[11px] text-red-600 font-semibold">{failedTests} упало</span>
            )}
          </div>

          {lastExecutedInfo && (
            <span className="text-[11px] font-mono text-indigo-600 dark:text-indigo-400 truncate max-w-md bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded border border-indigo-200 dark:border-indigo-800">
              {lastExecutedInfo}
            </span>
          )}
        </div>

        {/* Search & Category Filter */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-2">
          <div className={`flex-1 flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c] text-slate-200' : 'bg-white border-[#d0d7de] text-slate-700'
          }`}>
            <Search className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <input 
              type="text"
              placeholder="Поиск по названию или описанию теста (например, Контрагент, Заказ, КПП)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-transparent border-none outline-hidden text-xs w-full font-mono placeholder:text-slate-400"
            />
          </div>

          <div className="flex items-center gap-1 shrink-0 overflow-x-auto">
            {['all', 'UI & Vanessa', 'Управление формами', 'Ассерт-движок', 'Клиент-серверный обмен'].map(cat => (
              <button
                key={cat}
                onClick={() => setFilterCategory(cat)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold whitespace-nowrap transition-colors ${
                  filterCategory === cat
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-200/60 dark:hover:bg-slate-800'
                }`}
              >
                {cat === 'all' ? 'Все категории' : cat}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Modules and Tests Tree */}
      <div className="p-3 space-y-3 overflow-y-auto max-h-[520px]">
        {filteredModules.length === 0 ? (
          <div className="text-center py-10 text-slate-400">
            <Search className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">По запросу «{searchQuery}» тестов не найдено</p>
          </div>
        ) : (
          filteredModules.map(module => {
            const isExpanded = expandedModules[module.id] ?? true;
            return (
              <div 
                key={module.id}
                className={`rounded-xl border transition-all ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#333]' : 'bg-slate-50/70 border-slate-200'
                }`}
              >
                {/* Module Header */}
                <div 
                  onClick={() => toggleModuleExpand(module.id)}
                  className={`p-3 flex items-center justify-between cursor-pointer select-none rounded-t-xl transition-colors ${
                    theme === 'dark' ? 'hover:bg-[#282828]' : 'hover:bg-slate-100/90'
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <button className="text-slate-400 hover:text-slate-600">
                      {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                    </button>
                    <Folder className="w-4 h-4 text-indigo-500 shrink-0" />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-xs font-bold text-slate-800 dark:text-slate-200">
                          {module.moduleName}
                        </span>
                        <span className="px-1.5 py-0.2 rounded text-[10px] font-semibold bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                          {module.category}
                        </span>
                        <span className="text-[10px] text-slate-400">
                          ({module.tests.length} тестов)
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                        {module.description}
                      </p>
                    </div>
                  </div>

                  {/* Module Action: Run Module */}
                  <div className="flex items-center gap-1.5 shrink-0 ml-2">
                    <button
                      onClick={(e) => runModule(module.id, e)}
                      title={`Запустить все ${module.tests.length} тестов модуля`}
                      className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 text-indigo-700 dark:text-indigo-300 text-[11px] font-bold border border-indigo-200 dark:border-indigo-800 transition-colors"
                    >
                      <Play className="w-3 h-3 fill-current" />
                      <span>Запустить модуль</span>
                    </button>
                  </div>
                </div>

                {/* Module Tests List */}
                {isExpanded && (
                  <div className="p-2 pt-0 space-y-1.5 border-t border-slate-200/60 dark:border-slate-800">
                    {module.tests.map(test => {
                      const isSelected = selectedTestId === test.id;
                      const isRunning = test.status === 'running';

                      return (
                        <div
                          key={test.id}
                          onClick={() => {
                            setSelectedTestId(test.id);
                            if (onSelectTestForDetails) {
                              onSelectTestForDetails(test.name, module.moduleName);
                            }
                          }}
                          className={`p-2.5 rounded-lg border flex items-center justify-between gap-3 cursor-pointer transition-all ${
                            isSelected
                              ? theme === 'dark'
                                ? 'bg-indigo-950/40 border-indigo-500/60 shadow-xs'
                                : 'bg-white border-indigo-300 shadow-xs ring-1 ring-indigo-200'
                              : theme === 'dark'
                                ? 'bg-[#252526] border-[#383838] hover:bg-[#2d2d2d]'
                                : 'bg-white border-slate-200/90 hover:bg-slate-100/80'
                          }`}
                        >
                          <div className="flex items-start gap-2.5 min-w-0">
                            <div className="mt-0.5 shrink-0">
                              {isRunning ? (
                                <div className="w-4 h-4 rounded-full border-2 border-amber-500 border-t-transparent animate-spin"></div>
                              ) : test.status === 'passed' ? (
                                <div className="p-0.5 rounded-md bg-emerald-100 dark:bg-emerald-950/80 text-emerald-600">
                                  <CheckCircle2 className="w-3.5 h-3.5" />
                                </div>
                              ) : test.status === 'failed' ? (
                                <div className="p-0.5 rounded-md bg-red-100 dark:bg-red-950/80 text-red-600">
                                  <XCircle className="w-3.5 h-3.5" />
                                </div>
                              ) : (
                                <div className="w-4 h-4 rounded-full border border-slate-300 dark:border-slate-600 flex items-center justify-center">
                                  <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                                </div>
                              )}
                            </div>

                            <div className="min-w-0">
                              <div className="flex items-center gap-2">
                                <span className={`text-xs font-mono truncate ${
                                  isSelected ? 'font-bold text-indigo-900 dark:text-indigo-200' : 'text-slate-800 dark:text-slate-200'
                                }`}>
                                  🔬 {test.name}
                                </span>
                                {isRunning && (
                                  <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-amber-100 text-amber-800 animate-pulse">
                                    ВЫПОЛНЯЕТСЯ...
                                  </span>
                                )}
                              </div>
                              <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate mt-0.5">
                                {test.description}
                              </p>
                            </div>
                          </div>

                          {/* Right Side: Duration & Run Button */}
                          <div className="flex items-center gap-2 shrink-0">
                            <span className="font-mono text-[10px] text-slate-400 flex items-center gap-1">
                              <Clock className="w-3 h-3 text-slate-400" />
                              {test.durationMs}ms
                            </span>

                            <button
                              onClick={(e) => runSingleTest(module.id, test.id, e)}
                              disabled={isRunning}
                              title="Запустить этот тест отдельно"
                              className={`p-1.5 rounded-lg border transition-all flex items-center gap-1 text-[11px] font-bold ${
                                isRunning
                                  ? 'bg-amber-100 text-amber-800 border-amber-300'
                                  : 'bg-slate-100 dark:bg-slate-800 hover:bg-emerald-600 hover:text-white text-slate-700 dark:text-slate-200 border-slate-300 dark:border-slate-700'
                              }`}
                            >
                              <Play className="w-3 h-3 fill-current" />
                              <span className="hidden sm:inline">Запуск</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Footer Info */}
      <div className={`p-3 border-t text-[11px] flex items-center justify-between ${
        theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-400' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-600'
      }`}>
        <span className="flex items-center gap-1.5">
          <Zap className="w-3.5 h-3.5 text-amber-500" />
          <span>Прямой запуск: EDT формирует runId и передает команды сценария в Тонкий клиент 1С</span>
        </span>

        <span className="font-mono text-[10px] opacity-75">
          ru.ozon.uitp.e2e.views.ExtensionTestsView
        </span>
      </div>
    </div>
  );
};
