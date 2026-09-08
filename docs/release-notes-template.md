# WakeSync — Release Notes Template

Как использовать:

1. Соберите подписанные артефакты: `./gradlew :app:bundlePlayRelease :app:assemblePlayRelease :app:verifyFdroidReleaseSize :wear:assembleRelease`.
2. Сгенерируйте `SHA256SUMS.txt` и `APK-CERT-FINGERPRINTS.txt`, поднимите версию в `app/build.gradle.kts` (versionCode/versionName), обновите заголовок `## [Unreleased]` в CHANGELOG.md на `## [{{VERSION}}] - {{DATE}}` — это требуется скриптом `verifyReleaseMetadata`.
3. Создайте GitHub Release с тегом `v{{VERSION}}` и вставьте ниже текст EN (базовый) + RU.
4. Прикрепите: Play AAB `app-play-release.aab`, APK `app-play-release.apk`, `app-fdroid-release.apk`, `wear-release.apk`, `SHA256SUMS.txt`, `APK-CERT-FINGERPRINTS.txt`.

---

## English

**WakeSync {{VERSION}}** — a rebranded, feature-rich open-source alarm clock for
Android + Wear OS. No ads, no tracking, no accounts.

### Highlights
- **Rebrand to WakeSync** — the app is now published under the `com.wakesync.app`
  package; installs from the old package must be migrated via Export / Import backup.
- **Phone ↔ watch alarm sync** — alarms stay in sync between your phone and Wear OS
  watch over the Data Layer (create, update, delete, enable/disable, snooze, dismiss).
- **Full UI parity on Wear OS** — dark Material 3 theme, matching list/editor/firing
  screens, and the watch shows sync provenance ("Changed on watch/phone at HH:mm").
- **Deep dark theme, 50+ alarm fields, 30 dismiss challenges** — math, shake, squats,
  handwriting, NFC, photo match, maze, Wordle, mission chaining, and more.

### What's new in this release
- One-off "snooze until" clock-time picker, carried through the service contract with
  exact-alarm and inexact fallback.
- Ongoing snooze countdown notification — Android 16 `ProgressStyle` live updates, and
  a chronometer fallback on older versions.
- Media3 stall hardening for local tones and internet radio — bounded READY watchdog
  routes to speaker/max volume, records the failure, and falls back to the legacy player.
- Opt-in on-call mode for rotating-shift alarms (temporary DND override during the ring,
  restored automatically afterward).
- Android 13+ in-app language picker (`LocaleManager`) with a system-default reset.
- Folder-based ringtone import through the system document-tree picker (no broad
  storage permission).
- Per-OEM battery/autostart guidance for Samsung, Xiaomi, Oppo/Realme, Vivo/iQOO,
  OnePlus with safe fallbacks; OTA build changes re-open the wake-readiness checklist.
- Configurable firing-screen dismiss hold (0.5–5 s), default-on alarm status-icon
  preference, TalkBack-audited firing screen, predictive-back discard guard in editor.
- Full RU/EN localization.

### Upgrade note
From previous versions: Settings → Backup → Export, then install the new APK and
Settings → Backup → Restore (or import the backup file). For share links use `acx://`.

### Verify the APK
```bash
apksigner verify --print-certs WakeSync-v{{VERSION}}-play-release.apk
```
Compare the `certificate SHA-256 digest` against `APK-CERT-FINGERPRINTS.txt`.

### Download
- Google Play (Play flavor): `app-play-release.apk` — includes YouTube alarm-sound
  downloader, Health Connect, ML Kit handwriting, Wear sync bridge.
- F-Droid (F-Droid flavor): `app-fdroid-release.apk` — without proprietary pieces.
- Wear OS: `wear-release.apk` — install on a paired watch.

**Full changelog:** [CHANGELOG.md](https://github.com/serejaishkin/alarm-sync-wear/blob/main/CHANGELOG.md)

---

## Русский

**WakeSync {{VERSION}}** — ребрендированный многофункциональный будильник с открытым
исходным кодом для Android + Wear OS. Без рекламы, трекинга и аккаунтов.

### Главное
- **Ребрендинг в WakeSync** — приложение теперь публикуется под пакетом
  `com.wakesync.app`; со старого пакета переходите через экспорт и импорт резервной копии.
- **Синхронизация телефон ↔ часы** — будильники синхронизируются между телефоном и
  часами Wear OS через Data Layer (создание, изменение, удаление, включение/выключение,
  повтор, отключение).
- **Полный визуальный паритет на Wear OS** — тёмная Material 3-тема, одинаковые экраны
  списка/редактора/звонка, на часах виден источник изменения («Изменено на часах/телефоне»).
- **Глубокая тёмная тема, 50+ полей будильника, 30 способов отключения** — математика,
  встряска, приседания, рукописный ввод, NFC, фото, лабиринт, Wordle, цепочка заданий и другое.

### Что нового в этом релизе
- Пикер «отложить до» (одноразовый выбор времени) с точным и неточным планированием.
- Постоянное уведомление-обратный отсчёт повтора — Live Update `ProgressStyle` на
  Android 16 и хронометр на старых версиях.
- Устойчивость Media3 при зависании звука для локальных мелодий и интернет-радио.
- Опциональный «режим дежурного» для сменных графиков (временное «Не беспокоить» только
  для будильников, с автовосстановлением после звонка).
- Выбор языка приложения на Android 13+ (с возвратом к системному).
- Импорт папки с мелодиями через системный документ-пикер (без широкого разрешения).
- Инструкции по батарее/автозапуску для Samsung, Xiaomi, Oppo/Realme, Vivo/iQOO,
  OnePlus; изменения прошивки снова открывают чек-лист готовности к звонку.
- Настраиваемое удержание для отключения (0,5–5 с), иконка статуса будильника,
  экран звонка с полной поддержкой TalkBack, отмена редактирования жестом назад.
- Полная RU/EN локализация.

### Как обновиться
Настройки → Резервная копия → Экспорт, затем установите новый APK и:
Настройки → Резервная копия → Восстановить. Для шеринга ссылок используется `acx://`.

### Проверка подписи
```bash
apksigner verify --print-certs WakeSync-v{{VERSION}}-play-release.apk
```
Сравните `certificate SHA-256 digest` с `APK-CERT-FINGERPRINTS.txt`.

### Скачать
- Google Play (Play flavor): `app-play-release.apk` — загрузчик звуков с YouTube,
  Health Connect, ML Kit, мост синхронизации с часами.
- F-Droid (F-Droid flavor): `app-fdroid-release.apk` — без проприетарных компонентов.
- Wear OS: `wear-release.apk` — установите на сопряжённые часы.

**Полный список изменений:** [CHANGELOG.md](https://github.com/serejaishkin/alarm-sync-wear/blob/main/CHANGELOG.md)