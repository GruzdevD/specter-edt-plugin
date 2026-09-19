import React, { useState } from 'react';
import { CODE_ISSUES } from '../data/reviewData';
import { CodeIssue, Severity, Category } from '../types';
import { 
  AlertOctagon, 
  AlertTriangle, 
  Info, 
  Check, 
  Copy, 
  FileCode, 
  ChevronDown, 
  ChevronUp, 
  ArrowRight,
  Search,
  SlidersHorizontal,
  X,
  Code2
} from 'lucide-react';

interface ErrorListProps {
  onSelectFile: (filePath: string) => void;
}

export const ErrorList: React.FC<ErrorListProps> = ({ onSelectFile }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSeverity, setSelectedSeverity] = useState<Severity | 'ALL'>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<Category | 'ALL'>('ALL');
  const [expandedIssues, setExpandedIssues] = useState<Record<string, boolean>>({
    FATAL_RUNID_DESYNC: true,
    UI_BLOCKING_FILE_IO: true,
    OSGI_MANIFEST_ERRORS: true
  });
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const toggleExpand = (id: string) => {
    setExpandedIssues(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const expandAll = () => {
    const all: Record<string, boolean> = {};
    CODE_ISSUES.forEach(i => { all[i.id] = true; });
    setExpandedIssues(all);
  };

  const collapseAll = () => {
    setExpandedIssues({});
  };

  const handleCopy = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const filteredIssues = CODE_ISSUES.filter(issue => {
    if (selectedSeverity !== 'ALL' && issue.severity !== selectedSeverity) return false;
    if (selectedCategory !== 'ALL' && issue.category !== selectedCategory) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const match = 
        issue.title.toLowerCase().includes(q) ||
        issue.filePath.toLowerCase().includes(q) ||
        issue.summary.toLowerCase().includes(q) ||
        issue.impact.toLowerCase().includes(q) ||
        issue.violation.toLowerCase().includes(q);
      if (!match) return false;
    }
    return true;
  });

  const getSeverityBadge = (severity: Severity) => {
    switch (severity) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800 border border-rose-200 shadow-2xs">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-pulse"></span>
            Критическая
          </span>
        );
      case 'MAJOR':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-100 text-amber-900 border border-amber-200">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600"></span>
            Мажорная
          </span>
        );
      case 'MODERATE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-100 text-blue-900 border border-blue-200">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
            Средняя
          </span>
        );
      case 'OPTIMIZATION':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
            <span className="w-1.5 h-1.5 rounded-full bg-slate-500"></span>
            Оптимизация
          </span>
        );
    }
  };

  return (
    <div className="space-y-5">
      {/* Search and Filters Header */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          {/* Live Search */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Поиск по названию, классу Java или описанию дефекта..."
              className="w-full pl-9 pr-8 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all placeholder:text-slate-400 text-slate-800"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
            <button
              onClick={expandAll}
              className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Развернуть все
            </button>
            <span className="text-slate-300">|</span>
            <button
              onClick={collapseAll}
              className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Свернуть
            </button>
          </div>
        </div>

        {/* Severity and Category Filters */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mr-1 text-[10px]">
              Критичность:
            </span>
            {(['ALL', 'CRITICAL', 'MAJOR', 'MODERATE', 'OPTIMIZATION'] as const).map(sev => (
              <button
                key={sev}
                onClick={() => setSelectedSeverity(sev)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all duration-150 active:scale-[0.97] ${
                  selectedSeverity === sev
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100/80 text-slate-600 hover:bg-slate-200/70 hover:text-slate-900'
                }`}
              >
                {sev === 'ALL' ? 'Все (7)' : sev === 'CRITICAL' ? 'Критические (2)' : sev === 'MAJOR' ? 'Мажорные (3)' : sev === 'MODERATE' ? 'Средние (2)' : 'Оптимизации (1)'}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <SlidersHorizontal className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value as Category | 'ALL')}
              className="text-xs font-semibold rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all cursor-pointer"
            >
              <option value="ALL">Все категории дефектов</option>
              <option value="LOGIC_AND_SYNC">Синхронизация и контракт моста</option>
              <option value="UI_THREAD_BLOCKING">UI-поток и Eclipse Job API</option>
              <option value="OSGI_DEPENDENCIES">OSGi и MANIFEST.MF</option>
              <option value="SWT_JFACE_COMPLIANCE">SWT/JFace разметка</option>
              <option value="RESOURCE_LIFECYCLE">Утечки ресурсов и память</option>
            </select>
          </div>
        </div>
      </div>

      {/* Issues List */}
      <div className="space-y-3.5">
        {filteredIssues.length === 0 ? (
          <div className="p-12 text-center bg-white rounded-2xl border border-slate-200">
            <p className="text-slate-500 text-sm">По выбранным фильтрам ошибок не найдено.</p>
            <button
              onClick={() => { setSelectedSeverity('ALL'); setSelectedCategory('ALL'); setSearchQuery(''); }}
              className="mt-3 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold"
            >
              Сбросить фильтры
            </button>
          </div>
        ) : (
          filteredIssues.map((issue) => {
            const isExpanded = !!expandedIssues[issue.id];
            return (
              <div
                key={issue.id}
                className={`bg-white rounded-2xl border transition-all duration-200 shadow-xs overflow-hidden ${
                  issue.severity === 'CRITICAL' ? 'border-rose-200/90' : 'border-slate-200/80'
                }`}
              >
                {/* Card Header */}
                <div
                  onClick={() => toggleExpand(issue.id)}
                  className="p-5 cursor-pointer flex items-start justify-between gap-4 hover:bg-slate-50/80 transition-colors"
                >
                  <div className="flex items-start gap-3.5 min-w-0">
                    <div className="mt-0.5 shrink-0">
                      {issue.severity === 'CRITICAL' ? (
                        <div className="p-2 rounded-xl bg-rose-100 text-rose-700 border border-rose-200">
                          <AlertOctagon className="w-5 h-5" />
                        </div>
                      ) : issue.severity === 'MAJOR' ? (
                        <div className="p-2 rounded-xl bg-amber-100 text-amber-800 border border-amber-200">
                          <AlertTriangle className="w-5 h-5" />
                        </div>
                      ) : (
                        <div className="p-2 rounded-xl bg-blue-100 text-blue-700 border border-blue-200">
                          <Info className="w-5 h-5" />
                        </div>
                      )}
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-2.5 flex-wrap">
                        <h4 className="text-sm sm:text-base font-bold text-slate-900 tracking-tight">
                          {issue.title}
                        </h4>
                        {getSeverityBadge(issue.severity)}
                      </div>
                      
                      <div className="mt-1.5 flex items-center gap-2 text-xs text-slate-500 font-mono flex-wrap">
                        <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 font-medium">
                          {issue.filePath}
                        </span>
                        {issue.lineNumbers && (
                          <span className="text-slate-400 font-normal">строки {issue.lineNumbers}</span>
                        )}
                      </div>

                      <p className="mt-2 text-xs text-slate-600 leading-relaxed max-w-3xl">
                        {issue.summary}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectFile(issue.filePath);
                      }}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100/80 border border-indigo-200 active:scale-[0.97] transition-all"
                    >
                      <FileCode className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">К файлу</span>
                      <ArrowRight className="w-3 h-3" />
                    </button>
                    <div className="p-1 text-slate-400 hover:text-slate-600">
                      {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                    </div>
                  </div>
                </div>

                {/* Card Body */}
                {isExpanded && (
                  <div className="px-5 pb-5 pt-2 border-t border-slate-100 space-y-4 bg-slate-50/30">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                      <div className="p-4 bg-rose-50/40 rounded-xl border border-rose-200/80">
                        <span className="font-bold text-rose-900 block mb-1 text-xs">
                          Нарушение стандарта EDT_PLUGIN_DEV:
                        </span>
                        <p className="text-slate-700 leading-relaxed mb-2.5">{issue.violation}</p>
                        
                        <span className="font-bold text-rose-900 block mb-1 text-xs">
                          Реальное воздействие (Impact):
                        </span>
                        <p className="text-rose-900/90 font-medium leading-relaxed">{issue.impact}</p>
                      </div>

                      <div className="p-4 bg-emerald-50/50 rounded-xl border border-emerald-200/80 flex flex-col justify-between">
                        <div>
                          <span className="font-bold text-emerald-900 block mb-1 text-xs">
                            Архитектурное решение (Solution):
                          </span>
                          <p className="text-slate-700 leading-relaxed">{issue.solution}</p>
                        </div>
                        <div className="mt-3 pt-2 border-t border-emerald-200/50 flex items-center justify-between text-[11px] text-emerald-800">
                          <span>Статус исправления:</span>
                          <span className="font-bold inline-flex items-center gap-1">
                            <Check className="w-3.5 h-3.5 text-emerald-600" /> Готово в патче
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Code snippet before & after */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 text-xs">
                      <div className="rounded-xl border border-rose-200 bg-white overflow-hidden shadow-2xs">
                        <div className="px-3.5 py-2 bg-rose-50 border-b border-rose-200 text-rose-900 font-bold flex items-center justify-between">
                          <span className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-rose-500"></span>
                            Исходный дефектный фрагмент
                          </span>
                          <span className="text-[10px] font-mono text-rose-700 font-normal">Before</span>
                        </div>
                        <pre className="p-3.5 font-mono text-[11px] text-slate-800 overflow-x-auto leading-relaxed max-h-72 bg-slate-900 text-slate-200">
                          {issue.originalSnippet}
                        </pre>
                      </div>

                      <div className="rounded-xl border border-emerald-200 bg-white overflow-hidden shadow-2xs">
                        <div className="px-3.5 py-2 bg-emerald-50 border-b border-emerald-200 text-emerald-900 font-bold flex items-center justify-between">
                          <span className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                            Исправленный код (Production-Ready)
                          </span>
                          <button
                            onClick={() => handleCopy(issue.id, issue.correctedSnippet)}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-white text-emerald-800 hover:bg-emerald-100 border border-emerald-300 text-[11px] font-semibold transition-colors active:scale-[0.97]"
                          >
                            {copiedId === issue.id ? (
                              <>
                                <Check className="w-3 h-3 text-emerald-600" />
                                <span>Скопировано!</span>
                              </>
                            ) : (
                              <>
                                <Copy className="w-3 h-3 text-emerald-700" />
                                <span>Копировать сниппет</span>
                              </>
                            )}
                          </button>
                        </div>
                        <pre className="p-3.5 font-mono text-[11px] overflow-x-auto leading-relaxed max-h-72 bg-slate-950 text-emerald-300">
                          {issue.correctedSnippet}
                        </pre>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

