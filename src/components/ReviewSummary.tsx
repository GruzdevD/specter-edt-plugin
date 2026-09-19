import React from 'react';
import { REVIEW_METRICS } from '../data/reviewData';
import { 
  AlertOctagon, 
  CheckCircle2, 
  AlertTriangle, 
  ArrowRight, 
  Zap, 
  Cpu, 
  Layers, 
  FileCode2, 
  Check, 
  Activity, 
  Sparkles,
  Database,
  ArrowRightLeft,
  Clock,
  TerminalSquare
} from 'lucide-react';

interface ReviewSummaryProps {
  onSelectTab: (tab: 'overview' | 'issues' | 'files' | 'simulation') => void;
  onSelectIssue?: (issueId: string) => void;
}

export const ReviewSummary: React.FC<ReviewSummaryProps> = ({ onSelectTab, onSelectIssue }) => {
  return (
    <div className="space-y-7">
      {/* Executive Callout Alert */}
      <div className="relative overflow-hidden rounded-2xl border border-rose-200/80 bg-gradient-to-br from-rose-50/90 via-white to-orange-50/40 p-6 shadow-sm">
        <div className="absolute top-0 right-0 -mt-8 -mr-8 w-48 h-48 bg-rose-500/5 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col sm:flex-row items-start gap-5">
          <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-600 flex items-center justify-center shrink-0 shadow-inner">
            <AlertOctagon className="w-6 h-6" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-3 flex-wrap">
              <div className="flex items-center gap-2.5">
                <span className="px-2.5 py-1 rounded-lg text-[11px] font-extrabold tracking-wider uppercase bg-rose-600 text-white shadow-xs">
                  P0 Blocker
                </span>
                <span className="text-xs text-rose-700 font-mono font-medium">
                  BridgeRunner.java • line 45-65
                </span>
              </div>
              <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-rose-500" />
                Таймаут зависания: 10 минут (600 000 мс)
              </span>
            </div>

            <h2 className="mt-2.5 text-lg font-bold text-slate-900 tracking-tight">
              Критический дефект аудита: фатальная рассинхронизация runId в мосте 1С
            </h2>
            
            <p className="mt-2 text-sm text-slate-700 leading-relaxed max-w-4xl">
              В исходном коде плагина <code className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-xs text-rose-800 font-medium">ru.ozon.uitp.e2e</code> кнопка 
              запуска тестов формировала один <code className="font-mono font-semibold text-rose-700">runId_A</code> в <code className="font-mono text-xs">bridge-commands.json</code>, 
              а класс <code className="font-mono text-xs font-semibold">BridgeRunner</code> в фоновом Eclipse Job генерировал <strong>второй, независимый runId_B</strong> и 
              ждал появления файла отчёта со своим ID. Клиент 1С формировал отчёт по первому ID, из-за чего плагин 
              <strong> гарантированно зависал ровно на 10 минут</strong> по таймауту. Тесты никогда не завершались успешно!
            </p>

            <div className="mt-4 flex flex-wrap items-center gap-3 pt-2">
              <button
                onClick={() => onSelectTab('issues')}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 active:scale-[0.98] text-white text-xs font-semibold shadow-sm transition-all duration-150 border border-rose-500/30"
              >
                <span>Смотреть разбор дефекта</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
              
              <button
                onClick={() => onSelectTab('files')}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white hover:bg-slate-50 active:scale-[0.98] text-slate-800 border border-slate-200 shadow-2xs hover:border-slate-300 text-xs font-semibold transition-all duration-150"
              >
                <FileCode2 className="w-3.5 h-3.5 text-emerald-600" />
                <span>Смотреть исправленный BridgeRunner.java</span>
              </button>

              <button
                onClick={() => onSelectTab('simulation')}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-50 hover:bg-indigo-100/80 active:scale-[0.98] text-indigo-800 border border-indigo-200 text-xs font-semibold transition-all duration-150"
              >
                <TerminalSquare className="w-3.5 h-3.5 text-indigo-600" />
                <span>Запустить симуляцию бага vs фикса</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bridge Protocol Architecture Flow */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-6 shadow-xs">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <div>
            <h3 className="text-sm font-bold text-slate-900 tracking-tight flex items-center gap-2">
              <ArrowRightLeft className="w-4 h-4 text-indigo-600" />
              <span>Архитектура обмена между 1C:EDT и тонким клиентом 1С (Live Bridge)</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Схема взаимодействия через файловый шлюз и точка устранения рассинхронизации
            </p>
          </div>
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Контракт синхронизирован
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/70 relative">
            <span className="text-[10px] font-mono font-bold uppercase text-slate-500 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
              Этап 1: Инициация
            </span>
            <div className="font-semibold text-slate-900 text-xs mt-1">TestsView.java</div>
            <p className="text-[11px] text-slate-600 mt-1">
              Генерация <code className="font-mono text-indigo-700 font-bold">runId</code> и команд в JSON через <code className="font-mono">BridgeScenario</code>.
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-rose-50/50 border border-rose-200 relative">
            <span className="text-[10px] font-mono font-bold uppercase text-rose-700 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></span>
              Этап 2: Точка фикса
            </span>
            <div className="font-semibold text-rose-900 text-xs mt-1">BridgeRunner.java</div>
            <p className="text-[11px] text-slate-600 mt-1">
              ✅ <strong>Фикс:</strong> передаем <code className="font-mono text-emerald-700 font-bold">runId</code> в Job вместо повторной генерации другого ID.
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-amber-50/50 border border-amber-200 relative">
            <span className="text-[10px] font-mono font-bold uppercase text-amber-700 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
              Этап 3: Исполнение 1С
            </span>
            <div className="font-semibold text-amber-900 text-xs mt-1">1С:Предприятие (BSL)</div>
            <p className="text-[11px] text-slate-600 mt-1">
              Тонкий клиент читает команды, выполняет клики и пишет <code className="font-mono text-xs">bridge-result-[runId].json</code>.
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-emerald-50/50 border border-emerald-200 relative">
            <span className="text-[10px] font-mono font-bold uppercase text-emerald-700 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              Этап 4: Результаты
            </span>
            <div className="font-semibold text-emerald-900 text-xs mt-1">ResultsView.java</div>
            <p className="text-[11px] text-slate-600 mt-1">
              Фоновый парсинг отчёта, асинхронный показ в SWT StyledText без блокировки GUI.
            </p>
          </div>
        </div>
      </div>

      {/* Mandatory Guideline Checks */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-5 rounded-2xl border border-amber-200/80 bg-gradient-to-b from-amber-50/70 to-white flex flex-col justify-between shadow-2xs hover:shadow-xs transition-shadow">
          <div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-amber-100/80 text-amber-800 border border-amber-200">
                  <Layers className="w-4 h-4" />
                </div>
                <span className="text-xs font-bold uppercase tracking-wider text-amber-900">
                  OSGi &amp; MANIFEST.MF
                </span>
              </div>
              <span className="px-2 py-0.5 rounded-md text-[11px] font-semibold bg-amber-100 text-amber-900 border border-amber-300">
                Замечания
              </span>
            </div>
            <p className="mt-3 text-xs text-slate-700 leading-relaxed">
              Отсутствовал завершающий перенос строки в <code className="font-mono text-amber-900">MANIFEST.MF</code> (нарушение RFC 1960 — последний пакет отбрасывался OSGi), 
              не были экспортированы пакеты UI, отсутствовали зависимости <code className="font-mono">org.eclipse.core.resources</code> и <code className="font-mono">org.eclipse.core.jobs</code>.
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-amber-200/80 flex items-center justify-between text-[11px]">
            <span className="text-amber-900 font-medium">Статус: Полностью исправлен</span>
            <button
              onClick={() => onSelectTab('files')}
              className="text-amber-800 hover:text-amber-900 font-semibold underline underline-offset-2 hover:opacity-80"
            >
              Смотреть файл
            </button>
          </div>
        </div>

        <div className="p-5 rounded-2xl border border-rose-200/80 bg-gradient-to-b from-rose-50/70 to-white flex flex-col justify-between shadow-2xs hover:shadow-xs transition-shadow">
          <div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-rose-100/80 text-rose-800 border border-rose-200">
                  <Cpu className="w-4 h-4" />
                </div>
                <span className="text-xs font-bold uppercase tracking-wider text-rose-900">
                  UI-поток и Job API
                </span>
              </div>
              <span className="px-2 py-0.5 rounded-md text-[11px] font-semibold bg-rose-100 text-rose-900 border border-rose-300">
                Устранено
              </span>
            </div>
            <p className="mt-3 text-xs text-slate-700 leading-relaxed">
              Синхронный вызов <code className="font-mono text-rose-900">reloadLatestFromOutDir()</code> выполнялся прямо в UI-потоке 
              при создании панели и тулбаре (фриз интерфейса EDT). В <code className="font-mono">waitForBridgeResult</code> отсутствовала проверка отмены <code className="font-mono">monitor.isCanceled()</code>.
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-rose-200/80 flex items-center justify-between text-[11px]">
            <span className="text-rose-900 font-medium">Статус: Job API + asyncExec</span>
            <button
              onClick={() => onSelectTab('files')}
              className="text-rose-800 hover:text-rose-900 font-semibold underline underline-offset-2 hover:opacity-80"
            >
              Смотреть файл
            </button>
          </div>
        </div>

        <div className="p-5 rounded-2xl border border-emerald-200/80 bg-gradient-to-b from-emerald-50/70 to-white flex flex-col justify-between shadow-2xs hover:shadow-xs transition-shadow">
          <div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-xl bg-emerald-100/80 text-emerald-800 border border-emerald-200">
                  <CheckCircle2 className="w-4 h-4" />
                </div>
                <span className="text-xs font-bold uppercase tracking-wider text-emerald-900">
                  SWT / JFace Only
                </span>
              </div>
              <span className="px-2 py-0.5 rounded-md text-[11px] font-semibold bg-emerald-100 text-emerald-900 border border-emerald-300">
                100% Чисто
              </span>
            </div>
            <p className="mt-3 text-xs text-slate-700 leading-relaxed">
              В исходниках нет ни единого импорта Swing или AWT. Использованы нативные SWT <code className="font-mono">Composite</code>, 
              <code className="font-mono">StyledText</code>, JFace <code className="font-mono">TreeViewer</code>, Eclipse <code className="font-mono">ViewPart</code>.
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-emerald-200/80 flex items-center justify-between text-[11px]">
            <span className="text-emerald-900 font-medium">Статус: Строгое соответствие RCP</span>
            <span className="text-emerald-700 font-mono text-[10px]">0 AWT/Swing</span>
          </div>
        </div>
      </div>

      {/* Audit Metric Cards */}
      <div>
        <div className="flex items-center justify-between mb-3.5">
          <h3 className="text-sm font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Activity className="w-4 h-4 text-indigo-600" />
            <span>Метрики соответствия EDT_PLUGIN_DEV архитектуре</span>
          </h3>
          <span className="text-xs text-slate-500">5 ключевых областей аудита</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5">
          {REVIEW_METRICS.map((metric) => (
            <div
              key={metric.id}
              className="p-4 rounded-2xl bg-white border border-slate-200/80 shadow-2xs hover:shadow-xs transition-all hover:border-slate-300 group"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-slate-500 group-hover:text-slate-700 transition-colors">
                  {metric.label}
                </span>
                {metric.status === 'passed' && <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />}
                {metric.status === 'warning' && <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />}
                {metric.status === 'failed' && <AlertOctagon className="w-4 h-4 text-rose-500 shrink-0" />}
              </div>
              <div className={`text-base font-extrabold ${
                metric.status === 'passed' ? 'text-emerald-700' :
                metric.status === 'warning' ? 'text-amber-700' : 'text-rose-700'
              }`}>
                {metric.value}
              </div>
              <p className="mt-2 text-[11px] text-slate-600 leading-normal line-clamp-3">
                {metric.description}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* Architectural Improvements Detailed Grid */}
      <div className="bg-white rounded-2xl border border-slate-200/90 p-6 shadow-xs">
        <div className="flex items-center justify-between mb-5 flex-wrap gap-2">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Zap className="w-4 h-4" />
            </div>
            <span>Комплекс выполненных оптимизаций и исправлений (Production-Ready)</span>
          </h3>
          <span className="text-xs font-medium text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200/70">
            6 из 6 дефектов исправлено
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              1
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Синхронизация контракта моста (runId)</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Теперь <code className="font-mono text-slate-800">BridgeRunner.runAsync</code> принимает строго тот <code className="font-mono text-slate-800">runId</code>, 
                который был сериализован в <code className="font-mono text-slate-800">bridge-commands.json</code>. Тесты завершаются сразу по готовности отчёта, 
                исключая 10-минутное зависание.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              2
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Полная ликвидация UI-блокировок в TestsView</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Вызов <code className="font-mono text-slate-800">reloadLatestFromOutDirAsync</code> перенесен в системный Eclipse Job. 
                Создание панели <code className="font-mono text-slate-800">TestsView</code> и клик по кнопке «Обновить» никогда не подвешивают воркбенч 1C:EDT.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              3
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Поддержка Cancel в Eclipse Progress View</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Цикл ожидания в <code className="font-mono text-slate-800">LaunchMonitor.waitForBridgeResult</code> проверяет 
                <code className="font-mono text-slate-800">IProgressMonitor.isCanceled()</code> и немедленно освобождает поток при нажатии кнопки остановки.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              4
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Исправление подсветки StyledText и O(N) в ResultsView</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Исправлена грубая ошибка сдвига <code className="font-mono text-slate-800">colondIdx - 3</code> в <code className="font-mono text-slate-800">ResultsView</code>. 
                Устранено замедляющее копирование <code className="font-mono text-slate-800">append(ranges, sr)</code> на каждый шаг сценария через <code className="font-mono text-slate-800">List&lt;StyleRange&gt;</code>.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              5
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Предотвращение утечки ILaunchConfiguration</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Вместо размножения копий конфигурации в каталоге метаданных воркспейса, <code className="font-mono text-slate-800">BridgeLaunchHelper</code> 
                повторно использует существующую рабочую конфигурацию <code className="font-mono text-slate-800">"UITP - &lt;имя&gt;"</code>.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3.5 p-4 rounded-xl bg-slate-50/80 border border-slate-200/60 hover:border-slate-300 transition-colors">
            <span className="w-6 h-6 rounded-lg bg-emerald-600 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs">
              6
            </span>
            <div>
              <strong className="text-slate-900 font-semibold block text-sm">Безопасный жизненный цикл SWT Color &amp; RFC OSGi</strong>
              <p className="text-slate-600 text-xs mt-1 leading-relaxed">
                Убраны статические утечки <code className="font-mono text-slate-800">Display.getDefault().getSystemColor</code>. 
                Добавлен RFC 1960 перенос строки в <code className="font-mono text-slate-800">MANIFEST.MF</code>, экспортированы пакеты и добавлены отсутствующие бандлы.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

