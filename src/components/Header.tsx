import React, { useState } from 'react';
import { ShieldCheck, GitBranch, ExternalLink, CheckCircle2, AlertTriangle, Bug, Copy, Check, Download, Layers, Sparkles, Terminal, Layout, AppWindow } from 'lucide-react';

interface HeaderProps {
  activeTab: 'overview' | 'issues' | 'files' | 'simulation' | 'design' | 'forms1c';
  setActiveTab: (tab: 'overview' | 'issues' | 'files' | 'simulation' | 'design' | 'forms1c') => void;
  issuesCount: number;
  criticalCount: number;
  onOpenExport: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  setActiveTab,
  issuesCount,
  criticalCount,
  onOpenExport
}) => {
  const [copiedBundle, setCopiedBundle] = useState(false);

  const copyBundleId = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText('ru.ozon.uitp.e2e');
    setCopiedBundle(true);
    setTimeout(() => setCopiedBundle(false), 2000);
  };

  return (
    <header className="border-b border-slate-200/90 bg-white/95 backdrop-blur-md sticky top-0 z-40 shadow-xs transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20 gap-4">
          {/* Logo and branding */}
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="relative group shrink-0">
              <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-amber-500 via-rose-500 to-indigo-600 p-0.5 shadow-md shadow-rose-500/10 group-hover:scale-105 transition-transform duration-200">
                <div className="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center text-amber-400 font-extrabold text-lg tracking-tight shadow-inner">
                  1С
                </div>
              </div>
              <span className="absolute -bottom-1 -right-1 flex h-3.5 w-3.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3.5 w-3.5 bg-emerald-500 border-2 border-white"></span>
              </span>
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-base sm:text-lg font-bold text-slate-900 tracking-tight flex items-center gap-2">
                  <span>Specter</span>
                  <span className="text-slate-400 font-normal">/</span>
                  <span className="text-indigo-600 font-semibold text-sm sm:text-base">UIxUnit 1C:EDT</span>
                </h1>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/80 shadow-2xs">
                  <ShieldCheck className="w-3.5 h-3.5 mr-1 text-emerald-600" />
                  EDT_PLUGIN_DEV Verified
                </span>
              </div>
              
              <div className="text-xs text-slate-500 flex items-center gap-2 mt-0.5 flex-wrap">
                <button
                  onClick={copyBundleId}
                  title="Нажмите, чтобы скопировать ID бандла"
                  className="group inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200/80 text-slate-700 text-[11px] font-mono transition-colors"
                >
                  <Layers className="w-3 h-3 text-slate-500 group-hover:text-indigo-600" />
                  <span>ru.ozon.uitp.e2e</span>
                  {copiedBundle ? (
                    <Check className="w-3 h-3 text-emerald-600" />
                  ) : (
                    <Copy className="w-2.5 h-2.5 text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity" />
                  )}
                </button>
                <span className="text-slate-300">•</span>
                <a 
                  href="https://github.com/GruzdevD/specter-edt-plugin" 
                  target="_blank" 
                  rel="noreferrer"
                  className="text-slate-600 hover:text-indigo-600 text-[11px] inline-flex items-center gap-1 font-medium transition-colors hover:underline"
                >
                  <GitBranch className="w-3 h-3" />
                  <span>GruzdevD/specter-edt-plugin</span>
                  <ExternalLink className="w-2.5 h-2.5 ml-0.5 text-slate-400" />
                </a>
              </div>
            </div>
          </div>

          {/* Navigation and Actions */}
          <div className="flex items-center gap-3 shrink-0">
            <nav className="flex items-center gap-1 bg-slate-100/90 p-1.5 rounded-2xl border border-slate-200/60 shadow-inner">
              <button
                id="tab-overview"
                onClick={() => setActiveTab('overview')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'overview'
                    ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Sparkles className={`w-3.5 h-3.5 ${activeTab === 'overview' ? 'text-indigo-600' : 'text-slate-400'}`} />
                <span>Сводка</span>
              </button>

              <button
                id="tab-issues"
                onClick={() => setActiveTab('issues')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'issues'
                    ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Bug className={`w-3.5 h-3.5 ${activeTab === 'issues' ? 'text-rose-600' : 'text-slate-400'}`} />
                <span>Ошибки</span>
                <span className="px-1.5 py-0.5 bg-rose-100 text-rose-700 font-mono rounded-md text-[10px] font-bold">
                  {issuesCount}
                </span>
              </button>

              <button
                id="tab-files"
                onClick={() => setActiveTab('files')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'files'
                    ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Layers className={`w-3.5 h-3.5 ${activeTab === 'files' ? 'text-emerald-600' : 'text-slate-400'}`} />
                <span>Патчи / Diff</span>
                <span className="px-1.5 py-0.5 bg-emerald-100 text-emerald-800 font-mono rounded-md text-[10px] font-bold">
                  8 файлов
                </span>
              </button>

              <button
                id="tab-simulation"
                onClick={() => setActiveTab('simulation')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'simulation'
                    ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Terminal className={`w-3.5 h-3.5 ${activeTab === 'simulation' ? 'text-indigo-600' : 'text-slate-400'}`} />
                <span>Песочница EDT</span>
              </button>

              <button
                id="tab-forms1c"
                onClick={() => setActiveTab('forms1c')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'forms1c'
                    ? 'bg-amber-500 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <AppWindow className={`w-3.5 h-3.5 ${activeTab === 'forms1c' ? 'text-white' : 'text-amber-500'}`} />
                <span>1С Тесты & HTML Отчёт</span>
                <span className={`px-1.5 py-0.5 rounded-md text-[10px] font-bold ${
                  activeTab === 'forms1c' ? 'bg-amber-600 text-white' : 'bg-amber-100 text-amber-900'
                }`}>
                  Live
                </span>
              </button>

              <button
                id="tab-design"
                onClick={() => setActiveTab('design')}
                className={`relative px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 flex items-center gap-1.5 ${
                  activeTab === 'design'
                    ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Layout className={`w-3.5 h-3.5 ${activeTab === 'design' ? 'text-emerald-600' : 'text-slate-400'}`} />
                <span>Дизайн До/После</span>
                <span className="px-1.5 py-0.5 bg-emerald-100 text-emerald-800 font-mono rounded-md text-[10px] font-bold">
                  New
                </span>
              </button>
            </nav>

            <button
              onClick={onOpenExport}
              className="hidden sm:inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 active:scale-[0.98] text-white text-xs font-semibold shadow-sm transition-all duration-150 border border-slate-800"
            >
              <Download className="w-3.5 h-3.5 text-slate-300" />
              <span>Экспорт файлов</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
