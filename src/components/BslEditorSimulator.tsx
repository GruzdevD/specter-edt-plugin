import React, { useState } from 'react';
import { 
  Play, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Terminal, 
  Sparkles, 
  Code2, 
  FileCode, 
  Check, 
  RotateCcw,
  Zap,
  Info,
  Layers,
  ChevronRight
} from 'lucide-react';

interface BslEditorSimulatorProps {
  theme: 'dark' | 'light';
}

interface BslTestMethod {
  name: string;
  line: number;
  description: string;
  steps: Array<{
    action: string;
    target: string;
    details: string;
    durationMs: number;
  }>;
}

const BSL_TESTS: BslTestMethod[] = [
  {
    name: 'Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии',
    line: 6,
    description: 'Открытие формы списка, поиск контрагента по ИНН и валидация статуса карточки',
    steps: [
      { action: 'СП_ОжиданияКлиент.ДождатьсяОткрытияОкна', target: 'Справочник.Контрагенты.ФормаСписка', details: 'Поиск окна формы списка контрагентов', durationMs: 95 },
      { action: 'СП_ДействияКлиент.НайтиСтроку', target: 'Таблица "Список", ИНН="7701234567"', details: 'Позиционирование на строку контрагента ООО Вектор', durationMs: 40 },
      { action: 'СП_ДействияКлиент.НажатьКнопку', target: 'Команда "Изменить"', details: 'Открытие формы элемента контрагента', durationMs: 110 },
      { action: 'СП_ДействияКлиент.ПолучитьЗначение', target: 'Реквизит "Статус"', details: 'Считывание текущего статуса карточки', durationMs: 25 },
      { action: 'СП_Утверждения.ПроверитьРавенство', target: 'Статус, "Действующий"', details: 'Утверждение истинности активного статуса', durationMs: 15 }
    ]
  },
  {
    name: 'Тест_ПроведениеЗаказаПокупателя_РасчетСуммы',
    line: 24,
    description: 'Создание заказа, добавление строк ТЧ Товары, проведение и проверка движений',
    steps: [
      { action: 'СП_ОжиданияКлиент.ДождатьсяОткрытияОкна', target: 'Документ.ЗаказПокупателя.ФормаОбъекта', details: 'Открытие новой формы заказа покупателя', durationMs: 130 },
      { action: 'СП_ДействияКлиент.УстановитьЗначение', target: 'Реквизит "Контрагент"', details: 'Установка значения "ООО Ромашка"', durationMs: 50 },
      { action: 'СП_ДействияКлиент.ДобавитьСтроку', target: 'ТабличнаяЧасть "Товары"', details: 'Добавление новой строки спецификации', durationMs: 65 },
      { action: 'СП_ДействияКлиент.УстановитьЗначениеЯчейки', target: 'Товары[0].Номенклатура', details: 'Установка товара "Кабель силовой ВВГнг"', durationMs: 45 },
      { action: 'СП_ДействияКлиент.УстановитьЗначениеЯчейки', target: 'Товары[0].Количество', details: 'Установка количества 10 шт.', durationMs: 30 },
      { action: 'СП_ДействияКлиент.НажатьКнопку', target: 'Команда "ФормаПровестиИЗакрыть"', details: 'Исполнение проведения документа', durationMs: 180 },
      { action: 'СП_ОжиданияКлиент.ДождатьсяЗакрытияОкна', target: 'Документ.ЗаказПокупателя.ФормаОбъекта', details: 'Проверка успешного закрытия после проведения', durationMs: 70 }
    ]
  },
  {
    name: 'Тест_ВалидацияНекорректногоКПП_ВыводОшибки',
    line: 48,
    description: 'Ввод некорректного 3-значного КПП и проверка появления сообщения пользователю',
    steps: [
      { action: 'СП_ОжиданияКлиент.ДождатьсяОткрытияОкна', target: 'Справочник.Контрагенты.Форма.ФормаЭлемента', details: 'Открытие карточки редактирования', durationMs: 85 },
      { action: 'СП_ДействияКлиент.УстановитьЗначение', target: 'ПолеВвода "КПП"', details: 'Ввод ошибочного значения "123"', durationMs: 35 },
      { action: 'СП_ДействияКлиент.НажатьКнопку', target: 'Команда "Записать"', details: 'Попытка записи невалидного объекта', durationMs: 90 },
      { action: 'СП_ОжиданияКлиент.ДождатьсяСообщенияПользователю', target: 'Сообщения', details: 'Перехват текста сообщения об ошибке', durationMs: 40 },
      { action: 'СП_Утверждения.ПроверитьСодержитТекст', target: 'Сообщение, "КПП должен содержать 9 цифр"', details: 'Проверка вывода подсказки пользователю', durationMs: 20 }
    ]
  }
];

export const BslEditorSimulator: React.FC<BslEditorSimulatorProps> = ({ theme }) => {
  const [activeTest, setActiveTest] = useState<BslTestMethod | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [currentStepIndex, setCurrentStepIndex] = useState<number>(-1);
  const [runLog, setRunLog] = useState<Array<{ text: string; time: string; ok: boolean }>>([]);
  const [hoveredLine, setHoveredLine] = useState<number | null>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; test: BslTestMethod } | null>(null);

  const startTest = (test: BslTestMethod) => {
    setActiveTest(test);
    setIsRunning(true);
    setCurrentStepIndex(0);
    setRunLog([
      { text: `[EDT Gutter] Запуск теста «${test.name}»...`, time: '00:00.000', ok: true },
      { text: `[Bridge] Генерация runId=${Math.random().toString(36).substring(2, 9)} • Модуль: СП_Тесты_ЗаказПокупателя`, time: '00:00.015', ok: true }
    ]);

    let step = 0;
    const interval = setInterval(() => {
      step++;
      if (step < test.steps.length) {
        setCurrentStepIndex(step);
        const s = test.steps[step];
        setRunLog(prev => [
          ...prev,
          { 
            text: `[Шаг ${step + 1}/${test.steps.length}] ${s.action}("${s.target}"): OK (${s.durationMs} мс)`, 
            time: `+${(step * 0.12).toFixed(3)}s`, 
            ok: true 
          }
        ]);
      } else {
        clearInterval(interval);
        setIsRunning(false);
        setRunLog(prev => [
          ...prev,
          { 
            text: `✔ [ИТОГ] Тест «${test.name}» завершён УСПЕШНО! Все утверждения истинны.`, 
            time: `ИТОГ`, 
            ok: true 
          }
        ]);
      }
    }, 450);
  };

  const codeLines = [
    { num: 1, text: '// @test-suite', comment: true },
    { num: 2, text: '// Модуль прикладного UI-тестирования в стиле Vanessa-Automation', comment: true },
    { num: 3, text: '// Движок: расширение СП_Тестирование • Плагин: Specter EDT', comment: true },
    { num: 4, text: '' },
    { num: 5, text: '&Тест', annotation: true },
    { num: 6, text: 'Процедура Тест_ПроверкаРеквизитовКонтрагента_ПриОткрытии() Экспорт', isTest: true, testIdx: 0 },
    { num: 7, text: '    // 1. Открытие списка и ожидание готовности формы' },
    { num: 8, text: '    СП_ОжиданияКлиент.ДождатьсяОткрытияОкна("Справочник.Контрагенты.ФормаСписка");' },
    { num: 9, text: '    ' },
    { num: 10, text: '    // 2. Поиск строки контрагента в динамическом списке' },
    { num: 11, text: '    СП_ДействияКлиент.НайтиСтроку("Список", "ИНН", "7701234567");' },
    { num: 12, text: '    СП_ДействияКлиент.НажатьКнопку("Изменить");' },
    { num: 13, text: '    ' },
    { num: 14, text: '    // 3. Проверка значения реквизита формы' },
    { num: 15, text: '    Статус = СП_ДействияКлиент.ПолучитьЗначение("Статус");' },
    { num: 16, text: '    СП_Утверждения.ПроверитьРавенство(Статус, "Действующий");' },
    { num: 17, text: '    ' },
    { num: 18, text: '    СП_ДействияКлиент.НажатьКнопку("Закрыть");' },
    { num: 19, text: 'КонецПроцедуры' },
    { num: 20, text: '' },
    { num: 21, text: '' },
    { num: 22, text: '// -------------------------------------------------------------------------' },
    { num: 23, text: '&Тест', annotation: true },
    { num: 24, text: 'Процедура Тест_ПроведениеЗаказаПокупателя_РасчетСуммы() Экспорт', isTest: true, testIdx: 1 },
    { num: 25, text: '    // Создание и заполнение шапки документа' },
    { num: 26, text: '    СП_ОжиданияКлиент.ДождатьсяОткрытияОкна("Документ.ЗаказПокупателя.ФормаОбъекта");' },
    { num: 27, text: '    СП_ДействияКлиент.УстановитьЗначение("Контрагент", "ООО Ромашка");' },
    { num: 28, text: '    ' },
    { num: 29, text: '    // Добавление строки спецификации в табличную часть' },
    { num: 30, text: '    СтрокаТЧ = СП_ДействияКлиент.ДобавитьСтроку("Товары");' },
    { num: 31, text: '    СП_ДействияКлиент.УстановитьЗначениеЯчейки("Товары", "Номенклатура", "Кабель силовой ВВГнг");' },
    { num: 32, text: '    СП_ДействияКлиент.УстановитьЗначениеЯчейки("Товары", "Количество", 10);' },
    { num: 33, text: '    ' },
    { num: 34, text: '    // Проведение и проверка закрытия' },
    { num: 35, text: '    СП_ДействияКлиент.НажатьКнопку("ФормаПровестиИЗакрыть");' },
    { num: 36, text: '    СП_ОжиданияКлиент.ДождатьсяЗакрытияОкна("Документ.ЗаказПокупателя.ФормаОбъекта");' },
    { num: 37, text: 'КонецПроцедуры' },
    { num: 38, text: '' },
    { num: 39, text: '' },
    { num: 40, text: '// -------------------------------------------------------------------------' },
    { num: 41, text: '&Тест', annotation: true },
    { num: 42, text: 'Процедура Тест_ВалидацияНекорректногоКПП_ВыводОшибки() Экспорт', isTest: true, testIdx: 2 },
    { num: 43, text: '    СП_ОжиданияКлиент.ДождатьсяОткрытияОкна("Справочник.Контрагенты.Форма.ФормаЭлемента");' },
    { num: 44, text: '    СП_ДействияКлиент.УстановитьЗначение("КПП", "123");' },
    { num: 45, text: '    СП_ДействияКлиент.НажатьКнопку("Записать");' },
    { num: 46, text: '    ' },
    { num: 47, text: '    // Проверка появления сообщения пользователю' },
    { num: 48, text: '    Сообщение = СП_ОжиданияКлиент.ДождатьсяСообщенияПользователю();' },
    { num: 49, text: '    СП_Утверждения.ПроверитьСодержитТекст(Сообщение, "КПП должен содержать 9 цифр");' },
    { num: 50, text: 'КонецПроцедуры' }
  ];

  return (
    <div className="flex flex-col space-y-4">
      {/* Feature Explainer Banner */}
      <div className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
        theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-slate-200 shadow-xs'
      }`}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-500 shrink-0">
            <Play className="w-5 h-5 fill-emerald-500" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="text-xs font-bold text-slate-900 dark:text-white">
                Запуск тестов прямо из окна редактора кода 1C:EDT (YAxUnit-паттерн)
              </h4>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-300">
                Gutter Marker Ready
              </span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Плагин Specter анализирует открытый BSL-модуль, находит объявления процедур тестов (<code className="font-mono text-emerald-600 dark:text-emerald-400">&Тест</code> или префикс <code className="font-mono text-emerald-600 dark:text-emerald-400">Тест_*</code>) и выставляет зелёную кнопку запуска на левом поле (Gutter).
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <span className="text-[11px] font-mono text-slate-500 dark:text-slate-400">
            Кликните <strong>▶</strong> на строке 6, 24 или 42
          </span>
        </div>
      </div>

      {/* Editor Simulator Box */}
      <div className={`rounded-xl border overflow-hidden shadow-md flex flex-col ${
        theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-[#fafafa] border-[#d0d7de]'
      }`}>
        {/* Editor Tabs Bar */}
        <div className={`px-2 pt-2 border-b flex items-center justify-between text-xs select-none ${
          theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-[#e9ecef] border-[#ced4da]'
        }`}>
          <div className="flex items-center gap-1">
            <div className={`px-3 py-1.5 rounded-t-lg border-t-2 flex items-center gap-2 text-xs font-semibold ${
              theme === 'dark' 
                ? 'bg-[#1e1e1e] text-white border-emerald-500' 
                : 'bg-white text-slate-900 border-emerald-600 shadow-xs'
            }`}>
              <FileCode className="w-3.5 h-3.5 text-amber-500" />
              <span>СП_Тесты_ЗаказПокупателя.bsl</span>
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
            </div>
            <div className={`px-3 py-1.5 rounded-t-lg flex items-center gap-2 text-xs text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 cursor-pointer`}>
              <FileCode className="w-3.5 h-3.5 opacity-60" />
              <span>СП_Тестирование.bsl</span>
            </div>
          </div>

          <div className="flex items-center gap-3 pr-2 font-mono text-[11px] text-slate-500">
            <span>BSL • UTF-8 • CRLF</span>
          </div>
        </div>

        {/* Code & Gutter Area */}
        <div className="relative font-mono text-xs overflow-x-auto min-h-[420px] select-text">
          <table className="w-full border-collapse">
            <tbody>
              {codeLines.map(line => {
                const isTestDeclaration = line.isTest;
                const testObj = isTestDeclaration !== undefined && line.testIdx !== undefined ? BSL_TESTS[line.testIdx] : null;
                const isCurrentActive = activeTest && testObj && activeTest.name === testObj.name;

                return (
                  <tr 
                    key={line.num}
                    onMouseEnter={() => setHoveredLine(line.num)}
                    onMouseLeave={() => setHoveredLine(null)}
                    onContextMenu={(e) => {
                      if (testObj) {
                        e.preventDefault();
                        setContextMenu({ x: e.clientX, y: e.clientY, test: testObj });
                      }
                    }}
                    className={`transition-colors ${
                      isCurrentActive 
                        ? (theme === 'dark' ? 'bg-emerald-950/20' : 'bg-emerald-50/60') 
                        : (hoveredLine === line.num ? (theme === 'dark' ? 'bg-[#2a2d2e]' : 'bg-slate-100') : '')
                    }`}
                  >
                    {/* Gutter: Line Number & YAxUnit Run Marker */}
                    <td className={`w-16 px-2 py-0.5 text-right select-none border-r ${
                      theme === 'dark' ? 'bg-[#1e1e1e] border-[#333333] text-slate-600' : 'bg-[#f0f0f0] border-slate-200 text-slate-400'
                    }`}>
                      <div className="flex items-center justify-between gap-1">
                        {/* Run Test Button Marker */}
                        {testObj ? (
                          <button
                            title={`Запустить тест СП: ${testObj.name}`}
                            onClick={() => startTest(testObj)}
                            className="w-4 h-4 rounded hover:scale-125 transition-transform flex items-center justify-center bg-emerald-500 text-slate-950 hover:bg-emerald-400 shadow-xs cursor-pointer group"
                          >
                            <Play className="w-2.5 h-2.5 fill-slate-950 ml-0.5" />
                          </button>
                        ) : (
                          <span className="w-4 inline-block"></span>
                        )}

                        <span className="text-[11px] font-mono opacity-80">{line.num}</span>
                      </div>
                    </td>

                    {/* Code Text with BSL Highlighting */}
                    <td className="px-3 py-0.5 whitespace-pre">
                      {line.comment ? (
                        <span className="text-emerald-600 dark:text-emerald-500 italic">{line.text}</span>
                      ) : line.annotation ? (
                        <span className="text-purple-600 dark:text-purple-400 font-bold">{line.text}</span>
                      ) : line.isTest && testObj ? (
                        <div className="flex items-center gap-2 flex-wrap">
                          <span>
                            <span className="text-blue-600 dark:text-blue-400 font-bold">Процедура</span>{' '}
                            <span className="text-amber-600 dark:text-amber-300 font-bold underline decoration-dotted underline-offset-2">{testObj.name}</span>() <span className="text-blue-600 dark:text-blue-400 font-bold">Экспорт</span>
                          </span>
                          <button 
                            onClick={() => startTest(testObj)}
                            className={`px-2 py-0.5 rounded text-[10px] font-sans font-bold flex items-center gap-1 border transition-all ${
                              isCurrentActive && isRunning
                                ? 'bg-amber-100 text-amber-900 border-amber-300 animate-pulse'
                                : isCurrentActive && !isRunning
                                ? 'bg-emerald-100 text-emerald-900 border-emerald-300'
                                : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border-emerald-200'
                            }`}
                          >
                            <Play className="w-2.5 h-2.5 fill-emerald-700" />
                            <span>{isCurrentActive && isRunning ? 'Выполняется...' : isCurrentActive ? 'Пройден ✔' : 'Запустить тест'}</span>
                          </button>
                        </div>
                      ) : line.text.includes('СП_') ? (
                        <span>
                          {line.text.split(/(СП_[a-zA-Zа-яА-Я0-9_]+)/g).map((part, i) => 
                            part.startsWith('СП_') ? (
                              <span key={i} className="text-cyan-600 dark:text-cyan-400 font-semibold">{part}</span>
                            ) : (
                              <span key={i} className={theme === 'dark' ? 'text-slate-200' : 'text-slate-800'}>{part}</span>
                            )
                          )}
                        </span>
                      ) : (
                        <span className={theme === 'dark' ? 'text-slate-200' : 'text-slate-800'}>
                          {line.text.replace('КонецПроцедуры', 'КонецПроцедуры')}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Real-Time Live Execution Bridge Console */}
        <div className={`p-3 border-t text-xs flex flex-col space-y-2 ${
          theme === 'dark' ? 'bg-[#181818] border-[#3c3c3c]' : 'bg-[#f1f3f5] border-[#ced4da]'
        }`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Terminal className="w-4 h-4 text-emerald-500" />
              <span className="font-bold text-slate-800 dark:text-slate-200">
                Консоль живого моста Specter (outDir/commands.json → result.json)
              </span>
              {isRunning && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500 text-slate-950 animate-pulse">
                  Выполняется шаг {currentStepIndex + 1}...
                </span>
              )}
            </div>

            <span className="font-mono text-[11px] text-slate-500">
              {activeTest ? `Активный тест: ${activeTest.name}` : 'Нажмите ▶ у теста для запуска'}
            </span>
          </div>

          <div className={`p-3 rounded-lg font-mono text-[11px] max-h-36 overflow-y-auto space-y-1 ${
            theme === 'dark' ? 'bg-[#0f0f0f] text-slate-300' : 'bg-white text-slate-800 border border-slate-200'
          }`}>
            {runLog.length === 0 ? (
              <div className="text-slate-500 italic">
                Ожидание запуска теста из редактора... Кликните на маркер ▶ на левом поле любой процедуры теста.
              </div>
            ) : (
              runLog.map((log, idx) => (
                <div key={idx} className="flex items-start justify-between gap-3">
                  <span className={log.text.includes('✔') ? 'text-emerald-500 font-bold' : log.text.includes('Запуск') ? 'text-cyan-400 font-bold' : ''}>
                    {log.text}
                  </span>
                  <span className="text-slate-500 text-[10px] shrink-0">{log.time}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
