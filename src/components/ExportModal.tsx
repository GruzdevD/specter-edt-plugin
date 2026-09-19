import React, { useState } from 'react';
import { PROJECT_FILES } from '../data/reviewData';
import { Copy, Check, Download, FileCode, CheckCircle2, GitPullRequest, X, Sparkles, Archive, Package } from 'lucide-react';

interface ExportModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ExportModal: React.FC<ExportModalProps> = ({ isOpen, onClose }) => {
  const [copiedFile, setCopiedFile] = useState<string | null>(null);
  const [copiedAllPatch, setCopiedAllPatch] = useState(false);
  const [activeTab, setActiveTab] = useState<'releases' | 'files'>('releases');

  if (!isOpen) return null;

  const handleCopy = (path: string, code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedFile(path);
    setTimeout(() => setCopiedFile(null), 2000);
  };

  const handleCopyGitPatch = () => {
    const patchHeader = `# Git Patch for Specter 1C:EDT Plugin (ru.ozon.uitp.e2e)\n# Fixes P0 runId desync, UI blocking Job API, and OSGi bundle manifests\n\n`;
    const fullPatch = patchHeader + PROJECT_FILES.map(f => {
      return `### File: ${f.path}\n\`\`\`${f.title.endsWith('.mf') ? 'manifest' : 'java'}\n${f.fixedCode}\n\`\`\`\n`;
    }).join('\n');

    navigator.clipboard.writeText(fullPatch);
    setCopiedAllPatch(true);
    setTimeout(() => setCopiedAllPatch(false), 2000);
  };

  const handleDownloadAll = () => {
    // Downloads all fixed files
    PROJECT_FILES.forEach(file => {
      const blob = new Blob([file.fixedCode], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.title;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl border border-slate-200 shadow-2xl max-w-2xl w-full max-h-[85vh] flex flex-col overflow-hidden">
              {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-indigo-50 text-indigo-700">
                <Sparkles className="w-4 h-4" />
              </div>
              <h3 className="text-base font-bold text-slate-900 tracking-tight">
                Дистрибутивы релиза v0.3.1 & Исходники Specter
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Единая версия плагина и расширения (<code className="font-mono text-indigo-600 font-bold">0.3.1</code>). Готовые архивы сборки и проверенные патчи.
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 p-2 rounded-xl hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="px-6 pt-3 pb-0 bg-slate-50/50 border-b border-slate-200/60 flex items-center gap-2">
          <button
            onClick={() => setActiveTab('releases')}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 flex items-center gap-1.5 transition-colors ${
              activeTab === 'releases'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <Archive className="w-3.5 h-3.5" />
            <span>Пакеты релиза v0.3.1 (ZIP)</span>
          </button>
          <button
            onClick={() => setActiveTab('files')}
            className={`pb-2.5 px-3 text-xs font-bold border-b-2 flex items-center gap-1.5 transition-colors ${
              activeTab === 'files'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <FileCode className="w-3.5 h-3.5" />
            <span>Файлы с исправлениями ({PROJECT_FILES.length})</span>
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-3 flex-1 bg-slate-50/50">
          {activeTab === 'releases' ? (
            <div className="space-y-3.5">
              {/* Plugin Archive Card */}
              <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-2xs hover:border-indigo-300 transition-all">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100 mt-0.5">
                      <Package className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-900">
                          UITP-EDT.v0.3.1.zip
                        </span>
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                          EDT Plugin v0.3.1
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                        Update-site и OSGi бандлы плагина (<code className="font-mono text-[11px]">ru.ozon.uitp.e2e</code>, feature, repository, targets). Включает конвертер Vanessa Automation с выбором проекта, SpecterPreferencePage и Gutter-маркеры.
                      </p>
                      <div className="flex items-center gap-3 mt-2.5 text-[11px] text-slate-400 font-mono">
                        <span>Размер: ~115 KB</span>
                        <span>•</span>
                        <span>Формат: Eclipse p2 repository</span>
                      </div>
                    </div>
                  </div>
                  <a
                    href="https://github.com/GruzdevD/specter-edt-plugin/releases/download/v0.3.1/UITP-EDT.v0.3.1.zip"
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 active:scale-[0.97] text-white text-xs font-bold shadow-xs transition-all shrink-0"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Скачать</span>
                  </a>
                </div>
              </div>

              {/* Extension Archive Card */}
              <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-2xs hover:border-emerald-300 transition-all">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-xl border border-emerald-100 mt-0.5">
                      <Archive className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-900">
                          UITP-EXTENSION._.v0.3.1.zip
                        </span>
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                          1С Расширение v0.3.1
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                        Чистый движок расширения <code className="font-mono text-[11px]">СП_Тестирование</code>: BSL-мост, <code className="font-mono text-[11px]">СП_ДействияКлиент</code>, <code className="font-mono text-[11px]">СП_ОжиданияКлиент</code>, генератор тестовых данных.
                      </p>
                      <div className="flex items-center gap-3 mt-2.5 text-[11px] text-slate-400 font-mono">
                        <span>Размер: ~58 KB</span>
                        <span>•</span>
                        <span>34 файла метаданных EDT/1C</span>
                      </div>
                    </div>
                  </div>
                  <a
                    href="https://github.com/GruzdevD/specter-edt-plugin/releases/download/v0.3.1/UITP-EXTENSION._.v0.3.1.zip"
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 active:scale-[0.97] text-white text-xs font-bold shadow-xs transition-all shrink-0"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Скачать</span>
                  </a>
                </div>
              </div>

              {/* Version Alignment Notice */}
              <div className="p-3 bg-indigo-50/60 rounded-xl border border-indigo-100/80 text-xs text-slate-600 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0" />
                <span>
                  <strong>Синхронизация версий:</strong> Версия плагина (<code className="font-mono font-semibold text-slate-800">pom.xml</code>, <code className="font-mono font-semibold text-slate-800">MANIFEST.MF</code>) строго равна версии расширения (<code className="font-mono font-semibold text-slate-800">Configuration.mdo</code>) = <strong className="text-indigo-700">0.3.1</strong>.
                </span>
              </div>
            </div>
          ) : (
            <div className="space-y-2.5">
              {PROJECT_FILES.map((file) => (
                <div
                  key={file.path}
                  className="p-3.5 bg-white rounded-2xl border border-slate-200/90 shadow-2xs flex items-center justify-between gap-3 hover:border-slate-300 transition-colors"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <FileCode className="w-4 h-4 text-indigo-600 shrink-0" />
                      <span className="font-mono text-xs font-bold text-slate-900 truncate">
                        {file.title}
                      </span>
                      {file.status === 'critical_fix' ? (
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                          P0 Blocker
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
                          Патч готов
                        </span>
                      )}
                    </div>
                    <div className="text-[11px] text-slate-500 font-mono truncate mt-1">
                      {file.path}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => handleCopy(file.path, file.fixedCode)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold active:scale-[0.97] transition-all"
                    >
                      {copiedFile === file.path ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                          <span className="text-emerald-800 font-bold">Скопировано!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5 text-slate-500" />
                          <span>Копировать</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-5 bg-white border-t border-slate-100 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <span className="text-xs text-slate-500">
            Соответствует спецификации <code className="font-mono font-semibold text-slate-800">EDT_PLUGIN_DEV.md</code>
          </span>
          <div className="flex items-center gap-2.5">
            <button
              onClick={handleCopyGitPatch}
              className="inline-flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 active:scale-[0.97] text-slate-700 text-xs font-semibold shadow-2xs transition-all"
            >
              {copiedAllPatch ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                  <span className="text-emerald-800">Патч скопирован!</span>
                </>
              ) : (
                <>
                  <GitPullRequest className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Скопировать весь патч</span>
                </>
              )}
            </button>
            <button
              onClick={handleDownloadAll}
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 active:scale-[0.97] text-white text-xs font-bold shadow-xs transition-all"
            >
              <Download className="w-3.5 h-3.5 text-slate-300" />
              <span>Скачать все файлы</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

