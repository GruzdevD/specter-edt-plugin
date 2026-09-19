import React, { useState } from 'react';
import { Header } from './components/Header';
import { ReviewSummary } from './components/ReviewSummary';
import { ErrorList } from './components/ErrorList';
import { DiffViewer } from './components/DiffViewer';
import { SimulationSandbox } from './components/SimulationSandbox';
import { DesignShowcase } from './components/DesignShowcase';
import { Forms1CStudio } from './components/Forms1CStudio';
import { ExportModal } from './components/ExportModal';
import { CODE_ISSUES } from './data/reviewData';
import { Download, CheckCircle2, ShieldCheck, Sparkles, Terminal, ArrowRight, Layout, AppWindow } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'overview' | 'issues' | 'files' | 'simulation' | 'design' | 'forms1c'>('forms1c');
  const [selectedFilePath, setSelectedFilePath] = useState<string>(
    'bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/views/BridgeRunner.java'
  );
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);

  const handleSelectFileFromIssues = (filePath: string) => {
    setSelectedFilePath(filePath);
    setActiveTab('files');
  };

  const criticalIssuesCount = CODE_ISSUES.filter(i => i.severity === 'CRITICAL').length;

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col font-sans antialiased selection:bg-indigo-500 selection:text-white">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        issuesCount={CODE_ISSUES.length}
        criticalCount={criticalIssuesCount}
        onOpenExport={() => setIsExportModalOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Quick Action Banner */}
        <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200/90 shadow-xs">
          <div className="flex items-center gap-3.5">
            <div className="p-2.5 bg-amber-50 text-amber-700 rounded-xl border border-amber-200 shrink-0">
              <AppWindow className="w-5 h-5 text-amber-600" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-sm font-bold text-slate-900 tracking-tight">
                  Консоль тестирования 1С-расширения СП_Тестирование
                </h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-900 border border-amber-300">
                  Версия 0.2.0 (СП_ Движок)
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5 max-w-2xl leading-relaxed">
                Слева — динамически обнаруженные тесты проектов разработчиков, справа — визуальный HTML-документ с журналом Vanessa-шагов, миллисекундами и диагностикой.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0 self-start sm:self-center">
            <button
              onClick={() => setActiveTab('forms1c')}
              className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold border transition-colors ${
                activeTab === 'forms1c'
                  ? 'bg-amber-500 text-white border-amber-600 shadow-xs'
                  : 'bg-amber-50 hover:bg-amber-100 text-amber-800 border-amber-200'
              }`}
            >
              <AppWindow className="w-3.5 h-3.5" />
              <span>1С Тесты & HTML Отчёт</span>
            </button>
            <button
              onClick={() => setActiveTab('design')}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold border border-indigo-200 transition-colors"
            >
              <Layout className="w-3.5 h-3.5" />
              <span>Плагин EDT</span>
            </button>
            <button
              onClick={() => setIsExportModalOpen(true)}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 active:scale-[0.97] text-white text-xs font-bold shadow-xs transition-all"
            >
              <Download className="w-3.5 h-3.5 text-slate-300" />
              <span>Релиз v0.2.0</span>
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'forms1c' && (
          <Forms1CStudio />
        )}

        {activeTab === 'design' && (
          <DesignShowcase />
        )}

        {activeTab === 'overview' && (
          <ReviewSummary
            onSelectTab={setActiveTab}
            onSelectIssue={() => setActiveTab('issues')}
          />
        )}

        {activeTab === 'issues' && (
          <ErrorList onSelectFile={handleSelectFileFromIssues} />
        )}

        {activeTab === 'files' && (
          <DiffViewer
            selectedFilePath={selectedFilePath}
            onSelectFilePath={setSelectedFilePath}
          />
        )}

        {activeTab === 'simulation' && (
          <SimulationSandbox />
        )}
      </main>

      <footer className="border-t border-slate-200/80 bg-white py-5 mt-14">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div className="flex items-center gap-2.5">
            <img src="/icons/specter-emblem.svg" alt="Specter" className="w-4 h-4 object-contain" />
            <span className="font-bold text-slate-800">Specter for 1C:EDT</span>
            <span className="text-slate-300">•</span>
            <span>Eclipse RCP / OSGi Plugin Suite</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="font-mono text-[11px] text-slate-400">Tycho 5.0.4 • JDK 17 • Eclipse 4.30+</span>
            <span className="text-slate-300">•</span>
            <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold text-[11px]">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              Все замечания устранены
            </span>
          </div>
        </div>
      </footer>

      <ExportModal
        isOpen={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
      />
    </div>
  );
}

