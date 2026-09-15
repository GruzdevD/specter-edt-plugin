# uitp-edt-plugin — EDT-плагин запуска UI-моста

Малый **EDT-плагин** (OSGi, `ru.ozon.uitp.e2e`): кнопка **«Запустить сценарий»** на
главной панели EDT программно запускает тонкий клиент АУФ (1С) со стартовой опцией
автозапуска BSL-моста на **реальном UI**, затем ждёт результат
(`bridge-result-<runId>.json`).

Часть платформы UITP (UI Test Platform) — рабочий инструмент этапа 2: убрать ручное
открытие формы-агента при прогоне UI-сценария.

## Что делает кнопка «Запустить сценарий»

1. Находит launch-конфигурацию EDT типа `com._1c.g5.v8.dt.launching.core.RuntimeClient`
   (в нашей среде — «Тонкий клиент АУФ»).
2. Копирует её (`UITP - <имя>`) и пишет в копию:
   - `ATTR_STARTUP_OPTION = "OZONUI_START_BRIDGE"` → клиент получает `/C OZONUI_START_BRIDGE`;
     модуль управляемого приложения расширения `afm.OZON_UI` открывает ФормуСписка,
     чьё `ПриОткрытии` исполняет команды моста на реальном UI;
   - `ATTR_LAUNCH_USER_NAME` / `ATTR_LAUNCH_USER_PASSWORD` → автовход без окна входа.
3. Запускает копию через `DebugUITools.launch(copy, "run")`.
4. В фоновом Job ждёт появления `bridge-result-*.json` в рабочей папке моста
   (свойство `uitp.e2e.outDir`).

## Установка через UI EDT (Help → Install New Software)

> Плагин распространяется как **p2 update-site** на GitHub Pages. Установка
> выполняется прямо из EDT без ручного копирования jar/pool/bundles.info.

1. В EDT: **Help → Install New Software...**
2. В поле **Work with:** вставьте адрес update-site:

   ```
   https://gruzdevd.github.io/uitp-edt-plugin/
   ```

   (нажмите Enter — список категорий подгрузится).
3. Отметьте категорию **«UITP E2E (EDT)»** → **Next**.
4. Нажмите **Next** → согласитесь на установку неподписанного (unsigned) ПО —
   плагин не подписан, это ожидаемо → **Finish** → **Restart Now**.

## Проверка

- На главной панели EDT появилась кнопка **«Запустить сценарий»** (и пункт меню
  **UITP E2E → Запустить сценарий**).
- Заполните `out/bridge-commands.json` командами моста (runner-side) и нажмите кнопку:
  EDT запустит тонкий клиент АУФ, клиент сам откроет ФормуСписка, исполнит команды
  на реальном UI и запишет `out/bridge-result-<runId>.json`.
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
