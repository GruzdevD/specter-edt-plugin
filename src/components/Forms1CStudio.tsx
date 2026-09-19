import React, { useState, useEffect, useRef } from 'react';
import { DISCOVERED_1C_TESTS, TestCase1C, generateTestHtmlReport } from '../data/tests1cData';
import {
  Play,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RotateCcw,
  Search,
  Download,
  Copy,
  Check,
  Code2,
  Eye,
  FileCode,
  Sparkles,
  Terminal,
  ExternalLink,
  Layers,
  ChevronRight,
  Filter,
  Flame,
  Clock,
  ShieldCheck,
  FileText
} from 'lucide-react';

export const Forms1CStudio: React.FC = () => {
  const [tests, setTests] = useState<TestCase1C[]>(DISCOVERED_1C_TESTS);
  const [selectedTestId, setSelectedTestId] = useState<string>(DISCOVERED_1C_TESTS[0].id);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTag, setSelectedTag] = useState<string>('all');
  const [isRunningAll, setIsRunningAll] = useState(false);
  const [runningTestId, setRunningTestId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'rendered_html' | 'source_html'>('rendered_html');
  const [isSimulateFailure, setIsSimulateFailure] = useState(false);
  const [copiedHtml, setCopiedHtml] = useState(false);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const selectedTest = tests.find(t => t.id === selectedTestId) || tests[0];

  // All unique tags
  const allTags = Array.from(new Set(tests.flatMap(t => t.tags)));

  // Filtered tests
  const filteredTests = tests.filter(test => {
    const matchesSearch =
      test.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.suite.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesTag = selectedTag === 'all' || test.tags.includes(selectedTag);
    return matchesSearch && matchesTag;
  });

  // Group tests by suite
  const suites = Array.from(new Set(filteredTests.map(t => t.suite)));

  // Generate HTML Report
  const htmlReport = generateTestHtmlReport(selectedTest, tests, isSimulateFailure);

  // Update iframe whenever HTML changes
  useEffect(() => {
    if (iframeRef.current) {
      iframeRef.current.srcdoc = htmlReport;
    }
  }, [htmlReport, selectedTest, isSimulateFailure]);

  // Run all tests simulation
  const handleRunAllTests = () => {
    if (isRunningAll) return;
    setIsRunningAll(true);

    // Reset status to pending
    setTests(prev => prev.map(t => ({ ...t, status: 'pending' })));

    let currentIndex = 0;
    const runNext = () => {
      if (currentIndex >= tests.length) {
        setIsRunningAll(false);
        setRunningTestId(null);
        return;
      }

      const currentTest = tests[currentIndex];
      setRunningTestId(currentTest.id);
      setSelectedTestId(currentTest.id);

      setTests(prev =>
        prev.map((t, idx) =>
          idx === currentIndex ? { ...t, status: 'running' } : t
        )
      );

      setTimeout(() => {
        const isFailed = isSimulateFailure && currentIndex === tests.length - 1;
        setTests(prev =>
          prev.map((t, idx) =>
            idx === currentIndex
              ? { ...t, status: isFailed ? 'failed' : 'passed' }
              : t
          )
        );
        currentIndex++;
        runNext();
      }, 350);
    };

    runNext();
  };

  // Run single test
  const handleRunSingleTest = (testId: string) => {
    setSelectedTestId(testId);
    setRunningTestId(testId);
    setTests(prev =>
      prev.map(t => (t.id === testId ? { ...t, status: 'running' } : t))
    );

    setTimeout(() => {
      setRunningTestId(null);
      const isFailed = isSimulateFailure && testId === selectedTest.id;
      setTests(prev =>
        prev.map(t =>
          t.id === testId ? { ...t, status: isFailed ? 'failed' : 'passed' } : t
        )
      );
    }, 450);
  };

  const copyHtmlReport = () => {
    navigator.clipboard.writeText(htmlReport);
    setCopiedHtml(true);
    setTimeout(() => setCopiedHtml(false), 2000);
  };

  const downloadHtmlFile = () => {
    const blob = new Blob([htmlReport], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `report-1c-${selectedTest.name}.html`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const passedCount = tests.filter(t => t.status === 'passed').length;
  const failedCount = tests.filter(t => t.status === 'failed').length;

  return (
    <div className="space-y-4">
      {/* 1. TOP CONTROL BAR / HEADER */}
      <div className="bg-white rounded-2xl border border-slate-200/90 p-4 sm:p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Left branding & stats */}
        <div className="flex items-center gap-3.5">
          <div className="w-11 h-11 rounded-2xl bg-amber-500 flex items-center justify-center text-slate-950 font-black text-lg shadow-sm border border-amber-400">
            1С
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-base font-bold text-slate-900 tracking-tight">
                Консоль тестов расширения <span className="font-mono text-amber-800 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200 text-xs">СП_Тестирование</span>
              </h2>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                {tests.length} тестов обнаружено
              </span>
            </div>
            <div className="flex items-center gap-3 text-xs text-slate-500 mt-1 font-mono">
              <span className="text-emerald-600 font-semibold">✔ Пройдено: {passedCount}</span>
              <span className="text-slate-300">•</span>
              <span className={failedCount > 0 ? 'text-rose-600 font-bold' : 'text-slate-400'}>
                ✖ Ошибок: {failedCount}
              </span>
              <span className="text-slate-300">•</span>
              <span>Мост: <code className="text-indigo-600 font-semibold">ru.ozon.uitp.e2e</code></span>
            </div>
          </div>
        </div>

        {/* Right actions: Run buttons and failure simulator */}
        <div className="flex items-center gap-2.5 flex-wrap">
          <label className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700 cursor-pointer hover:bg-slate-100 select-none">
            <input
              type="checkbox"
              checked={isSimulateFailure}
              onChange={(e) => setIsSimulateFailure(e.target.checked)}
              className="w-3.5 h-3.5 text-rose-600 rounded border-slate-300 focus:ring-rose-500"
            />
            <span className="text-[11px] font-medium">Симулировать ошибку (Mismatch)</span>
          </label>

          <button
            onClick={() => handleRunSingleTest(selectedTest.id)}
            disabled={isRunningAll}
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200/80 active:scale-[0.98] text-slate-800 text-xs font-bold transition-all disabled:opacity-50"
          >
            <Play className="w-3.5 h-3.5 text-slate-600 fill-slate-600" />
            <span>Выполнить выбранный</span>
          </button>

          <button
            onClick={handleRunAllTests}
            disabled={isRunningAll}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 active:scale-[0.98] text-slate-950 font-bold text-xs shadow-md shadow-amber-500/20 border border-amber-600 transition-all disabled:opacity-50"
          >
            {isRunningAll ? (
              <>
                <Sparkles className="w-4 h-4 animate-spin text-slate-900" />
                <span>Выполняются тесты 1С...</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-slate-950" />
                <span>Выполнить все тесты</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* 2. MAIN 2-COLUMN WORKSPACE */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        {/* LEFT COLUMN: LIST OF DISCOVERED TESTS IN EXTENSION */}
        <div className="lg:col-span-5 space-y-3">
          {/* Search & Tags Filter */}
          <div className="bg-white p-3 rounded-2xl border border-slate-200/90 shadow-xs space-y-2.5">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Поиск по имени теста, описанию или модулю..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-amber-500 focus:ring-1 focus:ring-amber-500 outline-none"
              />
            </div>

            {/* Tag Pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5 text-[11px] no-scrollbar">
              <button
                onClick={() => setSelectedTag('all')}
                className={`px-2.5 py-1 rounded-lg font-medium whitespace-nowrap transition-colors ${
                  selectedTag === 'all'
                    ? 'bg-slate-900 text-white font-bold'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Все ({tests.length})
              </button>
              {allTags.map(tag => (
                <button
                  key={tag}
                  onClick={() => setSelectedTag(tag)}
                  className={`px-2 py-1 rounded-lg font-medium whitespace-nowrap transition-colors ${
                    selectedTag === tag
                      ? 'bg-amber-500 text-slate-950 font-bold'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  #{tag}
                </button>
              ))}
            </div>
          </div>

          {/* Test Suites Accordion / List */}
          <div className="space-y-3">
            {suites.map(suiteName => {
              const suiteTests = filteredTests.filter(t => t.suite === suiteName);
              if (suiteTests.length === 0) return null;
              const firstTest = suiteTests[0];

              return (
                <div key={suiteName} className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                  {/* Suite Title Bar */}
                  <div className="bg-slate-50/90 px-3.5 py-2.5 border-b border-slate-200/80 flex items-center justify-between">
                    <div className="flex items-center gap-2 min-w-0">
                      <Layers className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                      <span className="text-xs font-bold text-slate-900 truncate">
                        {suiteName}
                      </span>
                    </div>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-200/70 text-slate-600 font-semibold shrink-0">
                      {suiteTests.length} {suiteTests.length === 1 ? 'тест' : 'тестов'}
                    </span>
                  </div>

                  {/* Test Cases inside Suite */}
                  <div className="divide-y divide-slate-100">
                    {suiteTests.map(test => {
                      const isSelected = selectedTest.id === test.id;
                      const isRunningThis = runningTestId === test.id;

                      return (
                        <div
                          key={test.id}
                          onClick={() => setSelectedTestId(test.id)}
                          className={`p-3 transition-all cursor-pointer flex items-start justify-between gap-3 ${
                            isSelected
                              ? 'bg-amber-50/70 border-l-4 border-amber-500'
                              : 'hover:bg-slate-50/80'
                          }`}
                        >
                          <div className="space-y-1 min-w-0 flex-1">
                            <div className="flex items-center gap-2">
                              {isRunningThis ? (
                                <Sparkles className="w-3.5 h-3.5 text-amber-600 animate-spin shrink-0" />
                              ) : test.status === 'passed' ? (
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                              ) : test.status === 'failed' ? (
                                <XCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />
                              ) : (
                                <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                              )}
                              <h4 className={`text-xs font-bold truncate ${
                                isSelected ? 'text-amber-950 font-mono' : 'text-slate-800 font-mono'
                              }`}>
                                {test.name}
                              </h4>
                            </div>

                            <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed pl-5">
                              {test.description}
                            </p>

                            <div className="flex items-center gap-1.5 pl-5 pt-1 flex-wrap">
                              {test.tags.map(t => (
                                <span key={t} className="text-[9px] font-mono px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded">
                                  #{t}
                                </span>
                              ))}
                              <span className="text-[10px] font-mono text-slate-400 ml-auto">
                                {test.durationMs} мс
                              </span>
                            </div>
                          </div>

                          {/* Quick Run single button */}
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleRunSingleTest(test.id);
                            }}
                            title="Выполнить только этот тест"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-amber-700 hover:bg-amber-100 transition-colors shrink-0"
                          >
                            <Play className="w-3.5 h-3.5 fill-current" />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* RIGHT COLUMN: BEAUTIFUL HTML REPORT OF TEST EXECUTION LOGS */}
        <div className="lg:col-span-7 space-y-3">
          {/* HTML Viewer Toolbar */}
          <div className="bg-white rounded-2xl border border-slate-200/90 p-3 shadow-xs flex items-center justify-between gap-3">
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setViewMode('rendered_html')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                  viewMode === 'rendered_html'
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Eye className="w-3.5 h-3.5" />
                <span>HTML Отчёт (Рендеринг)</span>
              </button>

              <button
                onClick={() => setViewMode('source_html')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                  viewMode === 'source_html'
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Code2 className="w-3.5 h-3.5" />
                <span>Исходный HTML код</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={copyHtmlReport}
                className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium transition-colors"
                title="Скопировать весь HTML отчет"
              >
                {copiedHtml ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                <span className="hidden sm:inline">{copiedHtml ? 'Скопировано' : 'Копировать'}</span>
              </button>

              <button
                onClick={downloadHtmlFile}
                className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-900 text-xs font-bold transition-colors"
                title="Скачать автономный HTML файл"
              >
                <Download className="w-3.5 h-3.5 text-amber-700" />
                <span className="hidden sm:inline">Скачать .html</span>
              </button>
            </div>
          </div>

          {/* Report Display: Rendered in Iframe or Source */}
          {viewMode === 'rendered_html' ? (
            <div className="bg-slate-950 rounded-2xl border border-slate-800 shadow-md overflow-hidden min-h-[640px] flex flex-col">
              {/* Browser window simulation frame header */}
              <div className="bg-slate-900 px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400 select-none">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-rose-500/80" />
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-500/80" />
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500/80" />
                  </div>
                  <span className="font-mono text-[11px] text-slate-300 ml-2">
                    report://specter-1c-runner/{selectedTest.name}.html
                  </span>
                </div>
                <span className="font-mono text-[10px] text-amber-400 bg-amber-950/60 px-2 py-0.5 rounded border border-amber-800/60">
                  LIVE HTML ENGINE
                </span>
              </div>

              {/* The Live Rendered HTML Document */}
              <iframe
                ref={iframeRef}
                title="1C Test Execution HTML Report"
                className="w-full flex-1 border-0 min-h-[600px] bg-[#0f172a]"
                sandbox="allow-same-origin"
              />
            </div>
          ) : (
            <div className="bg-slate-950 rounded-2xl border border-slate-800 overflow-hidden shadow-md">
              <div className="bg-slate-900 px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs font-mono text-slate-300">
                <span>report-template.html</span>
                <span className="text-[10px] text-slate-400">Сгенерированный разметчик логов</span>
              </div>
              <div className="p-4 overflow-x-auto text-xs font-mono leading-relaxed text-emerald-300/90 max-h-[600px]">
                <pre>{htmlReport}</pre>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
