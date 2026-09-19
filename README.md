<p align="center">
  <img src="public/icons/specter-emblem.svg" width="120" height="120" alt="Specter Logo" />
</p>

<h1 align="center">Specter — EDT-плагин UI-тестирования 1С</h1>

<p align="center">
  <a href="https://github.com/GruzdevD/specter-edt-plugin/releases"><img src="https://img.shields.io/github/v/release/GruzdevD/specter-edt-plugin?color=6366f1&label=Release&logo=github" alt="GitHub Release"></a>
  <a href="https://github.com/GruzdevD/specter-edt-plugin/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/GruzdevD/specter-edt-plugin/build.yml?branch=master&label=Build&logo=githubactions" alt="Build Status"></a>
  <a href="https://gruzdevd.github.io/specter-edt-plugin/"><img src="https://img.shields.io/badge/p2_update--site-online-06b6d4?logo=eclipseide" alt="p2 Update Site"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-EPL--2.0-blue.svg" alt="License: EPL 2.0"></a>
</p>

<p align="center">
  <b>Specter</b> — платформа сценарного и UI-тестирования 1С изнутри сессии тонкого клиента (функциональный аналог Vanessa Automation с процессом написания тестов как в YAxUnit — без внешних .epf, видео и скриншотов).
</p>

---

## 🌟 Ключевые возможности

- 🚀 **Запуск сценариев в 1 клик**: прямо из 1C:Enterprise Development Tools (EDT) через кнопку «Запустить сценарий» или контекстное меню BSL-модулей.
- 🌲 **Интерактивные панели в EDT**:
  - **«Specter: Тесты и Результаты»** — динамическое обнаружение тестов в активном BSL-редакторе.
  - **«Тесты расширения (СП)»** — динамическое сканирование всех тестов проекта/расширений воркспейса с фильтрацией системных модулей и автообновлением при правках.
  - **«Результаты (лог)»** — дерево прогона шагов с подсветкой статусов (Passed / Failed), временем выполнения и выводом точных причин падений (включая аварийные сбои и синтаксические ошибки BSL).
- ⚡ **Прямой мост 1С**: обмен командами и результатами в JSON на лету через сессионный `runId` без ручных шагов.
- 🎯 **Умные маркеры на полях (Gutter Markers)**: быстрый запуск отдельных методов прямо со строки `Процедура Тест_*()`.

---

## Что делает кнопка «Запустить сценарий»

Кнопка запускает **единый автоматизированный контур**:

1. Генерирует уникальный `runId` и записывает командный файл моста `bridge-commands.json` (`{runId, commands:[...]}`) в каталог обмена.
2. Находит launch-конфигурацию EDT типа `com._1c.g5.v8.dt.launching.core.RuntimeClient` (например, «Тонкий клиент АУФ»).
3. Создает изолированную копию (`UITP - <имя>`) и передает параметры:
   - `ATTR_STARTUP_OPTION = "SPECTER_START_BRIDGE|outDir=<каталог обмена>"` → клиент получает ключ запуска `/C SPECTER_START_BRIDGE|outDir=...`; модуль управляемого приложения расширения `СП_Тестирование` исполняет сценарий на реальном UI;
   - `ATTR_LAUNCH_USER_NAME` / `ATTR_LAUNCH_USER_PASSWORD` → прозрачный автовход без окна авторизации.
4. Запускает тонкий клиент через `DebugUITools.launch(copy, "run")`.
5. Фоновый Job отслеживает появление `bridge-result-<runId>.json` строго по текущему `runId`. При любых сбоях (аварийное завершение процесса 1С, ошибки компиляции, таймаут) плагин формирует понятный FAILED-отчет в панели результатов.

---

## ⚙️ Конфигурация (системные свойства EDT)

Плагин не хранит учетных данных в коде. Укажите в среде EDT (`eclipse.ini` аргументами `-D`, либо в параметрах запуска):

| Свойство | Назначение |
|---|---|
| `-Duitp.e2e.outDir=<абс. путь>` | каталог обмена моста (`bridge-commands.json` / `bridge-result-<runId>.json`) |
| `-Duitp.e2e.launchUser=<логин ИБ>` | пользователь ИБ для автовхода клиента |
| `-Duitp.e2e.launchPassword=<пароль ИБ>` | пароль ИБ для автовхода клиента |

---

## 📦 Установка через UI EDT (Help → Install New Software)

> Плагин распространяется как **p2 update-site** на GitHub Pages. Установка выполняется прямо из EDT без ручного копирования JAR-файлов.

1. В EDT перейдите: **Help → Install New Software...**
2. В поле **Work with:** вставьте адрес репозитория:
   ```
   https://gruzdevd.github.io/specter-edt-plugin/
   ```
3. Отметьте категорию **«Specter»** → **Next**.
4. Нажмите **Next** → согласитесь на установку неподписанного ПО → **Finish** → **Restart Now**.

---

## 🔍 Проверка работы

1. На главной панели инструментов EDT появится кнопка **«Запустить сценарий»** (и меню **Specter → Запустить сценарий**).
2. Панели открываются через **Window → Show View → Other... → Specter**:
   - **Specter: Тесты и Результаты**
   - **Тесты расширения (СП)**
   - **Результаты (лог)**
3. При открытии любого тестового модуля (`Тесты_*`, `*_Тесты`) плагин автоматически распознает экспортные процедуры `Тест_*` и выведет маркеры быстрого запуска на полях редактора.

---

## 🛠️ Сборка проекта

Сборка выполняется через Maven + Tycho (целевая платформа `ruby/2025.2` и Eclipse 4.30, JDK 17+):

```bash
mvn -f pom.xml clean verify -T 1C
```

Собранный p2 update-site располагается в:
`repositories/ru.ozon.uitp.e2e.repository/target/repository/`

---

## 📂 Структура репозитория

```
bundles/ru.ozon.uitp.e2e/          OSGi-плагин EDT (UI, Viewers, Launchers, BSL-парсер, маркеры)
features/ru.ozon.uitp.e2e.feature/ Feature-манифест плагина
repositories/ru.ozon.uitp.e2e.repository/ p2 update-site + category.xml
targets/default/default.target     Target Platform (p2 1С ruby/2025.2 + Eclipse 4.30)
extension/СП_Тестирование/         BSL-расширение 1С (движок шагов, мост и ассерты)
tools/ci/                          Скрипт CLI-раннера (specter-cli.py) и шаблоны CI/CD (GitLab)
```

---

## 🤖 Интеграция с CI/CD и Параллельный запуск (Test Sharding)

Фреймворк **Specter** поддерживает автоматизированный безинтерфейсный (headless) запуск UI-тестов 1С в CI/CD пайплайнах (GitLab CI, GitHub Actions, Jenkins и др.) как в одиночном, так и в **параллельном** режиме (Test Sharding) для максимального ускорения проверок.

### Архитектура и изоляция файлов (`specter-cli.py`)

Тонкий клиент 1С (`1cv8c`) является однопоточным приложением. Для параллельного выполнения наборов тестов утилита `tools/ci/specter-cli.py` использует пул потоков (`ThreadPoolExecutor`) и стандартный модуль Python `tempfile`.

- **Изоляция ФС**: Каждый воркер создает собственную временную изолированную папку в системном каталоге ОС (`tempfile.TemporaryDirectory()`). Взаимодействие раннера с экземпляром 1С через `bridge-commands.json` и `bridge-result-<runId>.json` происходит внутри этой временной папки. В корне репозитория CI-раннера не создается никакого мусора — сохраняется только финальный агрегированный `junit-report.xml`.
- **Атомарность**: Запись файлов команд выполняется атомарно, исключая гонку потоков и чтение неполных JSON-структур.

---

### ⚠️ ВАЖНО: Предотвращение коллизий данных при параллельном запуске (`--workers > 1`)

При параллельном запуске нескольких экземпляров тонкого клиента 1С к одной информационной базе возникает риск **транзакционных блокировок (`Lock wait timeout`)** и конфликтов уникальности (например, попытка двух воркеров одновременно создать справочник "ООО Ромашка" с одинаковым ИНН или провести документ по одним и тем же остаткам).

**Правила написания тестов для параллельного режима:**
1. **Уникальные префиксы данных**: Генераторы тестовых данных (например, общий модуль `СП_ГенераторДанных`) обязаны использовать уникальный суффикс на основе потока, времени или случайного GUID для всех создаваемых объектов (например, `"ООО Ромашка " + Новый УникальныйИдентификатор()` или `СтрШаблон("Клиент %1", _ГлобальныйПрефиксВоркера)`).
2. **Изоляция сущностей**: Тестовые сценарии не должны зависеть от жестко зашитых константных наименований справочников, если они изменяются в ходе теста.

---

### 1. Использование параллельного CLI-раннера

Пример запуска локально или на CI/CD раннере с 4 параллельными воркерами:

```bash
python3 tools/ci/specter-cli.py \
  --workers 4 \
  --client-path "C:\Program Files\1cv8\8.3.25.1234\bin\1cv8c.exe" \
  --ib-conn "File=\"C:\1C\Bases\DemoDB\"" \
  --user "Administrator" \
  --timeout 600 \
  --report-path "junit-report.xml"
```

Аргументы командной строки `specter-cli.py`:
- `--workers <N>` — количество параллельных процессов (по умолчанию `1`).
- `--test-list <path>` — путь к JSON-файлу со списком тестовых модулей для распределения по воркерам.
- `--client-path <path>` — путь к исполняемому файлу тонкого клиента 1С (`1cv8c` / `1cv8.exe`).
- `--ib-conn <str>` — строка подключения к информационной базе 1С (`File="..."` или `Srvr="..."`).
- `--timeout <sec>` — таймаут ожидания выполнения тестов воркером (по умолчанию `300s`).
- `--report-path <path>` — путь для сохранения итогового сводного отчета JUnit XML.

---

### 2. Шаблон GitLab CI Пайплайна (`tools/ci/gitlab-ci-template.yml`)

В репозитории подготовлен эталонный шаблон [tools/ci/gitlab-ci-template.yml](tools/ci/gitlab-ci-template.yml). Скопируйте его содержимое или подключите в ваш `.gitlab-ci.yml`:

```yaml
stages:
  - test

e2e-tests-1c:
  stage: test
  image: python:3.11-slim
  tags:
    - 1c-windows-runner  # Тэг вашего раннера с установленным тонким клиентом 1С
  variables:
    SPECTER_CLIENT_PATH: "1cv8c"
    SPECTER_IB_CONN: "File=\"/var/lib/1c/infobases/test_ib\""
    WORKERS_COUNT: "4"
  script:
    - echo "Running 1C:Specter Parallel Headless Tests..."
    - python3 tools/ci/specter-cli.py \
        --workers $WORKERS_COUNT \
        --client-path "$SPECTER_CLIENT_PATH" \
        --ib-conn "$SPECTER_IB_CONN" \
        --timeout 600 \
        --report-path junit-report.xml
  artifacts:
    name: "specter-parallel-reports"
    when: always
    expire_in: 30 days
    reports:
      junit: junit-report.xml
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == "main"'
    - if: '$CI_COMMIT_BRANCH == "develop"'
```

---

### 3. Интеграция с Merge Request (GitLab UI)

Благодаря формированию стандартизированного **JUnit XML** (`reports: junit: junit-report.xml`):
- GitLab автоматически отображает интерактивный виджет **Unit Tests** прямо в интерфейсе **Merge Request**.
- Ревьюеры видят зеленые (пройденные), синие (пропущенные) и красные (упавшие) тесты без необходимости скачивать полные архивы логов.
- Детализированная трассировка шагов доступна во вкладке **Pipelines → Tests**.

---

## 📄 Лицензия

[EPL-2.0](LICENSE)
