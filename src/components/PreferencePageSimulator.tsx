import React, { useState } from 'react';
import { 
  Folder, 
  Settings, 
  Check, 
  RotateCcw, 
  Save, 
  FileCode, 
  Sparkles, 
  HardDrive, 
  CheckCircle2, 
  FolderOpen,
  Info,
  Layers,
  Code2,
  Zap,
  ArrowRight
} from 'lucide-react';

interface PreferencePageSimulatorProps {
  theme: 'dark' | 'light';
  onOpenConverter?: () => void;
}

export const PreferencePageSimulator: React.FC<PreferencePageSimulatorProps> = ({ 
  theme, 
  onOpenConverter 
}) => {
  const [vanessaPath, setVanessaPath] = useState<string>('/workspace/tests/vanessa-automation/features');
  const [savedPath, setSavedPath] = useState<string>('/workspace/tests/vanessa-automation/features');
  const [isSavedNotice, setIsSavedNotice] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'ui' | 'code' | 'plugin-xml'>('ui');

  const defaultPath = '/workspace/tests/vanessa-automation/features';

  const handleApply = () => {
    setSavedPath(vanessaPath);
    setIsSavedNotice(true);
    setTimeout(() => setIsSavedNotice(false), 2500);
  };

  const handleRestoreDefaults = () => {
    setVanessaPath(defaultPath);
  };

  const isDirty = vanessaPath !== savedPath;

  return (
    <div className="space-y-4">
      {/* Top Selector / Mode switcher */}
      <div className={`p-3 rounded-xl border flex flex-wrap items-center justify-between gap-2 text-xs ${
        theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-white border-slate-200 shadow-xs'
      }`}>
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800">
            <Settings className="w-4 h-4" />
          </div>
          <div>
            <div className="font-bold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
              <span>Окно: Preferences (Настройки 1C:EDT / Eclipse)</span>
              <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                org.eclipse.ui.preferencePages
              </span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Страница SpecterPreferencePage • Сохранение в IPreferenceStore плагина
            </p>
          </div>
        </div>

        <div className="flex items-center bg-slate-100 dark:bg-slate-800 p-0.5 rounded-lg text-xs font-semibold">
          <button
            onClick={() => setActiveSubTab('ui')}
            className={`px-2.5 py-1 rounded-md transition-all ${
              activeSubTab === 'ui'
                ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Интерфейс SWT
          </button>
          <button
            onClick={() => setActiveSubTab('code')}
            className={`px-2.5 py-1 rounded-md transition-all ${
              activeSubTab === 'code'
                ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            SpecterPreferencePage.java
          </button>
          <button
            onClick={() => setActiveSubTab('plugin-xml')}
            className={`px-2.5 py-1 rounded-md transition-all ${
              activeSubTab === 'plugin-xml'
                ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            plugin.xml
          </button>
        </div>
      </div>

      {activeSubTab === 'ui' && (
        <div className={`rounded-xl border shadow-sm overflow-hidden flex flex-col md:flex-row ${
          theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c]' : 'bg-white border-slate-300'
        }`}>
          {/* Left: Preferences Tree Category Sidebar */}
          <div className={`w-full md:w-64 border-b md:border-b-0 md:border-r p-3 space-y-1 text-xs select-none ${
            theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c] text-slate-300' : 'bg-[#f6f8fa] border-slate-200 text-slate-700'
          }`}>
            <div className="font-bold text-[11px] uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2 px-2">
              Категории настроек EDT
            </div>
            
            <div className="px-2 py-1.5 rounded-lg opacity-60 flex items-center gap-2">
              <span className="text-[10px]">▶</span>
              <span>General (Общие)</span>
            </div>
            <div className="px-2 py-1.5 rounded-lg opacity-60 flex items-center gap-2">
              <span className="text-[10px]">▶</span>
              <span>1C:Enterprise (1С:Предприятие)</span>
            </div>
            <div className="px-2 py-1.5 rounded-lg opacity-60 flex items-center gap-2 pl-4">
              <span className="text-[10px]">▶</span>
              <span>BSL Language & Syntax</span>
            </div>
            <div className={`px-2.5 py-2 rounded-lg font-bold flex items-center justify-between ${
              theme === 'dark' 
                ? 'bg-indigo-900/60 text-white border border-indigo-500/40' 
                : 'bg-indigo-50 text-indigo-900 border border-indigo-200'
            }`}>
              <div className="flex items-center gap-2">
                <img src="/icons/specter-emblem.svg" alt="Specter" className="w-3.5 h-3.5 object-contain" />
                <span>Specter</span>
              </div>
              <span className="text-[10px] px-1.5 py-0.2 rounded font-mono bg-indigo-200 dark:bg-indigo-800 text-indigo-900 dark:text-indigo-100">
                Активно
              </span>
            </div>
            <div className="px-2 py-1.5 rounded-lg opacity-60 flex items-center gap-2">
              <span className="text-[10px]">▶</span>
              <span>Run/Debug (Запуск/Отладка)</span>
            </div>
          </div>

          {/* Right: Preference Page Content (FieldEditorPreferencePage) */}
          <div className="flex-1 p-5 sm:p-6 flex flex-col justify-between space-y-6">
            <div className="space-y-5">
              {/* Header */}
              <div className="border-b pb-3 border-slate-200 dark:border-slate-700">
                <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <img src="/icons/specter-emblem.svg" alt="Specter" className="w-4 h-4 object-contain" />
                  <span>Specter</span>
                  <span className="text-xs font-normal text-slate-500 dark:text-slate-400">
                    (ru.ozon.uitp.e2e.preferences.SpecterPreferencePage)
                  </span>
                </h3>
                <p className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                  Параметры интеграции Specter и запуска тестовых сценариев Vanessa Automation в 1C:EDT
                </p>
              </div>

              {/* FieldEditor: DirectoryFieldEditor */}
              <div className={`p-4 rounded-xl border space-y-3 ${
                theme === 'dark' ? 'bg-[#252526] border-[#3c3c3c]' : 'bg-slate-50/80 border-slate-200'
              }`}>
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                    <FolderOpen className="w-4 h-4 text-amber-500" />
                    <span>Путь к тестам Vanessa Automation:</span>
                  </label>
                  <span className="text-[10px] font-mono text-slate-400">
                    DirectoryFieldEditor (GRID)
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      value={vanessaPath}
                      onChange={(e) => setVanessaPath(e.target.value)}
                      className={`w-full px-3 py-2 text-xs font-mono rounded-lg border outline-hidden transition-all ${
                        theme === 'dark'
                          ? 'bg-[#1e1e1e] border-[#4c4c4c] text-slate-200 focus:border-indigo-500'
                          : 'bg-white border-slate-300 text-slate-800 focus:border-indigo-500'
                      }`}
                      placeholder="/каталог/тестов/vanessa..."
                    />
                  </div>

                  <button
                    onClick={() => setVanessaPath('/workspace/custom-vanessa-tests/scenarios')}
                    className={`px-3 py-2 rounded-lg text-xs font-bold border transition-colors shrink-0 flex items-center gap-1.5 ${
                      theme === 'dark'
                        ? 'bg-[#2d2d2d] hover:bg-[#3c3c3c] border-[#4c4c4c] text-slate-200'
                        : 'bg-white hover:bg-slate-100 border-slate-300 text-slate-700'
                    }`}
                  >
                    <Folder className="w-3.5 h-3.5 text-amber-500" />
                    <span>Обзор...</span>
                  </button>
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 pt-1">
                  <span>Ключ в IPreferenceStore: <code className="font-mono font-bold text-indigo-600 dark:text-indigo-400">vanessaTestsPath</code></span>
                  <span>Текущее сохраненное значение: <code className="font-mono text-slate-700 dark:text-slate-300">{savedPath}</code></span>
                </div>
              </div>

              {/* Status note */}
              {isSavedNotice && (
                <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200 text-xs flex items-center gap-2 animate-in fade-in duration-200">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                  <span>Значение успешно сохранено в <strong>IPreferenceStore</strong> плагина (Activator.getDefault().getPreferenceStore())</span>
                </div>
              )}

              {/* Action Banner: Convert Recognized Tests */}
              {onOpenConverter && (
                <div className="p-4 rounded-xl bg-purple-50 dark:bg-purple-950/40 border border-purple-200 dark:border-purple-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
                  <div className="space-y-0.5">
                    <div className="font-bold text-purple-900 dark:text-purple-200 flex items-center gap-1.5">
                      <Zap className="w-4 h-4 text-purple-600 dark:text-purple-400" />
                      <span>Конвертация найденных тестов в модули расширения</span>
                    </div>
                    <p className="text-[11px] text-purple-700 dark:text-purple-300">
                      Плагин автоматически распознает .feature файлы по указанному пути и преобразует выбранные тесты в новые общие модули (CommonModules).
                    </p>
                  </div>
                  <button
                    onClick={onOpenConverter}
                    className="px-3.5 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-xs transition-colors shrink-0"
                  >
                    <span>Распознать и конвертировать</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {/* Info panel */}
              <div className={`p-3.5 rounded-xl border text-xs flex items-start gap-3 ${
                theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-300' : 'bg-blue-50/60 border-blue-100 text-slate-700'
              }`}>
                <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <div className="font-bold text-slate-900 dark:text-white">
                    Архитектура IPreferenceStore в 1C:EDT
                  </div>
                  <p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed">
                    Плагин сохраняет путь в workspace metadata (<code>.plugins/org.eclipse.core.runtime/.settings/ru.ozon.uitp.e2e.prefs</code>). 
                    Мост <code>BridgeRunner</code> и модуль сканирования тестов считывают этот путь через <code>Activator.getVanessaTestsPath()</code> для автоматического поиска .feature сценариев.
                  </p>
                </div>
              </div>
            </div>

            {/* Bottom buttons (Eclipse Preference Page Standard Dialog Buttons) */}
            <div className={`pt-4 border-t flex flex-wrap items-center justify-between gap-3 ${
              theme === 'dark' ? 'border-[#3c3c3c]' : 'border-slate-200'
            }`}>
              <button
                onClick={handleRestoreDefaults}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors flex items-center gap-1.5 ${
                  theme === 'dark'
                    ? 'bg-[#2d2d2d] hover:bg-[#3c3c3c] border-[#4c4c4c] text-slate-300'
                    : 'bg-white hover:bg-slate-100 border-slate-300 text-slate-700'
                }`}
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Восстановить умолчания (Restore Defaults)</span>
              </button>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleApply}
                  disabled={!isDirty}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                    isDirty
                      ? 'bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs'
                      : 'bg-slate-200 dark:bg-slate-800 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>Применить (Apply)</span>
                </button>
                <button
                  onClick={handleApply}
                  className="px-4 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white dark:bg-slate-100 dark:hover:bg-white dark:text-slate-900 text-xs font-bold shadow-xs transition-colors"
                >
                  Применить и закрыть (Apply and Close)
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeSubTab === 'code' && (
        <div className={`p-4 rounded-xl border font-mono text-xs overflow-auto ${
          theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-200' : 'bg-slate-900 text-slate-100 border-slate-800'
        }`}>
          <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-700">
            <span className="font-bold text-amber-400">SpecterPreferencePage.java</span>
            <span className="text-[11px] text-slate-400">bundles/ru.ozon.uitp.e2e/src/ru/ozon/uitp/e2e/preferences/</span>
          </div>
          <pre className="leading-relaxed">
{`package ru.ozon.uitp.e2e.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import ru.ozon.uitp.e2e.Activator;

/**
 * Страница настроек плагина Specter для 1C:EDT.
 * Наследует FieldEditorPreferencePage и реализует IWorkbenchPreferencePage.
 * Настраивает путь к тестам Vanessa Automation с сохранением в IPreferenceStore плагина.
 */
public class SpecterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "ru.ozon.uitp.e2e.preferences.SpecterPreferencePage";

	/** Ключ настройки для каталога тестов Vanessa Automation в IPreferenceStore */
	public static final String P_VANESSA_TESTS_PATH = "vanessaTestsPath";

	public SpecterPreferencePage() {
		super(GRID);
		// Привязываем страницу к IPreferenceStore нашего плагина:
		if (Activator.getDefault() != null) {
			setPreferenceStore(Activator.getDefault().getPreferenceStore());
		}
		setDescription("Параметры интеграции Specter и запуска тестовых сценариев Vanessa Automation в 1C:EDT");
	}

	@Override
	public void init(IWorkbench workbench) {
		// Гарантируем привязку IPreferenceStore при открытии диалога Preferences в воркбенче
		if (getPreferenceStore() == null && Activator.getDefault() != null) {
			setPreferenceStore(Activator.getDefault().getPreferenceStore());
		}
	}

	@Override
	protected void createFieldEditors() {
		// Поле выбора каталога "Путь к тестам Vanessa Automation"
		DirectoryFieldEditor vanessaPathEditor = new DirectoryFieldEditor(
				P_VANESSA_TESTS_PATH,
				"Путь к тестам Vanessa Automation:",
				getFieldEditorParent()
		);
		addField(vanessaPathEditor);
	}
}`}
          </pre>
        </div>
      )}

      {activeSubTab === 'plugin-xml' && (
        <div className={`p-4 rounded-xl border font-mono text-xs overflow-auto ${
          theme === 'dark' ? 'bg-[#1e1e1e] border-[#3c3c3c] text-slate-200' : 'bg-slate-900 text-slate-100 border-slate-800'
        }`}>
          <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-700">
            <span className="font-bold text-amber-400">plugin.xml (org.eclipse.ui.preferencePages)</span>
            <span className="text-[11px] text-slate-400">bundles/ru.ozon.uitp.e2e/plugin.xml</span>
          </div>
          <pre className="leading-relaxed">
{`   <!-- Страница настроек плагина Specter (Путь к тестам Vanessa Automation) -->
   <extension
         point="org.eclipse.ui.preferencePages">
      <page
            id="ru.ozon.uitp.e2e.preferences.SpecterPreferencePage"
            name="Specter"
            class="ru.ozon.uitp.e2e.preferences.SpecterPreferencePage">
      </page>
   </extension>`}
          </pre>
        </div>
      )}
    </div>
  );
};
