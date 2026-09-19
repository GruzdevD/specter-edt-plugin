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
  const [activeTab, setActiveTab] = useState<'overview' | 'issues' | 'files' | 'simulation' | 'design' | 'forms1c'>('design');
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
        {/* Quick Action Navigation & Status Banner */}
        <div className="mb-6 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200/90 shadow-xs flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 p-0.5 shrink-0 flex items-center justify-center shadow-xs">
              <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center p-1.5">
                <img src="/icons/specter-emblem.svg" alt="Specter" className="w-full h-full object-contain" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-sm sm:text-base font-bold text-slate-900 tracking-tight">
                  Specter 1C:EDT & 1C:Enterprise E2E Testing Platform
                </h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                  Релиз v0.3.2
                </span>
                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-600" />
                  Build Passing (Tycho 5.0.4)
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
                Интегрированная среда: Единая консоль EDT, Конвертер Vanessa Automation (.feature → BSL), Сканер расширений, Gutter-запуск и Live HTML-отчеты.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-wrap self-start lg:self-center">
            <button
              onClick={() => setActiveTab('design')}
              className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold border transition-all ${
                activeTab === 'design'
                  ? 'bg-indigo-600 text-white border-indigo-700 shadow-xs'
                  : 'bg-indigo-50 hover:bg-indigo-100 text-indigo-800 border-indigo-200'
              }`}
            >
              <Layout className="w-3.5 h-3.5" />
              <span>Воркбенч EDT</span>
            </button>

            <button
              onClick={() => setActiveTab('forms1c')}
              className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold border transition-all ${
                activeTab === 'forms1c'
                  ? 'bg-amber-500 text-white border-amber-600 shadow-xs'
                  : 'bg-amber-50 hover:bg-amber-100 text-amber-800 border-amber-200'
              }`}
            >
              <AppWindow className="w-3.5 h-3.5" />
              <span>1С Студия & Отчёт</span>
            </button>

            <button
              onClick={() => setActiveTab('overview')}
              className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold border transition-all ${
                activeTab === 'overview'
                  ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-indigo-500" />
              <span>Аудит & Архитектура</span>
            </button>

            <button
              onClick={() => setIsExportModalOpen(true)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 active:scale-[0.97] text-white text-xs font-bold shadow-xs transition-all border border-slate-800"
            >
              <Download className="w-3.5 h-3.5 text-slate-300" />
              <span>Скачать v0.3.2</span>
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'design' && (
          <DesignShowcase />
        )}

        {activeTab === 'forms1c' && (
          <Forms1CStudio />
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
            <span>Eclipse RCP / OSGi Plugin Suite v0.3.2</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="font-mono text-[11px] text-slate-400">Tycho 5.0.4 • JDK 17 • Eclipse 4.30+</span>
            <span className="text-slate-300">•</span>
            <a 
              href="https://github.com/GruzdevD/specter-edt-plugin/releases/tag/v0.3.2" 
              target="_blank" 
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 font-semibold text-[11px] hover:underline"
            >
              <Download className="w-3.5 h-3.5" />
              GitHub Release v0.3.2
            </a>
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

