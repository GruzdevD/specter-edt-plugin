import React, { useState } from 'react';
import { PROJECT_FILES } from '../data/reviewData';
import { ProjectFileInfo } from '../types';
import { 
  Copy, 
  Check, 
  Download, 
  FileCode, 
  CheckCircle2, 
  AlertCircle, 
  GitCommit, 
  Split, 
  FileText, 
  Sparkles,
  ExternalLink,
  Search
} from 'lucide-react';

interface DiffViewerProps {
  selectedFilePath: string;
  onSelectFilePath: (path: string) => void;
}

export const DiffViewer: React.FC<DiffViewerProps> = ({
  selectedFilePath,
  onSelectFilePath
}) => {
  const [viewMode, setViewMode] = useState<'fixed' | 'diff' | 'original'>('fixed');
  const [copied, setCopied] = useState(false);
  const [copiedPatch, setCopiedPatch] = useState(false);
  const [fileFilter, setFileFilter] = useState('');

  const currentFile: ProjectFileInfo = 
    PROJECT_FILES.find(f => f.path === selectedFilePath) || PROJECT_FILES[0];

  const handleCopy = () => {
    const textToCopy = viewMode === 'original' ? currentFile.originalCode : currentFile.fixedCode;
    navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCopyPatch = () => {
    const patch = `--- a/${currentFile.path}
+++ b/${currentFile.path}
@@ -1,50 +1,50 @@
${currentFile.diffSummary}
`;
    navigator.clipboard.writeText(currentFile.fixedCode);
    setCopiedPatch(true);
    setTimeout(() => setCopiedPatch(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([currentFile.fixedCode], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = currentFile.title;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const filteredFiles = PROJECT_FILES.filter(f => 
    f.title.toLowerCase().includes(fileFilter.toLowerCase()) ||
    f.path.toLowerCase().includes(fileFilter.toLowerCase())
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      {/* File Navigation Sidebar */}
      <div className="lg:col-span-4 space-y-3">
        <div className="flex items-center justify-between px-1">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">
            Файлы плагина ({PROJECT_FILES.length})
          </h3>
          <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
            Все исправлены
          </span>
        </div>

        {/* Filter Input */}
        <div className="relative">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={fileFilter}
            onChange={(e) => setFileFilter(e.target.value)}
            placeholder="Фильтр по файлам..."
            className="w-full pl-8 pr-3 py-1.5 text-xs rounded-xl bg-white border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-slate-800"
          />
        </div>

        <div className="space-y-2">
          {filteredFiles.map((file) => {
            const isSelected = file.path === currentFile.path;
            return (
              <div
                key={file.path}
                onClick={() => onSelectFilePath(file.path)}
                className={`p-3.5 rounded-2xl border cursor-pointer transition-all duration-150 ${
                  isSelected
                    ? 'bg-indigo-50/80 border-indigo-300 shadow-sm ring-2 ring-indigo-500/20'
                    : 'bg-white border-slate-200/80 hover:bg-slate-50/80 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <FileCode className={`w-4 h-4 shrink-0 ${isSelected ? 'text-indigo-600' : 'text-slate-400'}`} />
                    <span className="font-mono text-xs font-bold text-slate-900 truncate">
                      {file.title}
                    </span>
                  </div>
                  {file.status === 'critical_fix' ? (
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200 shrink-0">
                      P0 Fix
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200 shrink-0">
                      Обновлен
                    </span>
                  )}
                </div>
                <p className="text-[11px] text-slate-600 mt-1.5 line-clamp-2 leading-relaxed">{file.description}</p>
                <div className="mt-2 text-[10px] text-slate-400 font-mono truncate">{file.path}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Code Viewer */}
      <div className="lg:col-span-8 bg-white rounded-2xl border border-slate-200/90 shadow-xs flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-200/80 bg-slate-50/60 flex flex-wrap items-center justify-between gap-3">
          <div className="min-w-0 max-w-md">
            <div className="flex items-center gap-2.5 flex-wrap">
              <div className="p-1.5 rounded-lg bg-indigo-100 text-indigo-700">
                <FileCode className="w-4 h-4" />
              </div>
              <h2 className="text-sm font-bold text-slate-900 font-mono tracking-tight">{currentFile.title}</h2>
              {currentFile.status === 'critical_fix' && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                  P0 Blocker Fixed
                </span>
              )}
            </div>
            <p className="text-xs text-slate-600 mt-1 line-clamp-2 leading-relaxed">{currentFile.diffSummary}</p>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {/* View Mode Tabs */}
            <div className="bg-slate-200/70 p-1 rounded-xl flex text-xs font-semibold">
              <button
                onClick={() => setViewMode('fixed')}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  viewMode === 'fixed' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Исправленный
              </button>
              <button
                onClick={() => setViewMode('diff')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1 ${
                  viewMode === 'diff' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Split className="w-3 h-3" />
                <span>Side-by-Side</span>
              </button>
              <button
                onClick={() => setViewMode('original')}
                className={`px-3 py-1.5 rounded-lg transition-all ${
                  viewMode === 'original' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Оригинал
              </button>
            </div>

            <button
              onClick={handleCopy}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 active:scale-[0.97] text-white text-xs font-semibold shadow-xs transition-all"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Скопировано!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5 text-slate-300" />
                  <span>Копировать код</span>
                </>
              )}
            </button>

            <button
              onClick={handleDownload}
              title="Скачать готовый файл"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-semibold shadow-2xs transition-colors"
            >
              <Download className="w-3.5 h-3.5 text-slate-500" />
              <span>Скачать</span>
            </button>
          </div>
        </div>

        {/* Code Content */}
        <div className="p-4 flex-1 overflow-auto max-h-[640px] bg-slate-950 text-slate-100 font-mono text-xs leading-relaxed">
          {viewMode === 'diff' ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="rounded-xl border border-rose-950 bg-rose-950/20 p-3 overflow-hidden">
                <div className="text-[11px] text-rose-400 font-bold mb-2 pb-1.5 border-b border-rose-900/60 flex items-center justify-between">
                  <span>--- Исходный код (с дефектом)</span>
                  <span className="text-[10px] text-rose-500 font-normal">Original</span>
                </div>
                <pre className="whitespace-pre overflow-x-auto text-[11px] text-slate-300 leading-relaxed font-mono">
                  {currentFile.originalCode}
                </pre>
              </div>
              <div className="rounded-xl border border-emerald-950 bg-emerald-950/20 p-3 overflow-hidden">
                <div className="text-[11px] text-emerald-400 font-bold mb-2 pb-1.5 border-b border-emerald-900/60 flex items-center justify-between">
                  <span>+++ Исправленный код (EDT_PLUGIN_DEV)</span>
                  <span className="text-[10px] text-emerald-500 font-normal">Fixed</span>
                </div>
                <pre className="whitespace-pre overflow-x-auto text-[11px] text-emerald-200 leading-relaxed font-mono">
                  {currentFile.fixedCode}
                </pre>
              </div>
            </div>
          ) : (
            <div className="rounded-xl p-2">
              <pre className="whitespace-pre overflow-x-auto text-[11px] text-slate-200 leading-relaxed font-mono">
                {viewMode === 'original' ? currentFile.originalCode : currentFile.fixedCode}
              </pre>
            </div>
          )}
        </div>

        {/* Footer info bar */}
        <div className="p-3.5 bg-slate-50/90 border-t border-slate-200 text-xs text-slate-600 flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2 font-mono text-[11px] text-slate-500">
            <span>{currentFile.path}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-emerald-700 font-semibold text-[11px] flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              Готов к компиляции и сборке в Maven/Tycho 4.30+
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

