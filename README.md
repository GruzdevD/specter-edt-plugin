<p align="center">
  <img src="bundles/ru.ozon.uitp.e2e/icons/specter-emblem.png" width="96" height="96" alt="Specter Logo" />
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
```

---

## 📄 Лицензия

[EPL-2.0](LICENSE)
