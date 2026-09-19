# Specter — EDT-плагин UI-тестирования 1С

**Specter** — платформа сценарного/UI-тестирования 1С изнутри сессии (функциональный
аналог Vanessa Automation с процессом написания тестов как в YAxUnit — без `.epf`,
видео и скриншотов). Этот EDT-плагин (OSGi, `ru.ozon.uitp.e2e`) — рабочее место:
кнопка **«Запустить сценарий»** и панели **«Тесты» / «Результаты»** программно
запускают тонкий клиент АУФ (1С) со стартовой опцией автозапуска BSL-моста на
**реальном UI**, затем читают результат (`bridge-result-<runId>.json`) и показывают
дерево прогона. Тестовый набор можно запускать прямо из окна редактирования его
общего модуля (`OZON_UI_Тесты_*` / `УИ_Тесты_*`).

## Что делает кнопка «Запустить сценарий»

Кнопка запускает **единый контур без ручного шага** (этап №5):

1. Генерирует уникальный `runId` и записывает командный файл моста
   `bridge-commands.json` (`{runId, commands:[...]}`) в каталог обмена.
2. Находит launch-конфигурацию EDT типа `com._1c.g5.v8.dt.launching.core.RuntimeClient`
   (в нашей среде — «Тонкий клиент АУФ»).
3. Копирует её (`UITP - <имя>`) и пишет в копию:
   - `ATTR_STARTUP_OPTION = "OZONUI_START_BRIDGE|outDir=<каталог обмена>"` → клиент
     получает `/C OZONUI_START_BRIDGE|outDir=...`; модуль управляемого приложения
     расширения `afm.OZON_UI` открывает ФормуСписка, чьё `ПриОткрытии` исполняет
     команды моста на реальном UI;
   - `ATTR_LAUNCH_USER_NAME` / `ATTR_LAUNCH_USER_PASSWORD` → автовход без окна входа.
4. Запускает копию через `DebugUITools.launch(copy, "run")`.
5. В фоновом Job ждёт появления **именно** `bridge-result-<runId>.json` (по runId,
   а не по числу файлов). Исход задаёт `status` результата: `"failed"` у assertions
   не превращается в успех. Аварийное завершение клиента до появления файла
   результата — ошибка, а не «нашлись старые отчёты».

## Конфигурация (обязательные системные свойства)

Плагин не хранит путей и учётных данных в коде. Укажите в среде EDT
(`eclipse.ini` аргументами `-D`, либо в строке запуска EDT) три свойства —
без них кнопка вернёт понятную ошибку:

| Свойство | Назначение |
|---|---|
| `-Duitp.e2e.outDir=<абс. путь>` | каталог обмена моста (куда пишутся `bridge-commands.json` / `bridge-result-<runId>.json`) |
| `-Duitp.e2e.launchUser=<логин ИБ>` | пользователь ИБ для автовхода клиента |
| `-Duitp.e2e.launchPassword=<пароль ИБ>` | пароль ИБ для автовхода клиента |

Учётная запись ИБ — тестовая (не секрет в смысле кода, но задаётся в среде, а не в репо).

## Установка через UI EDT (Help → Install New Software)

> Плагин распространяется как **p2 update-site** на GitHub Pages. Установка
> выполняется прямо из EDT без ручного копирования jar/pool/bundles.info.

1. В EDT: **Help → Install New Software...**
2. В поле **Work with:** вставьте адрес update-site:

   ```
   https://gruzdevd.github.io/specter-edt-plugin/
   ```

   (нажмите Enter — список категорий подгрузится).
3. Отметьте категорию **«Specter»** → **Next**.
4. Нажмите **Next** → согласитесь на установку неподписанного (unsigned) ПО —
   плагин не подписан, это ожидаемо → **Finish** → **Restart Now**.

## Проверка

- На главной панели EDT появилась кнопка **«Запустить сценарий»** (и пункт меню
  **Specter → Запустить сценарий**).
- Панели **Тесты** и **Результаты** открываются через **Window → Show View → Other →
  Specter**. При открытом тестовом наборе панель «Тесты» показывает его тесты, а
  кнопка **«Запустить набор»** запускает их через мост.
- Убедитесь, что заданы три системных свойства (см. таблицу выше).
- Нажмите кнопку: плагин сгенерирует runId, запишет `bridge-commands.json`, запустит
  тонкий клиент АУФ — клиент сам откроет ФормуСписка, исполнит команды на реальном
  UI и запишет `bridge-result-<runId>.json`.
- Лог плагина — в EDT `.metadata/.log` с префиксом `ru.ozon.uitp.e2e`.

## Сборка

Tycho (публичные p2-репозитории 1С `ruby/2025.2` и Eclipse 4.30; JDK 17):

```
mvn -f pom.xml clean verify -T 1C
```

Готовый update-site — в
`repositories/ru.ozon.uitp.e2e.repository/target/repository/` (`content.jar`,
`artifacts.jar`, `features/`, `plugins/`).

## CI / Релиз / Публикация

- **build.yml** — сборка на push/PR (проверка компиляции).
- **release.yml** — по тегу `v*`: проставляет версию, собирает, кладёт
  `UITP-EDT.v<ver>.zip` в GitHub Release.
- **deploy-update-site.yml** — при публикации Release деплоит update-site на
  GitHub Pages (тот самый URL для Install New Software).

## Структура

```
bundles/ru.ozon.uitp.e2e/          сама OSGi-обёртка плагина (src + MANIFEST + plugin.xml)
  src/ru/ozon/uitp/e2e/
    Activator.java                 bundle-активатор
    launcher/BridgeLaunchHelper.java  поиск/копия RuntimeClient, автовход, DebugUITools.launch
    launcher/LaunchMonitor.java       ожидание bridge-result
    handler/RunScenarioHandler.java   кнопка (фоновый Job)
features/ru.ozon.uitp.e2e.feature/ feature (p2-контейнер)
repositories/ru.ozon.uitp.e2e.repository/ eclipse-repository (update-site) + category.xml
targets/default/default.target     target platform (публичный p2 1С + Eclipse 4.30)
```

## Лицензия

[EPL-2.0](LICENSE).
