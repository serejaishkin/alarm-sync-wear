# Alarm Sync (Phone ↔ Wear OS) — карта проекта

Дата: 2026-08-21
Статус: планирование, форк ещё не сделан

## TL;DR

Разобрал исходники двух реальных open-source проектов на GitHub (не с чужих слов,
а по коду через `codeload.github.com`). Ниже — что там на самом деле есть, что
можно унести к себе, и предложенная архитектура для своего Alarm Sync поверх
форка AlarmClockXtreme.

---

## 1. AlarmClockXtreme (SysAdminDoc/AlarmClockXtreme)

https://github.com/SysAdminDoc/AlarmClockXtreme — Apache 2.0, Kotlin/Compose,
реально существует, актуально поддерживается.

Структура: `app/` (телефон) + `wear/` (часы), разделение на flavor'ы `play` и
`fdroid`. Wear-логика на Data Layer (`com.google.android.gms.wearable`) живёт
только в `play` flavor — в `fdroid` её нет вообще (no-op бридж).

### Что там реально реализовано (проверено по коду)

Это **НЕ** двусторонняя синхронизация будильников как объектов. Это:

- Phone → Watch: публикуется **только один "next alarm" снапшот** (ближайший
  активный будильник), через `PutDataMapRequest` на путь
  `/alarmclockxtreme/next_alarm`. Публикуется по одному урезанному DTO
  (id, label, time_label, trigger_time, is_firing, timezone), не полный объект
  будильника, не список.
- Watch → Phone: только 3 команды-действия **для уже звонящего** будильника —
  skip / snooze / dismiss (`WearAlarmActionListenerService`), причём
  snooze/dismiss на телефоне проверяются на `AlarmService.activeAlarmId` —
  сработает только если этот будильник реально сейчас звонит на телефоне.
- **Нет** создания/редактирования/удаления/toggle будильника с часов.
- **Нет** независимого хранения полного будильника на часах — только
  SharedPreferences-снапшот для отображения на tile/complication.
- Транспорт — Google Wearable Data Layer (требует Play Services на обоих
  устройствах, никакого direct BLE тут нет).

### Что можно унести как есть

- `wear/.../WearAlarmData.kt` — константы путей и ключей DataMap, паттерн
  задания протокола. Годится как основа для своих path/key констант.
- `WearAlarmDataListenerService` — паттерн подписки на `onDataChanged` +
  триггер обновления Tile/Complication после апдейта.
- `PlayWearNextAlarmBridge` — паттерн `observeNextAlarm().combine(settings)`
  + `Wearable.getDataClient(context).putDataItem(...).setUrgent()`. Рабочий
  пример как публиковать поверх Room+Flow репозитория.
- Flavor-based DI (`WearNextAlarmBridge` интерфейс + `Play`/no-op реализации)
  — полезный паттерн, если тоже хотим F-Droid сборку без Play Services.
- Готовый Alarm Engine, UI, тесты миграций БД, tile/complication код — большой
  объём написанной и рабочей инфраструктуры вокруг будильников как таковых.

### Вывод по ACX

Хорошая база для форка (Alarm Engine + UI + Wear skeleton), но
**sync-протокол придётся писать с нуля** — готового bidirectional CRUD там нет.

---

## 2. UAC Companion (CCExtractor/uac_companion)

https://github.com/CCExtractor/uac_companion — официальный Wear OS companion
для Ultimate Alarm Clock, GSoC 2025. Flutter-приложение для часов + Kotlin
native слой для scheduling/DataClient. Это **только сторона часов** — сам
UAC (телефон) в отдельном репозитории CCExtractor/ultimate_alarm_clock.

### Что там реально реализовано (проверено по коду)

Это ближе к тому, что тебе нужно — **полный объект будильника гоняется в обе
стороны как JSON**, а не урезанный снапшот:

- Phone → Watch (`/uac_phone_to_watch/alarm`): полный `alarm_json` (Gson) —
  id, time, days, `unique_sync_id`, is_enabled, location/weather/activity/
  guardian-условия и т.д. — весь объект, не только "next alarm".
  На стороне часов `UACDataLayerListenerService` при получении сразу
  **insert в локальную SQLite** (`wear_alarms.db`) + `AlarmScheduler.
  scheduleNextAlarm()` + событие во Flutter UI.
- Watch → Phone (`/uac_watch_to_phone/alarm`, `WatchAlarmSender.
  sendAlarmToPhone`): аналогично, полный JSON + флаг `isNewAlarm`.
- Команды действий отдельным каналом
  (`/uac_phone_to_watch/action`, `/uac_watch_to_phone/action`):
  `delete alarm`, `dismiss`, `snooze` — с `uniqueSyncId`, не привязано к
  "сейчас звонит", в отличие от ACX.
- Есть отдельный канал verdict'ов от smart-conditions (`/uac/pre_check_verdict`)
  — если условие (погода/локация) говорит "не звонить", часы сами отменяют
  запланированный будильник.
- Идентификация объекта — строковый `unique_sync_id`, а не числовой id
  устройства-источника (важно: под конфликты между двумя устройствами лучше
  UUID, чем автоинкремент).

### Чего там НЕТ (не выдумываю — специально проверил)

- **Нет version/timestamp-based conflict resolution.** Есть `timestamp`
  в DataMap, но код его нигде не сравнивает при получении — просто
  last-write-wins по приходу сообщения. Одновременное изменение с обеих
  сторон отдельно не обрабатывается.
- **Нет toggle enabled/disabled** как отдельного action-пути — только полный
  re-send объекта или delete.

### Вывод по UAC

Протокол ближе к нужному "будильник как единый объект с обеих сторон", но
если хотим устойчивость к одновременному редактированию — версионирование
придётся добавить самим (см. раздел 4).

---

## 3. Проверка: DieselBridge

В прошлом обсуждении всплыл проект "DieselBridge" (прямой BLE GATT без Google
Play Services для Wear OS). **Не найден** ни прямым поиском, ни по темам
`bluetooth-gatt`/`android-wear` на GitHub — похоже на очередной
галлюцинированный проект (как раньше было с фейковым файлом от Kimi в
keenetic-local-app). Не бери за основу, пока не найдётся реальный репозиторий.

Direct BLE GATT без Data Layer как таковой подход рабочий и делается,
просто готового open-source проекта под это не нашлось — если нужно, это
Phase 2 и пишется с нуля (или переиспользуется транспортный код из WristKey,
у тебя там уже есть BLE + связка Phone↔Watch).

---

## 4. Предлагаемая архитектура своего Alarm Sync

Базовая идея не меняется: форк ACX (Alarm Engine + UI + tile/complication
готовые), а sync-слой пишем сам, забирая протокольные решения из UAC
Companion, но с добавлением версионирования.

```
┌─────────────────────────────────────────────┐
│                 Alarm (единый объект)         │
│  id: UUID (не автоинкремент!)                 │
│  time, days, label, enabled                   │
│  version: Long        ← добавляем сами        │
│  updatedAt: Long                              │
│  updatedBy: "phone" | "watch"                 │
└─────────────────────────────────────────────┘
```

### Транспорт v1 — Data Layer (как в обоих проектах)

Пути по аналогии с UAC (полный объект, не снапшот):

```
/alarmsync/alarm/put      — полный Alarm JSON, phone↔watch, оба направления
/alarmsync/alarm/delete   — { id, version }
/alarmsync/action         — { id, action: dismiss|snooze }, без привязки к "firing"
```

### Conflict resolution (то, чего нет ни в ACX, ни в UAC)

При получении `alarm/put`:

```
if incoming.version > local.version:
    apply(incoming)
elif incoming.version == local.version and incoming.updatedAt > local.updatedAt:
    apply(incoming)   # tie-break по времени
else:
    ignore  # у нас уже более новая версия — либо переслать свою в ответ
```

При локальном изменении: `version += 1`, `updatedAt = now()`, отправить.

### Выключение / dismiss — учесть баг ACX

В ACX snooze/dismiss с часов срабатывает только если будильник **сейчас
звонит на телефоне** (`AlarmService.activeAlarmId` проверка). Для честной
двусторонней синхронки "выключил на телефоне → выключился на часах" эту
проверку убирать/переделывать — это была логика для "укротить звонящий
будильник", а не "изменить состояние объекта".

### Что забираем в свой репо буквально (с адаптацией)

| Источник | Файл | Что берём |
|---|---|---|
| ACX | `wear/.../WearAlarmData.kt` | паттерн констант путей/ключей |
| ACX | `WearAlarmDataListenerService.kt` | подписка на onDataChanged + апдейт tile |
| ACX | `PlayWearNextAlarmBridge.kt` | паттерн Flow→DataClient публикации |
| UAC | `WatchAlarmSender.kt` | полный-объект-JSON вместо снапшота |
| UAC | `UACDataLayerListenerService.kt` | insert в локальную БД по приходу + broadcast dismiss/snooze без "firing"-ограничения |
| свой | — | version/updatedAt conflict resolution (нет ни там, ни там) |
| свой | — | UUID вместо числового id для alarm |

### Шаги

1. Форк `AlarmClockXtreme` (F-Droid flavor как база, чище от Play-специфики,
   Data Layer добавим сами по образцу play-flavor кода).
2. Перевести `Alarm.id` с автоинкремента на UUID + добавить `version`,
   `updatedAt`, `updatedBy` в схему/миграцию Room (у них уже есть миграции —
   `AlarmDatabaseMigrationTest.kt`, паттерн понятен).
3. Написать свой `AlarmSyncBridge` (интерфейс как `WearNextAlarmBridge`, но
   для полного объекта, не next-alarm), взяв структуру публикации из
   `PlayWearNextAlarmBridge` + payload-формат из `WatchAlarmSender`.
3a. На стороне часов — свой `AlarmSyncListenerService` = гибрид
    `WearAlarmDataListenerService` (тайл/complication апдейт) +
    `UACDataLayerListenerService` (insert в локальную БД, без ограничения
    "только firing").
4. Добавить conflict resolution по version/updatedAt — этого нет ни в одном
   из источников, писать самим.
5. Протестировать на реальном железе (часы + телефон), как обычно.
6. Phase 2 (опционально) — прямой BLE GATT транспорт вместо Data Layer,
   если хочется отвязаться от Google Play Services полностью. Готового
   open-source проекта под это не нашлось, писать с нуля или переиспользовать
   транспортный код из WristKey.

---

## 5. Прочее найденное (для справки, не проверено так же глубоко)

- `android/wear-os-samples` → модуль `DataLayer` — официальный минимальный
  пример от Google, если нужен чистый референс без стороннего кода.
- Ultimate Alarm Clock (основной репо, телефон-часть) отдельно от companion —
  если понадобится посмотреть, как они формируют `alarm_json` на телефоне.
