import React, { useState } from 'react';
import { 
  Play, 
  RotateCw, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  Clock, 
  RefreshCw, 
  Terminal, 
  Layers, 
  Cpu, 
  ShieldCheck, 
  ShieldAlert, 
  Copy, 
  Check, 
  Sliders, 
  Sparkles,
  Ban
} from 'lucide-react';

export const SimulationSandbox: React.FC = () => {
  const [simulationMode, setSimulationMode] = useState<'fixed' | 'buggy'>('fixed');
  const [isRunning, setIsRunning] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState(0);
  const [copiedLogs, setCopiedLogs] = useState(false);
  const [mockResult, setMockResult] = useState<{
    runId: string;
    status: 'passed' | 'failed' | 'timeout';
    steps: Array<{ id: string; action: string; status: 'passed' | 'failed'; detail?: string }>;
  } | null>(null);

  const handleCopyLogs = () => {
    navigator.clipboard.writeText(logs.join('\n'));
    setCopiedLogs(true);
    setTimeout(() => setCopiedLogs(false), 2000);
  };

  const startSimulation = () => {
    setIsRunning(true);
    setProgress(0);
    setMockResult(null);
    setLogs([]);

    const runIdCommands = 'run_' + Math.random().toString(36).substring(2, 9);
    const runIdJob = simulationMode === 'buggy' 
      ? 'run_' + Math.random().toString(36).substring(2, 9) // ❌ Desync in buggy mode
      : runIdCommands; // ✅ Synchronized in fixed mode

    setLogs(prev => [...prev, `[12:00:01] [UI-Thread] Нажата кнопка «Запустить мост» на панели TestsView`]);
    setLogs(prev => [...prev, `[12:00:01] [TestsView] Сгенерирован runId для JSON: ${runIdCommands}`]);
    setLogs(prev => [...prev, `[12:00:01] [BridgeScenario] Сформирован bridge-commands.json (runId="${runIdCommands}", 7 команд R1)`]);

    setTimeout(() => {
      setProgress(25);
      setLogs(prev => [...prev, `[12:00:02] [LaunchMonitor] bridge-commands.json записан в каталог обмена (-Duitp.e2e.outDir)`]);
      setLogs(prev => [...prev, `[12:00:02] [Job API] Запущен фоновый Eclipse Job "Specter: запуск тонкого клиента АУФ"`]);

      if (simulationMode === 'buggy') {
        setLogs(prev => [...prev, `[12:00:02] ❌ [ДЕФЕКТ BridgeRunner.java:49] Сгенерирован НОВЫЙ runId: ${runIdJob}`]);
        setLogs(prev => [...prev, `[12:00:03] [BridgeLaunchHelper] Копирование конфигурации "UITP - Тонкий клиент АУФ"`]);
        setLogs(prev => [...prev, `[12:00:03] [BridgeLaunchHelper] Запуск процесса 1cv8c.exe (режим run)...`]);
        setLogs(prev => [...prev, `[12:00:04] [LaunchMonitor] Ожидание файла: bridge-result-${runIdJob}.json (таймаут 10 мин)...`]);

        setTimeout(() => {
          setProgress(50);
          setLogs(prev => [...prev, `[12:00:06] [1C:Предприятие] Клиент открыл ФормуСписка, прочитал commands.json (runId="${runIdCommands}")`]);
          setLogs(prev => [...prev, `[12:00:07] [1C:Предприятие] Выполнены UI-действия (открыть карточку, изменить ИНН, Записать, Закрыть)`]);
          setLogs(prev => [...prev, `[12:00:08] [1C:Предприятие] 1С записала результат в файл: bridge-result-${runIdCommands}.json`]);

          setTimeout(() => {
            setProgress(75);
            setLogs(prev => [...prev, `[12:00:09] [LaunchMonitor] Опрос каталога обмена... Файл bridge-result-${runIdJob}.json НЕ НАЙДЕН!`]);
            setLogs(prev => [...prev, `[12:00:10] [LaunchMonitor] (В каталоге лежит только bridge-result-${runIdCommands}.json с другим ID)`]);
            setLogs(prev => [...prev, `[12:00:11] [LaunchMonitor] Сон Thread.sleep(1500ms)... Цикл 1 из 400...`]);

            setTimeout(() => {
              setProgress(100);
              setIsRunning(false);
              setLogs(prev => [...prev, `[12:10:11] ❌ [ОШИБКА ТАЙМАУТА] Прошло 600 секунд. Результат не получен: таймаут ожидания результата моста.`]);
              setMockResult({
                runId: runIdJob,
                status: 'timeout',
                steps: []
              });
            }, 1800);
          }, 1200);
        }, 1200);
      } else {
        // FIXED FLOW
        setLogs(prev => [...prev, `[12:00:02] ✅ [FIX BridgeRunner] Использован согласованный runId: ${runIdJob}`]);
        setLogs(prev => [...prev, `[12:00:03] [BridgeLaunchHelper] Переиспользование конфигурации "UITP - Тонкий клиент АУФ" (без дублирования)`]);
        setLogs(prev => [...prev, `[12:00:03] [BridgeLaunchHelper] Запуск тонкого клиента (с поддержкой IProgressMonitor)`]);

        setTimeout(() => {
          setProgress(60);
          setLogs(prev => [...prev, `[12:00:05] [1C:Предприятие] Клиент открыл форму, выполнил 7 шагов UI-сценария R1`]);
          setLogs(prev => [...prev, `[12:00:06] [1C:Предприятие] Сформирован bridge-result-${runIdJob}.json (статус: passed)`]);

          setTimeout(() => {
            setProgress(100);
            setIsRunning(false);
            setLogs(prev => [...prev, `[12:00:07] ✅ [LaunchMonitor] Найден ожидаемый файл: bridge-result-${runIdJob}.json`]);
            setLogs(prev => [...prev, `[12:00:07] [BridgeRunner] Парсинг JSON результата успешен: 7 шагов, passed=7, failed=0`]);
            setLogs(prev => [...prev, `[12:00:08] [Display.asyncExec] Безопасная передача в UI-поток: BridgeResultStore.set(br)`]);
            setLogs(prev => [...prev, `[12:00:08] [TestsView] Дерево обновлено: Прогон моста — PASSED [7 ✓ / 0 ✗]`]);
            setLogs(prev => [...prev, `[12:00:08] [ResultsView] StyledText раскрасил шаги: 7 строк подсвечено зеленым`]);
            setMockResult({
              runId: runIdJob,
              status: 'passed',
              steps: [
                { id: 'c1', action: 'openList (Список контрагентов)', status: 'passed' },
                { id: 'c2', action: 'openCard (Карточка ООО "Ромашка")', status: 'passed' },
                { id: 'c3', action: 'setValue (ИНН = "7701234567")', status: 'passed' },
                { id: 'c4', action: 'assertValue (Проверка значения ИНН)', status: 'passed' },
                { id: 'c5', action: 'click (Команда "Записать")', status: 'passed' },
                { id: 'c6', action: 'assert (Форма.Модифицированность = false)', status: 'passed' },
                { id: 'c7', action: 'click (Команда "Закрыть")', status: 'passed' }
              ]
            });
          }, 1100);
        }, 1100);
      }
    }, 900);
  };

  return (
    <div className="space-y-6">
      {/* Simulation Control Header */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-indigo-100 text-indigo-700">
                <Cpu className="w-4 h-4" />
              </div>
              <h3 className="text-base font-bold text-slate-900 tracking-tight">
                Интерактивный симулятор моста Specter 1C:EDT
              </h3>
            </div>
            <p className="text-xs text-slate-600 mt-1 max-w-2xl leading-relaxed">
              Наглядное воспроизведение поведения плагина в среде Eclipse RCP. Сравните работу с багом десинхронизации (10-минутный таймаут) 
              и исправленным контуром (мгновенный поиск файла и асинхронное обновление UI).
            </p>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            {/* Mode Switcher */}
            <div className="bg-slate-100 p-1 rounded-xl flex text-xs font-semibold">
              <button
                onClick={() => { if (!isRunning) setSimulationMode('fixed'); }}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                  simulationMode === 'fixed'
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Патч (Fixed)</span>
              </button>
              <button
                onClick={() => { if (!isRunning) setSimulationMode('buggy'); }}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                  simulationMode === 'buggy'
                    ? 'bg-rose-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <ShieldAlert className="w-3.5 h-3.5" />
                <span>Оригинал с багом</span>
              </button>
            </div>

            <button
              onClick={startSimulation}
              disabled={isRunning}
              className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold text-white shadow-xs transition-all active:scale-[0.97] ${
                isRunning
                  ? 'bg-slate-400 cursor-not-allowed'
                  : simulationMode === 'fixed'
                  ? 'bg-slate-900 hover:bg-slate-800'
                  : 'bg-rose-600 hover:bg-rose-700'
              }`}
            >
              {isRunning ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Выполняется Eclipse Job...</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" />
                  <span>Запустить сценарий R1</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Progress bar */}
        {isRunning && (
          <div className="pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-600 mb-1.5">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-indigo-600 animate-ping"></span>
                Прогресс выполнения Job в фоновом потоке Eclipse:
              </span>
              <span className="font-mono">{progress}%</span>
            </div>
            <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden p-0.5 border border-slate-200">
              <div
                className={`h-full rounded-full transition-all duration-300 ${
                  simulationMode === 'buggy' ? 'bg-rose-500' : 'bg-emerald-500'
                }`}
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Main Dual-View: Left EDT Views / Right Execution Trace */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Simulated EDT Workbench Views */}
        <div className="lg:col-span-6 space-y-4">
          {/* TestsView */}
          <div className="bg-white rounded-2xl border border-slate-200/90 overflow-hidden shadow-xs">
            <div className="px-4 py-3 bg-slate-100/90 border-b border-slate-200 flex items-center justify-between text-xs font-bold text-slate-800">
              <span className="flex items-center gap-2">
                <img src="/icons/specter-tests.png" alt="Specter Tests" className="w-4 h-4 rounded-xs shrink-0" />
                Виджет EDT: Панель «Тесты» (TestsView)
              </span>
              <span className="font-mono text-[10px] text-slate-500 px-2 py-0.5 bg-white rounded border border-slate-200">
                TreeViewer / JFace
              </span>
            </div>
            <div className="p-4 min-h-[170px] bg-white font-mono text-xs space-y-2">
              {mockResult ? (
                mockResult.status === 'timeout' ? (
                  <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs">
                    <div className="font-bold flex items-center gap-2 text-rose-800">
                      <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
                      <span>Ошибка: Результат моста не получен</span>
                    </div>
                    <p className="mt-1.5 text-[11px] text-rose-700 leading-relaxed font-sans">
                      Таймаут ожидания результата моста (10 минут). Записан ID: <code className="font-mono font-bold bg-rose-100 px-1 rounded">run_XXXX</code>, 
                      а проверялся несовпадающий ID. UI заблокирован или ждет отклика.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-1.5">
                    <div className="font-bold text-slate-900 pb-2 border-b border-slate-100 flex items-center justify-between">
                      <span>Прогон моста: <span className="text-emerald-700">PASSED</span> [7 ✓ / 0 ✗]</span>
                      <span className="text-[11px] text-slate-400 font-normal">7 шагов</span>
                    </div>
                    {mockResult.steps.map((s) => (
                      <div key={s.id} className="pl-2 text-[11px] text-slate-700 flex items-center justify-between hover:bg-slate-50 p-1 rounded transition-colors">
                        <div className="flex items-center gap-2 truncate">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                          <span className="text-slate-400 font-mono">#{s.id}</span>
                          <span className="truncate">{s.action}</span>
                        </div>
                        <span className="font-bold text-emerald-700 text-[10px] bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 shrink-0">
                          PASSED
                        </span>
                      </div>
                    ))}
                  </div>
                )
              ) : isRunning ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <RefreshCw className="w-6 h-6 text-indigo-600 animate-spin mb-2" />
                  <p className="text-slate-600 text-xs font-semibold">Запускаю тонкий клиент АУФ (сценарий-канон R1)…</p>
                  <p className="text-slate-400 text-[11px] mt-0.5">Ожидание формирования bridge-result.json</p>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center py-8 text-center text-slate-400">
                  <Play className="w-6 h-6 text-slate-300 mb-2" />
                  <p className="text-xs font-medium">Нажмите «Запустить сценарий R1», чтобы протестировать прогон.</p>
                </div>
              )}
            </div>
          </div>

          {/* ResultsView */}
          <div className="bg-white rounded-2xl border border-slate-200/90 overflow-hidden shadow-xs">
            <div className="px-4 py-3 bg-slate-100/90 border-b border-slate-200 flex items-center justify-between text-xs font-bold text-slate-800">
              <span className="flex items-center gap-2">
                <img src="/icons/specter-results.png" alt="Specter Results" className="w-4 h-4 rounded-xs shrink-0" />
                Виджет EDT: Панель «Результаты» (ResultsView)
              </span>
              <span className="font-mono text-[10px] text-slate-500 px-2 py-0.5 bg-white rounded border border-slate-200">
                StyledText / SWT
              </span>
            </div>
            <div className="p-4 min-h-[170px] bg-slate-950 text-slate-100 font-mono text-xs overflow-auto max-h-60 rounded-b-2xl">
              {mockResult && mockResult.status === 'passed' ? (
                <div className="space-y-1 text-[11px] leading-relaxed">
                  <div className="text-indigo-300">Прогон моста: <span className="font-bold">{mockResult.runId}</span></div>
                  <div>Статус: <span className="text-emerald-400 font-bold">PASSED</span> (passed 7 / failed 0)</div>
                  <div className="text-slate-400 text-[10px]">Файл: /exchange/bridge-result-{mockResult.runId}.json</div>
                  <div className="pt-2 text-slate-400 border-t border-slate-800">Шаги выполнения:</div>
                  {mockResult.steps.map((s) => (
                    <div key={s.id} className="pl-2 flex items-center justify-between">
                      <span className="text-slate-300">#{s.id} {s.action}</span>
                      <span className="text-emerald-400 font-bold ml-2">PASSED</span>
                    </div>
                  ))}
                </div>
              ) : mockResult && mockResult.status === 'timeout' ? (
                <div className="text-rose-400 text-xs leading-relaxed space-y-1">
                  <div className="font-bold">❌ Результат моста не найден. Процесс прерван по таймауту 600 сек (10 мин).</div>
                  <div className="text-slate-400 text-[10px]">Причина: файл bridge-result-{mockResult.runId}.json не появился в каталоге обмена.</div>
                </div>
              ) : (
                <div className="text-slate-500 italic text-xs py-8 text-center font-sans">
                  Результат моста не найден. Запустите сценарий на панели «Тесты».
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Live Execution Logs & Threading Tracer */}
        <div className="lg:col-span-6 bg-white rounded-2xl border border-slate-200/90 shadow-xs flex flex-col overflow-hidden">
          <div className="px-5 py-3 bg-slate-100/90 border-b border-slate-200 flex items-center justify-between text-xs font-bold text-slate-800">
            <span className="flex items-center gap-2">
              <Terminal className="w-4 h-4 text-slate-600" />
              Трассировка потоков (Thread Monitor &amp; Job API)
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={handleCopyLogs}
                disabled={logs.length === 0}
                className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-600 hover:text-slate-900 disabled:opacity-40 transition-colors"
              >
                {copiedLogs ? (
                  <>
                    <Check className="w-3 h-3 text-emerald-600" />
                    <span>Скопировано</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3 h-3" />
                    <span>Лог</span>
                  </>
                )}
              </button>
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                simulationMode === 'buggy' ? 'bg-rose-100 text-rose-800 border border-rose-200' : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
              }`}>
                {simulationMode === 'buggy' ? 'Десинхрон runId' : 'Fix: Согласован'}
              </span>
            </div>
          </div>

          <div className="p-4 bg-slate-900 text-slate-200 font-mono text-xs overflow-auto flex-1 min-h-[380px] max-h-[460px] space-y-2">
            {logs.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-slate-500 italic py-16 text-center font-sans">
                <Terminal className="w-8 h-8 text-slate-600 mb-2" />
                <p className="text-xs">Лог событий пуст.</p>
                <p className="text-[11px] text-slate-400 mt-1 max-w-xs">
                  Запустите сценарий для визуализации разделения UI-потока и фонового Eclipse Job.
                </p>
              </div>
            ) : (
              logs.map((log, idx) => {
                const isError = log.includes('❌') || log.includes('ОШИБКА');
                const isSuccess = log.includes('✅');
                return (
                  <div
                    key={idx}
                    className={`leading-relaxed text-[11px] p-1 rounded ${
                      isError ? 'bg-rose-950/40 text-rose-300 font-semibold border-l-2 border-rose-500' :
                      isSuccess ? 'bg-emerald-950/40 text-emerald-300 font-semibold border-l-2 border-emerald-500' : 
                      'text-slate-300 hover:bg-slate-800/40'
                    }`}
                  >
                    {log}
                  </div>
                );
              })
            )}
          </div>

          <div className="p-3 bg-slate-50 border-t border-slate-200 text-xs text-slate-500 flex items-center justify-between">
            <span className="text-[11px] font-mono">EDT 1C RCP Runtime Monitor</span>
            <span className="text-[11px] text-slate-400">Display.asyncExec / Job.schedule()</span>
          </div>
        </div>
      </div>
    </div>
  );
};
