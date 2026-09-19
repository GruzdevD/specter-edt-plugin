// Обнаруженные тесты расширения СП_Тестирование и генератор HTML-отчетов выполнения

export interface TestCase1C {
  id: string;
  name: string;
  suite: string;
  suiteTitle: string;
  moduleName: string;
  description: string;
  tags: string[];
  type: 'bsl_data' | 'ui_bridge' | 'live_form';
  status: 'passed' | 'failed' | 'running' | 'pending';
  durationMs: number;
  steps: Array<{
    num: number;
    action: string;
    target: string;
    details: string;
    status: 'passed' | 'failed' | 'running';
    durationMs: number;
    error?: string;
    expected?: string;
    actual?: string;
    codeSnippet?: string;
  }>;
  stackTrace?: string;
}

export const DISCOVERED_1C_TESTS: TestCase1C[] = [
  {
    id: 'test_inn_ul',
    name: 'ИННЮридическогоВалиден',
    suite: 'СП_Тесты_Контрагенты',
    suiteTitle: 'Набор тестов данных и контрольных сумм Контрагентов',
    moduleName: 'ОбщийМодуль.СП_Тесты_Контрагенты',
    description: 'Сгенерированный ИНН юр. лица корректен по контрольной сумме (10 цифр).',
    tags: ['данные', 'генерация', 'инн'],
    type: 'bsl_data',
    status: 'passed',
    durationMs: 42,
    steps: [
      {
        num: 1,
        action: 'generate',
        target: 'ИНН_ЮЛ',
        details: 'Вызов СП_ГенераторДанных.СлучайныйИННЮЛ()',
        status: 'passed',
        durationMs: 12,
        codeSnippet: 'ИНН = СП_ГенераторДанных.СлучайныйИННЮЛ(); // "7700000000"'
      },
      {
        num: 2,
        action: 'assert',
        target: 'КонтрольнаяСумма',
        details: 'Проверка длины строки (СтрДлина = 10) и валидности весовых коэффициентов',
        status: 'passed',
        durationMs: 18,
        codeSnippet: 'СП_Утверждения.ПроверитьИстину(ИННВалиден, "ИНН юр. лица некорректен");'
      },
      {
        num: 3,
        action: 'assert',
        target: 'Типизация',
        details: 'Проверка типа возвращаемого значения (Строка)',
        status: 'passed',
        durationMs: 12,
        codeSnippet: 'СП_Утверждения.ПроверитьРавенство(ТипЗнч(ИНН), Тип("Строка"));'
      }
    ]
  },
  {
    id: 'test_inn_fl',
    name: 'ИННФизическогоВалиден',
    suite: 'СП_Тесты_Контрагенты',
    suiteTitle: 'Набор тестов данных и контрольных сумм Контрагентов',
    moduleName: 'ОбщийМодуль.СП_Тесты_Контрагенты',
    description: 'Сгенерированный ИНН физ. лица корректен по контрольным суммам (12 цифр).',
    tags: ['данные', 'генерация'],
    type: 'bsl_data',
    status: 'passed',
    durationMs: 38,
    steps: [
      {
        num: 1,
        action: 'generate',
        target: 'ИНН_ФЛ',
        details: 'Вызов СП_ГенераторДанных.СлучайныйИННФЛ()',
        status: 'passed',
        durationMs: 14,
        codeSnippet: 'ИНН = СП_ГенераторДанных.СлучайныйИННФЛ(); // "770421737011"'
      },
      {
        num: 2,
        action: 'assert',
        target: 'КонтрольныеРазряды',
        details: 'Проверка обоих контрольных разрядов n11 и n12 по алгоритму ФНС',
        status: 'passed',
        durationMs: 24,
        codeSnippet: 'СП_Утверждения.ПроверитьИстину(КонтрольноеЧисло11 И КонтрольноеЧисло12);'
      }
    ]
  },
  {
    id: 'test_snils',
    name: 'СНИЛСВалиден',
    suite: 'СП_Тесты_Контрагенты',
    suiteTitle: 'Набор тестов данных и контрольных сумм Контрагентов',
    moduleName: 'ОбщийМодуль.СП_Тесты_Контрагенты',
    description: 'Сгенерированный СНИЛС корректен по контрольному числу ПФР.',
    tags: ['данные', 'генерация', 'снилс'],
    type: 'bsl_data',
    status: 'passed',
    durationMs: 31,
    steps: [
      {
        num: 1,
        action: 'generate',
        target: 'СНИЛС',
        details: 'Вызов СП_ГенераторДанных.СлучайныйСНИЛС()',
        status: 'passed',
        durationMs: 11
      },
      {
        num: 2,
        action: 'assert',
        target: 'ФорматСНИЛС',
        details: 'Формат 11 знаков (9 цифр + 2 контрольных)',
        status: 'passed',
        durationMs: 20
      }
    ]
  },
  {
    id: 'test_contractor_fields',
    name: 'НаборПолейКонтрагентаЗаполнен',
    suite: 'СП_Тесты_Контрагенты',
    suiteTitle: 'Набор тестов данных и контрольных сумм Контрагентов',
    moduleName: 'ОбщийМодуль.СП_Тесты_Контрагенты',
    description: 'Генератор контрагента заполняет согласованный набор реквизитов (Наименование, ИНН, КПП, Адреса).',
    tags: ['данные', 'справочник'],
    type: 'bsl_data',
    status: 'passed',
    durationMs: 65,
    steps: [
      {
        num: 1,
        action: 'setup',
        target: 'СоздатьОбъект',
        details: 'Формирование структуры тестового контрагента через генератор',
        status: 'passed',
        durationMs: 25
      },
      {
        num: 2,
        action: 'assert',
        target: 'ЗаполнениеРеквизитов',
        details: 'Проверка обязательных полей: Наименование, ИНН, ЮрАдрес, Код',
        status: 'passed',
        durationMs: 40
      }
    ]
  },
  {
    id: 'test_assert_types',
    name: 'УтверждениеРавенстваТипизировано',
    suite: 'СП_Тесты_Контрагенты',
    suiteTitle: 'Набор тестов данных и контрольных сумм Контрагентов',
    moduleName: 'ОбщийМодуль.СП_Тесты_Контрагенты',
    description: 'assert-библиотека корректно различает типы (числа/строки/булево) без неявного приведения 1С.',
    tags: ['единицы', 'assert'],
    type: 'bsl_data',
    status: 'passed',
    durationMs: 28,
    steps: [
      {
        num: 1,
        action: 'test',
        target: 'СтрокаПротивЧисла',
        details: 'СП_Утверждения.ПроверитьРавенство("100", 100) -> ожидаемый Mismatch типов',
        status: 'passed',
        durationMs: 14
      },
      {
        num: 2,
        action: 'test',
        target: 'БулевоПротивЧисла',
        details: 'Проверка изоляции Ложь <> 0',
        status: 'passed',
        durationMs: 14
      }
    ]
  },
  {
    id: 'test_ui_card_workflow',
    name: 'ОткрытьКарточкуЗаписатьИВыйти',
    suite: 'СП_Тесты_КонтрагентыКарточка',
    suiteTitle: 'UI-сценарии моста Specter (Тестовый клиент 1С)',
    moduleName: 'ОбщийМодуль.СП_Тесты_КонтрагентыКарточка',
    description: 'UI-кейс: войти (api/1), открыть список Контрагентов, открыть карточку, изменить ИНН, нажать «Записать», проверить модифицированность, закрыть.',
    tags: ['ui', 'карточка', 'мост', 'specter'],
    type: 'ui_bridge',
    status: 'passed',
    durationMs: 340,
    steps: [
      {
        num: 1,
        action: 'openList',
        target: 'Справочник.Контрагенты.ФормаСписка',
        details: 'Агент моста выполнил открытие списка контрагентов',
        status: 'passed',
        durationMs: 65,
        codeSnippet: 'Команды.Добавить(Новый Структура("action, target", "openList", "Список"));'
      },
      {
        num: 2,
        action: 'openCard',
        target: 'Справочник.Контрагенты.ФормаЭлемента',
        details: 'Открыта форма элемента «ООО Озон Ритейл Тест»',
        status: 'passed',
        durationMs: 95,
        codeSnippet: 'Команды.Добавить(Новый Структура("action, target", "openCard", "ФормаЭлемента"));'
      },
      {
        num: 3,
        action: 'setValue',
        target: 'ИНН',
        details: 'Ввод нового ИНН "7701234567" в управляемое поле',
        status: 'passed',
        durationMs: 45,
        codeSnippet: 'Команды.Добавить(Новый Структура("action, target, value", "setValue", "ИНН", "7701234567"));'
      },
      {
        num: 4,
        action: 'assertValue',
        target: 'ИНН',
        details: 'Проверка фактического значения поля в UI (ожидалось: 7701234567)',
        status: 'passed',
        durationMs: 30
      },
      {
        num: 5,
        action: 'click',
        target: 'КоманднаяПанель.Записать',
        details: 'Имитация клика мыши по кнопке командной панели формы',
        status: 'passed',
        durationMs: 60,
        codeSnippet: 'Команды.Добавить(Новый Структура("action, target, kind", "click", "Записать", "command"));'
      },
      {
        num: 6,
        action: 'assert',
        target: 'Форма.Модифицированность',
        details: 'Проверка свойства формы: Модифицированность = Ложь (успешно записано в БД)',
        status: 'passed',
        durationMs: 45,
        codeSnippet: 'Команды.Добавить(Новый Структура("action, property, expected", "assert", "Модифицированность", "false"));'
      }
    ]
  },
  {
    id: 'test_live_programmatic',
    name: 'LiveСценарий_ОткрытиеИЗапись',
    suite: 'СП_УправлениеФормами',
    suiteTitle: 'Обработка выполнения сценариев (Live AFM runner)',
    moduleName: 'Обработка.СП_УправлениеФормами.ОсновнаяФорма',
    description: 'Сквозной programmatic-прогон формы Контрагента с проверкой реквизитов и сериализацией отчета в live-afm-run-raw.json.',
    tags: ['live', 'форма', 'отчет_json'],
    type: 'live_form',
    status: 'passed',
    durationMs: 215,
    steps: [
      {
        num: 1,
        action: 'setup',
        target: 'УстановитьДанные',
        details: 'Подготовка тестового контрагента "TEST-UL-0001" в базе',
        status: 'passed',
        durationMs: 55
      },
      {
        num: 2,
        action: 'openForm',
        target: 'ФормаСписка.Открыть()',
        details: 'Открытие управляемой формы списка',
        status: 'passed',
        durationMs: 70
      },
      {
        num: 3,
        action: 'saveReport',
        target: 'live-afm-run-raw.json',
        details: 'Сериализация массива шагов и сохранение на диск хоста',
        status: 'passed',
        durationMs: 90
      }
    ]
  },
  {
    id: 'test_form_title_sync',
    name: 'Проверка_ДинамическогоЗаголовка',
    suite: 'СП_УправлениеФормами',
    suiteTitle: 'Обработка выполнения сценариев (Live AFM runner)',
    moduleName: 'Обработка.СП_УправлениеФормами.ОсновнаяФорма',
    description: 'Проверка динамической индикации прогресса в Заголовок управляемой формы (без модальных окон).',
    tags: ['live', 'ui_заголовок'],
    type: 'live_form',
    status: 'passed',
    durationMs: 48,
    steps: [
      {
        num: 1,
        action: 'checkTitle',
        target: 'ЭтаФорма.Заголовок',
        details: 'Заголовок обновлен: "LIVE: setup OK | waitFor: Список OK"',
        status: 'passed',
        durationMs: 48
      }
    ]
  }
];

// Helper to generate self-contained HTML document string with rich CSS styling
export function generateTestHtmlReport(
  selectedTest: TestCase1C | null,
  allTests: TestCase1C[],
  isFailureDemo: boolean = false
): string {
  const passedCount = isFailureDemo ? allTests.length - 1 : allTests.length;
  const failedCount = isFailureDemo ? 1 : 0;
  const totalDuration = allTests.reduce((acc, t) => acc + t.durationMs, 0);

  const testToDisplay = selectedTest || allTests[0];

  return `<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <style>
    :root {
      --bg-primary: #0f172a;
      --bg-card: #1e293b;
      --bg-accent: #334155;
      --text-main: #f8fafc;
      --text-muted: #94a3b8;
      --1c-yellow: #ffcb05;
      --1c-yellow-dark: #e5b600;
      --color-pass: #10b981;
      --color-fail: #ef4444;
      --color-warn: #f59e0b;
      --color-info: #3b82f6;
      --border-color: rgba(255, 255, 255, 0.1);
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      background-color: var(--bg-primary);
      color: var(--text-main);
      padding: 24px;
      font-size: 13px;
      line-height: 1.6;
    }
    .header-banner {
      background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
      border: 1px solid var(--border-color);
      border-left: 5px solid var(--1c-yellow);
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.3);
    }
    .badge-1c {
      display: inline-block;
      background-color: var(--1c-yellow);
      color: #111;
      font-weight: 800;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 4px;
      text-transform: uppercase;
      margin-bottom: 8px;
    }
    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
    .report-title {
      font-size: 18px;
      font-weight: 700;
      color: #fff;
    }
    .report-meta {
      font-size: 11px;
      color: var(--text-muted);
      font-family: 'JetBrains Mono', monospace;
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 12px;
      margin-bottom: 24px;
    }
    .stat-card {
      background-color: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      padding: 14px;
      text-align: center;
    }
    .stat-value {
      font-size: 22px;
      font-weight: 800;
      margin-bottom: 2px;
    }
    .stat-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-muted);
    }
    .test-detail-card {
      background-color: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;
    }
    .test-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 14px;
      margin-bottom: 16px;
    }
    .test-name {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
      font-family: 'JetBrains Mono', monospace;
    }
    .test-suite-name {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 4px;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
    }
    .status-badge.pass { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4); }
    .status-badge.fail { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); }
    .status-badge.running { background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.4); }

    .timeline {
      position: relative;
      padding-left: 28px;
      margin-top: 16px;
    }
    .timeline::before {
      content: '';
      position: absolute;
      left: 10px;
      top: 6px;
      bottom: 6px;
      width: 2px;
      background: var(--bg-accent);
    }
    .step-item {
      position: relative;
      margin-bottom: 16px;
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 12px 16px;
      transition: all 0.2s;
    }
    .step-item:hover {
      border-color: rgba(255, 255, 255, 0.25);
      background: rgba(15, 23, 42, 0.85);
    }
    .step-dot {
      position: absolute;
      left: -24px;
      top: 14px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background-color: var(--color-pass);
      border: 2px solid var(--bg-card);
      box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
    }
    .step-dot.fail {
      background-color: var(--color-fail);
      box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
    }
    .step-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }
    .step-title {
      font-weight: 600;
      color: #e2e8f0;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .step-action-tag {
      font-size: 10px;
      font-family: 'JetBrains Mono', monospace;
      padding: 1px 6px;
      border-radius: 4px;
      background: var(--bg-accent);
      color: #93c5fd;
    }
    .step-duration {
      font-size: 11px;
      color: var(--text-muted);
      font-family: 'JetBrains Mono', monospace;
    }
    .step-details {
      color: var(--text-muted);
      font-size: 12px;
    }
    .code-block {
      background: #090d16;
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 6px;
      padding: 8px 12px;
      font-family: 'JetBrains Mono', monospace;
      font-size: 11px;
      color: #cbd5e1;
      margin-top: 8px;
      overflow-x: auto;
    }
    .error-box {
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.4);
      border-radius: 8px;
      padding: 14px;
      margin-top: 14px;
      color: #fca5a5;
    }
    .error-title {
      font-weight: 700;
      color: #f87171;
      margin-bottom: 6px;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .stack-trace {
      font-family: 'JetBrains Mono', monospace;
      font-size: 11px;
      color: #fecaca;
      white-space: pre-wrap;
      line-height: 1.4;
      margin-top: 6px;
      padding-top: 6px;
      border-top: 1px dashed rgba(239, 68, 68, 0.3);
    }
    .json-protocol {
      background: #090d16;
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 14px;
      margin-top: 20px;
    }
    .json-title {
      font-size: 11px;
      font-weight: 700;
      color: var(--1c-yellow);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 8px;
    }
  </style>
</head>
<body>

  <!-- Header Banner -->
  <div class="header-banner">
    <div class="title-row">
      <div>
        <div class="badge-1c">1C:Enterprise • UITP / Specter Bridge Report</div>
        <div class="report-title">Протокол выполнения тестов расширения СП_Тестирование</div>
      </div>
      <div class="report-meta">
        <div>Сессия: api/1 (Тонкий клиент 1С)</div>
        <div>Отчёт: live-afm-run-raw.json • Версия: 0.2.0</div>
      </div>
    </div>
  </div>

  <!-- Summary Stats Grid -->
  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-value" style="color: #fff;">${allTests.length}</div>
      <div class="stat-label">Всего тестов</div>
    </div>
    <div class="stat-card">
      <div class="stat-value" style="color: var(--color-pass);">${passedCount}</div>
      <div class="stat-label">Пройдено</div>
    </div>
    <div class="stat-card">
      <div class="stat-value" style="color: ${failedCount > 0 ? 'var(--color-fail)' : 'var(--text-muted)'};">${failedCount}</div>
      <div class="stat-label">Провалено</div>
    </div>
    <div class="stat-card">
      <div class="stat-value" style="color: var(--1c-yellow);">${(totalDuration / 1000).toFixed(2)} с</div>
      <div class="stat-label">Длительность</div>
    </div>
  </div>

  <!-- Active Test Execution Card -->
  <div class="test-detail-card">
    <div class="test-header">
      <div>
        <div class="test-name">${testToDisplay.name}</div>
        <div class="test-suite-name">${testToDisplay.moduleName}</div>
        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">${testToDisplay.description}</div>
      </div>
      <div class="status-badge ${isFailureDemo ? 'fail' : 'pass'}">
        ${isFailureDemo ? '✖ Ошибка выполнения' : '✔ Пройдено успешно'}
      </div>
    </div>

    <!-- Step Timeline -->
    <div style="font-size: 12px; font-weight: 700; color: #cbd5e1; margin-bottom: 8px;">
      ХОД ВЫПОЛНЕНИЯ СЦЕНАРИЯ (Шагов: ${testToDisplay.steps.length}):
    </div>
    <div class="timeline">
      ${testToDisplay.steps.map((step, idx) => {
        const isStepFailed = isFailureDemo && idx === testToDisplay.steps.length - 1;
        return `
        <div class="step-item">
          <div class="step-dot ${isStepFailed ? 'fail' : ''}"></div>
          <div class="step-header">
            <div class="step-title">
              <span>Шаг ${step.num}:</span>
              <span class="step-action-tag">${step.action}</span>
              <span style="color: #fff; font-family: 'JetBrains Mono', monospace;">${step.target}</span>
            </div>
            <div class="step-duration">${step.durationMs} мс</div>
          </div>
          <div class="step-details">${step.details}</div>
          ${step.codeSnippet ? `<div class="code-block">${escapeHtml(step.codeSnippet)}</div>` : ''}
          ${isStepFailed ? `
            <div class="error-box">
              <div class="error-title">Исключение / Mismatch утверждения:</div>
              <div>Значение свойства «ИНН» не совпало с ожидаемым эталоном!</div>
              <div style="margin-top: 4px; font-family: monospace; font-size: 11px;">
                Ожидалось: <span style="color: #4ade80;">"7701234567"</span><br>
                Фактически: <span style="color: #f87171;">"7700000000"</span>
              </div>
              <div class="stack-trace">
Стек вызова BSL:
  ОбщийМодуль.СП_Утверждения.Модуль:128 (ВызватьИсключение)
  ОбщийМодуль.СП_Тесты_КонтрагентыКарточка.Модуль:46 (ВыполнитьКомандуМоста)
  Обработка.СП_КонсольТестов.Форма.ФормаСписка.Модуль:24 (ВыполнитьТестыНаСервере)
              </div>
            </div>
          ` : ''}
        </div>
        `;
      }).join('')}
    </div>

    <!-- JSON Bridge Protocol Payload -->
    <div class="json-protocol">
      <div class="json-title">Сгенерированный протокол Specter Bridge (JSON):</div>
      <pre style="font-family: 'JetBrains Mono', monospace; font-size: 11px; color: #a5f3fc; overflow-x: auto;">{
  "runId": "run-${Date.now().toString(16)}",
  "test": "${testToDisplay.name}",
  "suite": "${testToDisplay.suite}",
  "status": "${isFailureDemo ? 'failed' : 'passed'}",
  "durationMs": ${testToDisplay.durationMs},
  "stepsCount": ${testToDisplay.steps.length},
  "timestamp": "${new Date().toISOString()}"
}</pre>
    </div>
  </div>

</body>
</html>`;
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
