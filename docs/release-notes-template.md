# WakeSync — Release Notes Template

Как использовать:

1. Соберите подписанные артефакты: `./gradlew :app:bundlePlayRelease :app:assemblePlayRelease :app:verifyFdroidReleaseSize :wear:assembleRelease`.
2. Сгенерируйте `SHA256SUMS.txt` и `APK-CERT-FINGERPRINTS.txt`, поднимите версию в `app/build.gradle.kts` (versionCode/versionName), обновите заголовок `## [Unreleased]` в CHANGELOG.md на `## [{{VERSION}}] - {{DATE}}` — это требуется скриптом `verifyReleaseMetadata`.
3. Создайте GitHub Release с тегом `v{{VERSION}}` и вставьте ниже текст EN (базовый) + RU.
4. Прикрепите: Play AAB `app-play-release.aab`, APK `app-play-release.apk`, `app-fdroid-release.apk`, `wear-release.apk`, `SHA256SUMS.txt`, `APK-CERT-FINGERPRINTS.txt`.

---

## English

**WakeSync {{VERSION}}** — a feature-rich open-source alarm clock for
Android + Wear OS. No ads, no tracking, no accounts.

### Highlights
- **Phone ↔ watch alarm sync** — alarms stay in sync between your phone and Wear OS
  watch over the Data Layer (create, update, delete, enable/disable, snooze, dismiss).
- **Full UI parity on Wear OS** — dark Material 3 theme, matching list/editor/firing
  screens, and the watch shows sync provenance.
- **Deep dark theme, 50+ alarm fields, 30 dismiss challenges** — math, shake, squats,
  handwriting, NFC, photo match, maze, Wordle, mission chaining, and more.

### What's new in this release
- One-off "snooze until" clock-time picker.
- Ongoing snooze countdown notification (Live Update).
- Media3 stall hardening for local tones and internet radio.
- Opt-in on-call mode for rotating-shift alarms.
- Android 13+ in-app language picker and folder-based ringtone import.
- Full RU/EN localization.

### Upgrade note
Upgrading from a previous test build? Export/Import your data via Settings → Backup.

### Verify the APK
```bash
apksigner verify --print-certs WakeSync-v{{VERSION}}-play-release.apk
```
Compare the `certificate SHA-256 digest` against `APK-CERT-FINGERPRINTS.txt`.

### Download
- Google Play (Play flavor): `app-play-release.apk`
- F-Droid (F-Droid flavor): `app-fdroid-release.apk`
- Wear OS: `wear-release.apk`

---
## Русский

**WakeSync {{VERSION}}** — многофункциональный будильник с открытым
исходным кодом для Android + Wear OS. Без рекламы, трекинга и аккаунтов.

### Главное
- **Синхронизация телефон ↔ часы** — будильники синхронизируются между телефоном и
  часами Wear OS через Data Layer (создание, изменение, удаление, повтор, отключение).
- **Полный визуальный паритет на Wear OS** — тёмная Material 3-тема, одинаковые экраны
  списка/редактора/звонка.
- **50+ полей будильника, 30 способов отключения** — математика, встряска, приседания,
  рукописный ввод, NFC, фото, лабиринт, Wordle, цепочка заданий и другое.

### Что нового в этом релизе
- Пикер «отложить до» (одноразовый выбор времени).
- Постоянное уведомление-обратный отсчёт повтора.
- Устойчивость Media3 при зависании звука.
- Опциональный «режим дежурного» для сменных графиков.
- Выбор языка приложения (Android 13+), импорт папок с мелодиями.
- Полная RU/EN локализация.

### Как обновиться
Обновляетесь с тестовой сборки? Сделайте Экспорт/Импорт настроек через Настройки → Резервная копия.

### Проверка подписи
```bash
apksigner verify --print-certs WakeSync-v{{VERSION}}-play-release.apk
```
Сравните `certificate SHA-256 digest` с `APK-CERT-FINGERPRINTS.txt`.

### Скачать
- Google Play (Play flavor): `app-play-release.apk`
- F-Droid (F-Droid flavor): `app-fdroid-release.apk`
- Wear OS: `wear-release.apk`