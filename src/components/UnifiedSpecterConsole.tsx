import React, { useState } from 'react';
import { 
  Play, 
  RotateCcw, 
  Clock, 
  CheckCircle2, 
  XCircle, 
  Copy, 
  Check, 
  Layers, 
  Search, 
  Zap, 
  Terminal, 
  FileCode, 
  Sparkles, 
  SplitSquareVertical, 
  Layout, 
  Info,
  ChevronRight,
  Maximize2,
  Trash2,
  Folder,
  Tag
} from 'lucide-react';

interface Props {
  theme?: 'dark' | 'light';
  designVersion?: 'improved' | 'legacy';
}

export interface StepItem {
  id: number;
  action: string;
  target: string;
  category: 'form' | 'input' | 'button' | 'assert' | 'system';
  duration: number;
  status: 'passed' | 'failed';
  detail?: string;
}

export interface TestCaseProtocol {
  id: string;
  name: string;
  moduleName: string;
  category: string;
  status: 'passed' | 'failed' | 'running';
  durationMs: number;
  runId: string;
  executedAt: string;
  summary: string;
  steps: StepItem[];
}

const INITIAL_TEST_PROTOCOLS: Record<string, TestCaseProtocol> = {
  't-client-1': {
    id: 't-client-1',
    name: 'Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии',
    moduleName: 'СП_ТестыКлиентскихСценариев',
    category: 'UI & Управляемые формы',
    status: 'passed',
    durationMs: 380,
    runId: 'run_984a1f',
    executedAt: '12:41:05',
    summary: 'Проверка полей карточки контрагента, ИНН, КПП и доступности командных кнопок сохранения',
    steps: [
      { id: 1, action: 'ОткрытьФормуСписка', target: 'Справочник.Контрагенты', category: 'form', duration: 110, status: 'passed' },
      { id: 2, action: 'АктивизироватьСтроку', target: 'ИНН "7701234567"', category: 'input', duration: 45, status: 'passed' },
      { id: 3, action: 'ОткрытьКарточку', target: 'ООО "Вектор"', category: 'form', duration: 120, status: 'passed' },
      { id: 4, action: 'ПроверитьЗначение', target: 'Статус = "Действующий"', category: 'assert', duration: 35, status: 'passed', detail: 'Проверка реквизита: Статус == Действующий (OK)' },
      { id: 5, action: 'ПроверитьДоступностьКоманд', target: 'Кнопка "ЗаписатьИЗакрыть" активна', category: 'button', duration: 70, status: 'passed', detail: 'Команда сохранения доступна для пользователя с полными правами' }
    ]
  },
  't-client-2': {
    id: 't-client-2',
    name: 'Тест_ПроведениеЗаказаПокупателя_РасчетСуммы',
    moduleName: 'СП_ТестыКлиентскихСценариев',
    category: 'UI & Управляемые формы',
    status: 'passed',
    durationMs: 640,
    runId: 'run_a319bf',
    executedAt: '12:41:08',
    summary: 'Интерактивное заполнение заказа: добавление товаров в ТЧ, авторасчёт скидки и проведение документа',
    steps: [
      { id: 1, action: 'ОткрытьФормуНового', target: 'Документ.ЗаказПокупателя', category: 'form', duration: 130, status: 'passed' },
      { id: 2, action: 'ЗаполнитьРеквизит', target: 'Контрагент = "ООО Вектор"', category: 'input', duration: 85, status: 'passed' },
      { id: 3, action: 'ДобавитьСтрокуТЧ', target: 'ТЧ "Товары": Номенклатура="Сервер 1U", Кол=2, Цена=150 000', category: 'input', duration: 160, status: 'passed' },
      { id: 4, action: 'ПроверитьЗначение', target: 'СуммаДокумента = 300 000.00 руб.', category: 'assert', duration: 65, status: 'passed', detail: 'Формула пересчёта ТЧ: 2 шт * 150 000.00 = 300 000.00 руб. (OK)' },
      { id: 5, action: 'НажатьКоманду', target: 'Команда "ПровестиИЗакрыть"', category: 'button', duration: 200, status: 'passed', detail: 'Движения сформированы в регистре накопления "Продажи" без ошибок' }
    ]
  },
  't-client-3': {
    id: 't-client-3',
    name: 'Тест_ВалидацияНекорректногоКПП_ВыводОшибки',
    moduleName: 'СП_ТестыКлиентскихСценариев',
    category: 'UI & Управляемые формы',
    status: 'passed',
    durationMs: 210,
    runId: 'run_c741dd',
    executedAt: '12:41:10',
    summary: 'Ввод невалидного значения КПП (5 цифр вместо 9) и проверка штатного механизма валидации 1С',
    steps: [
      { id: 1, action: 'ОткрытьФормуНового', target: 'Справочник.Контрагенты', category: 'form', duration: 70, status: 'passed' },
      { id: 2, action: 'УстановитьРеквизит', target: 'Поле "КПП" = "12345" (некорректная длина)', category: 'input', duration: 50, status: 'passed' },
      { id: 3, action: 'ВыполнитьПроверкуФормы', target: 'Перехват ошибки валидации', category: 'assert', duration: 90, status: 'passed', detail: 'Ожидаемое исключение платформы 1С: "КПП организации должен содержать ровно 9 цифр" перехвачено корректно' }
    ]
  },
  't-form-1': {
    id: 't-form-1',
    name: 'Тест_ОткрытиеКарточки_ПроверкаЭлементов',
    moduleName: 'СП_ТестыФорм',
    category: 'Управление формами',
    status: 'passed',
    durationMs: 290,
    runId: 'run_e8822c',
    executedAt: '12:41:12',
    summary: 'Проверка доступности и видимости реквизитов формы номенклатуры при включённом признаке маркировки',
    steps: [
      { id: 1, action: 'ОткрытьФормуЭлемента', target: 'Справочник.Номенклатура ("Сервер 1U")', category: 'form', duration: 110, status: 'passed' },
      { id: 2, action: 'ПроверитьВидимость', target: 'Элементы.ГруппаМаркировка.Видимость == Истина', category: 'assert', duration: 40, status: 'passed', detail: 'Группа маркировки отображается согласно типу товара' },
      { id: 3, action: 'ПроверитьДоступность', target: 'Элементы.ЦенаЗакупки.Доступность == Истина', category: 'assert', duration: 50, status: 'passed' },
      { id: 4, action: 'ЗакрытьФорму', target: 'Форма.Закрыть() без сохранения', category: 'button', duration: 90, status: 'passed' }
    ]
  },
  't-form-2': {
    id: 't-form-2',
    name: 'Тест_ЗаполнениеПолей_БлокировкаКнопок',
    moduleName: 'СП_ТестыФорм',
    category: 'Управление формами',
    status: 'passed',
    durationMs: 310,
    runId: 'run_f1903e',
    executedAt: '12:41:15',
    summary: 'Тестирование автоблокировки команд печати и выгрузки при наличии несохраненных изменений',
    steps: [
      { id: 1, action: 'ОткрытьФормуНового', target: 'Документ.ПоступлениеТоваровУслуг', category: 'form', duration: 120, status: 'passed' },
      { id: 2, action: 'УстановитьМодифицированность', target: 'Модифицированность == Истина', category: 'input', duration: 30, status: 'passed' },
      { id: 3, action: 'ПроверитьБлокировкуКнопок', target: 'Кнопка "Печать" заблокирована до сохранения', category: 'assert', duration: 70, status: 'passed', detail: 'Печать запрещена для несохраненного объекта' },
      { id: 4, action: 'ОткатитьИзменения', target: 'ЗакрытьБезСохранения()', category: 'button', duration: 90, status: 'passed' }
    ]
  },
  't-assert-1': {
    id: 't-assert-1',
    name: 'Тест_АссертРавно_ЧислаИСтроки',
    moduleName: 'СП_ТестыУтверждений',
    category: 'Ассерт-движок',
    status: 'passed',
    durationMs: 45,
    runId: 'run_10b48a',
    executedAt: '12:41:18',
    summary: 'Проверка модуля СП_Утверждения на сравнение чисел с плавающей точкой и строк в кодировке UTF-8',
    steps: [
      { id: 1, action: 'ИнициализироватьКонтекст', target: 'СП_Утверждения.СоздатьКонтекст()', category: 'system', duration: 10, status: 'passed' },
      { id: 2, action: 'ПроверитьРавенствоЧисел', target: 'СП_Утверждения.ПроверитьРавно(100.5, 100.5)', category: 'assert', duration: 18, status: 'passed', detail: 'Числа с плавающей точкой эквивалентны' },
      { id: 3, action: 'ПроверитьРавенствоСтрок', target: 'СП_Утверждения.ПроверитьРавно("Озон", "Озон")', category: 'assert', duration: 17, status: 'passed', detail: 'Строки UTF-8 идентичны побайтово' }
    ]
  },
  't-bridge-1': {
    id: 't-bridge-1',
    name: 'Тест_СквознойЗапуск_Антимаскировка',
    moduleName: 'СП_ТестыИнтеграции',
    category: 'Клиент-серверный обмен',
    status: 'passed',
    durationMs: 520,
    runId: 'run_84f91e',
    executedAt: '12:41:20',
    summary: 'Сквозной прогон сценария: генерация runId в EDT, передача в Тонкий клиент 1С и валидация схемы JSON',
    steps: [
      { id: 1, action: 'ГенерацияRunId', target: 'runId = "run_84f91e"', category: 'system', duration: 15, status: 'passed' },
      { id: 2, action: 'ФормированиеКомандногоФайла', target: 'Запись outDir/cmd_run_84f91e.json', category: 'system', duration: 65, status: 'passed' },
      { id: 3, action: 'ИсполнениеВТонкомКлиенте1С', target: 'СП_ОбработчикКомандКлиент.Выполнить()', category: 'form', duration: 290, status: 'passed', detail: 'Все 5 UI-шагов выполнены в активном сеансе 1С' },
      { id: 4, action: 'КонтрольАнтимаскировки', target: 'Запрет скрытия исключений через пустой Попытка-Исключение', category: 'assert', duration: 45, status: 'passed', detail: 'Схема JSON валидна, исключения платформы не маскируются' },
      { id: 5, action: 'ЧтениеРезультатаИзШины', target: 'Чтение outDir/result_run_84f91e.json', category: 'system', duration: 65, status: 'passed' },
      { id: 6, action: 'ОчиститьВременныеФайлы', target: 'Удаление временных файлов сценария', category: 'system', duration: 40, status: 'passed' }
    ]
  }
};

export const UnifiedSpecterConsole: React.FC<Props> = ({ 
  theme = 'light',
  designVersion = 'improved' 
}) => {
  // Состояние: есть ли отчёты или отображается Empty State
  const [hasReports, setHasReports] = useState<boolean>(true);
  const [activeLayout, setActiveLayout] = useState<'split' | 'tree-only' | 'results-only'>('split');
  const [isRunningTest, setIsRunningTest] = useState(false);
  const [protocols, setProtocols] = useState<Record<string, TestCaseProtocol>>(INITIAL_TEST_PROTOCOLS);
  const [selectedTestId, setSelectedTestId] = useState<string>('t-client-1');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedLog, setCopiedLog] = useState(false);

  const currentTest = protocols[selectedTestId] || protocols['t-client-1'];

  // Запуск E2E-сценария (R1) или текущего теста
  const handleRunCurrentTest = (testIdToRun?: string) => {
    const targetId = testIdToRun || selectedTestId;
    setIsRunningTest(true);

    // Устанавливаем статус running
    setProtocols(prev => ({
      ...prev,
      [targetId]: {
        ...prev[targetId],
        status: 'running'
      }
    }));

    setTimeout(() => {
      setIsRunningTest(false);
      setHasReports(true);
      setProtocols(prev => {
        const item = prev[targetId];
        const newDuration = Math.round(item.durationMs * (0.9 + Math.random() * 0.25));
        const now = new Date();
        const timeStr = now.toTimeString().split(' ')[0];
        return {
          ...prev,
          [targetId]: {
            ...item,
            status: 'passed',
            durationMs: newDuration,
            executedAt: timeStr,
            runId: `run_${Math.random().toString(16).substring(2, 8)}`
          }
        };
      });
    }, 900);
  };

  const handleClearReports = () => {
    setHasReports(false);
  };

  const handleCopyLog = () => {
    setCopiedLog(true);
    setTimeout(() => setCopiedLog(false), 2000);
  };

  const allTestsList = Object.values(protocols);
  const filteredTests = allTestsList.filter(t => 
    t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.moduleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-3">
      {/* State Switcher Bar for Showcase */}
      <div className={`p-3 rounded-xl border flex flex-wrap items-center justify-between gap-3 text-xs ${
        theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-slate-200'
      }`}>
        <div className="flex items-center gap-2">
          <span className="font-bold text-slate-700 dark:text-slate-300">Состояние консоли:</span>
          <div className="flex items-center bg-slate-100 dark:bg-slate-800 p-0.5 rounded-lg">
            <button
              onClick={() => setHasReports(true)}
              className={`px-3 py-1 rounded-md font-bold transition-all ${
                hasReports 
                  ? 'bg-emerald-600 text-white shadow-xs' 
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Отчёты загружены ({allTestsList.length}/{allTestsList.length} ✓)
            </button>
            <button
              onClick={() => setHasReports(false)}
              className={`px-3 py-1 rounded-md font-bold transition-all ${
                !hasReports 
                  ? 'bg-indigo-600 text-white shadow-xs' 
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Отчёты не найдены (Empty State с эмблемой)
            </button>
          </div>
        </div>

        {/* Layout split modes */}
        <div className="flex items-center gap-1.5">
          <span className="text-[11px] text-slate-500">Вид сплиттера:</span>
          <button
            onClick={() => setActiveLayout('split')}
            className={`px-2 py-1 rounded text-[11px] font-bold border transition-colors ${
              activeLayout === 'split' 
                ? 'bg-slate-900 text-white border-slate-900 dark:bg-slate-100 dark:text-slate-900' 
                : 'border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-400'
            }`}
          >
            Сплит 45/55
          </button>
          <button
            onClick={() => setActiveLayout('tree-only')}
            className={`px-2 py-1 rounded text-[11px] font-bold border transition-colors ${
              activeLayout === 'tree-only' 
                ? 'bg-slate-900 text-white border-slate-900 dark:bg-slate-100 dark:text-slate-900' 
                : 'border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-400'
            }`}
          >
            Только тесты
          </button>
          <button
            onClick={() => setActiveLayout('results-only')}
            className={`px-2 py-1 rounded text-[11px] font-bold border transition-colors ${
              activeLayout === 'results-only' 
                ? 'bg-slate-900 text-white border-slate-900 dark:bg-slate-100 dark:text-slate-900' 
                : 'border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-400'
            }`}
          >
            Только протокол
          </button>
        </div>
      </div>

      {/* Main Unified View Component */}
      <div className={`rounded-xl border flex flex-col overflow-hidden shadow-xs ${
        theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-[#d0d7de]'
      }`}>
        {/* View Header: Specter: Тесты и Результаты */}
        <div className={`px-4 py-2.5 border-b flex flex-wrap items-center justify-between gap-2 text-xs font-bold ${
          theme === 'dark' ? 'bg-[#2d2d2d] border-[#3c3c3c] text-slate-200' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-800'
        }`}>
          <div className="flex items-center gap-2">
            <img 
              src="/icons/specter-tests.png" 
              alt="Specter" 
              className="w-4 h-4 shrink-0 rounded-xs shadow-xs" 
            />
            <span>Specter: Тесты и Результаты (All-in-One Console)</span>
            <span className="px-2 py-0.2 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 dark:bg-indigo-950/80 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
              Объединённый ViewPart
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => handleRunCurrentTest(selectedTestId)}
              disabled={isRunningTest}
              className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-[11px] font-bold text-white transition-all ${
                isRunningTest ? 'bg-amber-600 animate-pulse' : 'bg-indigo-600 hover:bg-indigo-700 shadow-xs active:scale-95'
              }`}
            >
              <Play className={`w-3 h-3 fill-current ${isRunningTest ? 'animate-spin' : ''}`} />
              <span>{isRunningTest ? 'Выполняется сценарий...' : 'Запустить E2E-сценарий (R1)'}</span>
            </button>

            {hasReports && (
              <button
                onClick={handleClearReports}
                title="Очистить результаты для просмотра экрана без отчётов"
                className={`p-1 rounded hover:bg-black/10 transition-colors ${
                  theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-slate-600'
                }`}
              >
                <Trash2 className="w-3.5 h-3.5 text-slate-400" />
              </button>
            )}

            <span className="font-mono text-[10px] opacity-60">ru.ozon.uitp.e2e.views.TestsView</span>
          </div>
        </div>

        {/* Console Body: Split / Empty State */}
        {!hasReports ? (
          /* ========================================================================= */
          /* EMPTY STATE COMPARISON (Пользовательский запрос про надпись Отчёты не найдены) */
          /* ========================================================================= */
          designVersion === 'legacy' ? (
            /* --- СТАРЫЙ ВИД (До) --- */
            <div className="p-8 font-mono text-xs flex flex-col items-center justify-center min-h-[380px] bg-slate-100 dark:bg-[#1a1a1a] text-slate-500 space-y-2">
              <div className="text-red-600 dark:text-red-400 font-bold text-sm">
                Отчёты не найдены
              </div>
              <div className="text-[11px] text-slate-400">
                (Старая плоская надпись без действий, без фирменного логотипа и без контекста)
              </div>
            </div>
          ) : (
            /* --- КАСТОМИЗИРОВАННЫЙ ВИД SPECTER (После) С ЭМБЛЕМОЙ НА ФОНЕ --- */
            <div className="relative p-8 flex flex-col items-center justify-center min-h-[420px] overflow-hidden select-none">
              {/* Background Specter Watermark Emblem */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-[0.07] dark:opacity-[0.12] transition-opacity">
                <img 
                  src="/icons/specter-emblem.svg" 
                  alt="Specter Watermark Emblem"
                  className="w-80 h-80 object-contain filter drop-shadow-md"
                />
              </div>

              {/* Foreground Content Card */}
              <div className="relative z-10 max-w-lg w-full flex flex-col items-center text-center space-y-4">
                {/* Glowing Emblem Icon Badge */}
                <div className="relative p-4 rounded-3xl bg-linear-to-b from-indigo-500/10 to-indigo-500/5 dark:from-indigo-500/20 dark:to-cyan-500/10 border border-indigo-200/80 dark:border-indigo-500/30 shadow-sm">
                  <img 
                    src="/icons/specter-emblem.svg" 
                    alt="Specter Emblem" 
                    className="w-16 h-16 object-contain" 
                  />
                  <div className="absolute -bottom-1 -right-1 p-1 rounded-full bg-emerald-500 text-white border-2 border-white dark:border-[#252526]">
                    <Sparkles className="w-3.5 h-3.5" />
                  </div>
                </div>

                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
                    Specter E2E Testing Suite
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-md leading-relaxed">
                    Отчёты ещё не сформированы. Запустите тестовый сценарий в Тонком клиенте 1С или откройте BSL-модуль набора в редакторе.
                  </p>
                </div>

                {/* Quick Action Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 w-full pt-2">
                  <button
                    onClick={() => handleRunCurrentTest('t-client-1')}
                    className="p-3 rounded-xl border border-indigo-200 dark:border-indigo-800/80 bg-white dark:bg-[#1e1e1e] hover:border-indigo-500 hover:shadow-xs transition-all text-left flex items-start gap-2.5 group"
                  >
                    <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 group-hover:bg-indigo-600 group-hover:text-white transition-colors shrink-0">
                      <Play className="w-4 h-4 fill-current" />
                    </div>
                    <div>
                      <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block">
                        Запустить сценарий (R1)
                      </span>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 block mt-0.5">
                        Канонический UI-сценарий карточки в Тонком клиенте 1С
                      </span>
                    </div>
                  </button>

                  <button
                    onClick={() => setHasReports(true)}
                    className="p-3 rounded-xl border border-slate-200 dark:border-[#383838] bg-white dark:bg-[#1e1e1e] hover:border-slate-400 hover:shadow-xs transition-all text-left flex items-start gap-2.5 group"
                  >
                    <div className="p-2 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 group-hover:bg-slate-800 group-hover:text-white transition-colors shrink-0">
                      <RotateCcw className="w-4 h-4" />
                    </div>
                    <div>
                      <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block">
                        Прочитать из outDir
                      </span>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 block mt-0.5">
                        Перезагрузить JSON-отчёт из каталога обмена
                      </span>
                    </div>
                  </button>
                </div>

                <div className="pt-2 text-[11px] text-slate-400 flex items-center gap-1.5">
                  <Info className="w-3.5 h-3.5 text-slate-400" />
                  <span>В Eclipse SWT реализован кастомный PaintListener с водяным знаком эмблемы Specter</span>
                </div>
              </div>
            </div>
          )
        ) : (
          /* ========================================================================= */
          /* ACTIVE RUN: ОБЪЕДИНЁННАЯ КОНСОЛЬ (Слева список тестов, Справа протокол) */
          /* ========================================================================= */
          <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[440px]">
            {/* LEFT PANE: Дерево тестов (45%) */}
            {(activeLayout === 'split' || activeLayout === 'tree-only') && (
              <div className={`${
                activeLayout === 'split' ? 'lg:col-span-5 border-r' : 'lg:col-span-12'
              } flex flex-col ${theme === 'dark' ? 'border-[#3c3c3c]' : 'border-[#d0d7de]'}`}>
                {/* Search & Filter in tests tree */}
                <div className={`p-2.5 border-b space-y-2 ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafbfc] border-[#e1e4e8]'
                }`}>
                  <div className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-1.5 font-bold text-slate-800 dark:text-slate-200">
                      <Layers className="w-3.5 h-3.5 text-indigo-500" />
                      <span>Тестовые сценарии 1С</span>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 dark:bg-emerald-950/80 text-emerald-800 dark:text-emerald-300">
                      {filteredTests.length} сценариев
                    </span>
                  </div>

                  <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md border text-xs ${
                    theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c] text-slate-200' : 'bg-white border-[#d0d7de] text-slate-700'
                  }`}>
                    <Search className="w-3 h-3 text-slate-400 shrink-0" />
                    <input
                      type="text"
                      placeholder="Поиск теста..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="bg-transparent border-none outline-hidden text-[11px] w-full font-mono placeholder:text-slate-400"
                    />
                  </div>
                </div>

                {/* Tree Items: Нажмите на любой тест, чтобы мгновенно сменить протокол справа */}
                <div className="p-2 space-y-1.5 flex-1 overflow-y-auto max-h-[460px]">
                  {filteredTests.map(test => {
                    const isSelected = selectedTestId === test.id;
                    const isRunning = test.status === 'running';

                    return (
                      <div
                        key={test.id}
                        onClick={() => setSelectedTestId(test.id)}
                        className={`p-2.5 rounded-lg cursor-pointer transition-all border flex items-center justify-between gap-2.5 ${
                          isSelected
                            ? theme === 'dark'
                              ? 'bg-indigo-950/50 border-indigo-500 text-white shadow-xs ring-1 ring-indigo-500/40'
                              : 'bg-indigo-50/90 border-indigo-300 text-indigo-950 font-semibold shadow-xs ring-1 ring-indigo-200'
                            : theme === 'dark'
                              ? 'border-transparent hover:bg-[#2e2e2e] text-slate-300'
                              : 'border-transparent hover:bg-slate-100 text-slate-700'
                        }`}
                      >
                        <div className="flex items-start gap-2 min-w-0">
                          {isRunning ? (
                            <div className="w-3.5 h-3.5 mt-0.5 rounded-full border-2 border-amber-500 border-t-transparent animate-spin shrink-0"></div>
                          ) : test.status === 'passed' ? (
                            <CheckCircle2 className="w-3.5 h-3.5 mt-0.5 text-emerald-500 shrink-0" />
                          ) : (
                            <XCircle className="w-3.5 h-3.5 mt-0.5 text-red-500 shrink-0" />
                          )}
                          
                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-mono truncate">{test.name}</span>
                            </div>
                            <div className="flex items-center gap-2 mt-0.5 text-[10px] opacity-75 font-normal">
                              <span>{test.moduleName}</span>
                              <span>•</span>
                              <span>{test.steps.length} шагов</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex flex-col items-end shrink-0">
                          <span className="font-mono text-[10px] text-slate-400">{test.durationMs}ms</span>
                          <span className={`text-[9px] font-bold ${
                            test.status === 'passed' ? 'text-emerald-600' : isRunning ? 'text-amber-500' : 'text-red-500'
                          }`}>
                            {isRunning ? 'RUNNING' : test.status.toUpperCase()}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div className={`p-2 border-t text-[11px] flex items-center justify-between ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-400' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-600'
                }`}>
                  <span className="text-[10px]">Клик переключает протокол справа</span>
                  <button
                    onClick={() => handleRunCurrentTest(selectedTestId)}
                    disabled={isRunningTest}
                    className="px-2.5 py-1 rounded bg-indigo-600 hover:bg-indigo-700 text-white text-[10px] font-bold transition-all"
                  >
                    Запустить этот тест
                  </button>
                </div>
              </div>
            )}

            {/* RIGHT PANE: ДЕТАЛЬНЫЙ ПРОТОКОЛ РЕЗУЛЬТАТОВ (ДИНАМИЧЕСКИ ОБНОВЛЯЕТСЯ ПРИ ВЫБОРЕ ТЕСТА) */}
            {(activeLayout === 'split' || activeLayout === 'results-only') && (
              <div className={`${
                activeLayout === 'split' ? 'lg:col-span-7' : 'lg:col-span-12'
              } flex flex-col`}>
                {/* Result Header Bar */}
                <div className={`p-2.5 border-b flex flex-wrap items-center justify-between gap-2 text-xs ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafbfc] border-[#e1e4e8]'
                }`}>
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-bold text-slate-800 dark:text-slate-200">
                      Протокол выполнения:
                    </span>
                    <span className="font-mono text-[11px] text-indigo-600 dark:text-indigo-400 font-semibold truncate max-w-xs">
                      {currentTest.name}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 dark:bg-emerald-950/80 text-emerald-800 dark:text-emerald-300">
                      {currentTest.status.toUpperCase()} ({currentTest.steps.length}/{currentTest.steps.length} шагов)
                    </span>
                    <span className="font-mono text-[10px] text-slate-400">{currentTest.runId}</span>
                    <button
                      onClick={handleCopyLog}
                      className={`p-1 rounded hover:bg-black/10 transition-colors ${
                        theme === 'dark' ? 'text-slate-400 hover:text-white' : 'text-slate-600'
                      }`}
                      title="Скопировать лог протокола"
                    >
                      {copiedLog ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>

                {/* Test Meta Bar */}
                <div className={`px-3 py-2 border-b text-[11px] flex flex-wrap items-center justify-between gap-2 ${
                  theme === 'dark' ? 'bg-[#222] border-[#333] text-slate-300' : 'bg-slate-50 border-slate-200 text-slate-700'
                }`}>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="flex items-center gap-1">
                      <Folder className="w-3 h-3 text-indigo-500" />
                      <span className="font-mono font-semibold">{currentTest.moduleName}</span>
                    </span>
                    <span className="text-slate-400">•</span>
                    <span className="flex items-center gap-1">
                      <Tag className="w-3 h-3 text-slate-400" />
                      <span>{currentTest.category}</span>
                    </span>
                  </div>

                  <div className="flex items-center gap-2 font-mono text-[10px] text-slate-400">
                    <span>Запуск в: {currentTest.executedAt}</span>
                    <span>•</span>
                    <span className="text-slate-600 dark:text-slate-300 font-bold">{currentTest.durationMs}ms</span>
                  </div>
                </div>

                {/* Summary / Description */}
                <div className={`px-3 py-1.5 border-b text-[11px] ${
                  theme === 'dark' ? 'bg-[#1a1a1a] border-[#333] text-slate-400' : 'bg-white border-slate-200 text-slate-600'
                }`}>
                  <span className="font-semibold text-slate-500">Сценарий: </span>
                  <span>{currentTest.summary}</span>
                </div>

                {/* Steps Log for Selected Test */}
                <div className="p-3 space-y-2 flex-1 overflow-y-auto max-h-[380px] font-mono text-xs">
                  {currentTest.steps.map(step => (
                    <div 
                      key={step.id}
                      className={`p-2.5 rounded-lg border transition-all ${
                        theme === 'dark' 
                          ? 'bg-[#1e1e1e] border-[#333]' 
                          : 'bg-white border-slate-200 shadow-2xs'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2 min-w-0">
                          <span className="text-slate-400 font-bold shrink-0">#{step.id}</span>
                          <span className="font-bold text-indigo-600 dark:text-indigo-400 shrink-0">{step.action}</span>
                          <span className="text-slate-700 dark:text-slate-300 font-normal truncate">({step.target})</span>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[10px] text-slate-400">{step.duration}ms</span>
                          <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-emerald-100 dark:bg-emerald-900/80 text-emerald-800 dark:text-emerald-300">
                            PASSED
                          </span>
                        </div>
                      </div>

                      {step.detail && (
                        <div className="mt-1.5 pt-1.5 border-t border-slate-200/60 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400 font-sans">
                          {step.detail}
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {/* Status Bar */}
                <div className={`p-2.5 border-t text-[11px] flex items-center justify-between ${
                  theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-400' : 'bg-[#f6f8fa] border-[#d0d7de] text-slate-600'
                }`}>
                  <span className="text-emerald-600 dark:text-emerald-400 font-bold flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Все {currentTest.steps.length} шагов сценария выполнены успешно</span>
                  </span>
                  <span className="text-[10px] font-mono text-slate-400">
                    Общее время: {currentTest.durationMs}ms
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
