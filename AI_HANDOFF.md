# WakeSync — Phone ↔ Wear OS Alarm Sync

Дата: 2026-08-24
Статус: активная разработка / интеграционное тестирование

## TL;DR

WakeSync — двусторонняя синхронизация будильников между Android-телефоном и Wear OS.

Цель: один логический будильник существует на обоих устройствах. Его можно создать,
изменить, включить/выключить, удалить, отложить или отключить с телефона либо часов,
а состояние второй стороны автоматически приводится к тому же состоянию.

База: форк AlarmClockXtreme (Apache 2.0), с использованием архитектурных решений
из UAC Companion там, где это подходит. Sync-протокол и conflict resolution пишем сами.

---

## 1. AlarmClockXtreme

https://github.com/SysAdminDoc/AlarmClockXtreme — Apache 2.0, Kotlin/Compose,
модули `app/` (телефон) + `wear/` (часы), flavor'ы `play` и `fdroid`.

### Что реально реализовано

ACX НЕ является полноценной двусторонней синхронизацией будильников как объектов.

- Phone → Watch: публикуется только один `next alarm` снапшот через `/alarmclockxtreme/next_alarm`.
- Watch → Phone: skip / snooze / dismiss для уже звонящего будильника.
- Нет полного двустороннего CRUD как готового решения.
- Data Layer находится в Play flavor; F-Droid вариант использует no-op bridge.

### Что берём

- готовый Alarm Engine;
- Room database и миграции;
- Android/Wear UI;
- Tile/complication;
- Wear Data Layer patterns;
- flavor-based DI для Play/no-op реализаций.

Исходный sync-протокол ACX не переносим как готовое решение — WakeSync нужен другой,
полноценный bidirectional CRUD протокол.

---

## 2. UAC Companion

https://github.com/CCExtractor/uac_companion

Это наиболее близкий референс по передаче полного объекта Alarm.

### Что используем как reference

- полный `alarm_json`, а не только next-alarm snapshot;
- отдельные Phone → Watch и Watch → Phone paths;
- `unique_sync_id` как идентификатор объекта;
- локальное сохранение полученного будильника на Watch;
- отдельные action messages для delete/dismiss/snooze.

### Что НЕ копируем без изменений

UAC не реализует полноценное version/timestamp conflict resolution.
WakeSync добавляет это самостоятельно.

---

## 3. WakeSync architecture

```text
                 WakeSync Core
                       |
              AlarmSyncRepository
                       |
              +--------+--------+
              |                 |
          Phone adapter     Wear adapter
              |                 |
         AlarmManager       AlarmManager
              |                 |
              +-------+---------+
                      |
                SyncTransport
                       |
                Wear Data Layer
```

### Alarm identity / versioning

Используем стабильный UUID/`syncId`, а не локальный auto-increment ID.

Логический объект должен содержать как минимум:

```text
Alarm
├── syncId: String
├── time
├── days
├── label
├── enabled
├── revision: Long
├── updatedAt: Long
├── source: phone | watch
└── originDeviceId: String
```

При конфликте более новая версия должна побеждать. Для одинаковой версии используется
время изменения и затем стабильный идентификатор устройства как tie-breaker.

---

## 4. Sync protocol

Используем Google Wearable Data Layer как основной транспорт Phase 1.

Ключевые операции:

```text
CREATE
UPDATE
ENABLE
DISABLE
DELETE
RINGING
SNOOZE
DISMISS
```

Нужны два механизма:

1. **MessageClient** — быстрые runtime-команды, например RINGING/SNOOZE/DISMISS.
2. **DataClient** — полный persistent snapshot будильника, который должен переживать
   временное отсутствие соединения и доставляться после восстановления связи.

### Reconciliation

Обе стороны должны уметь запросить полный snapshot второй стороны.

Дополнительно используется периодическая сверка примерно раз в 15 минут как страховка
после перезапуска, временной потери Data Layer или пропущенного события.

Периодическая сверка не заменяет немедленную передачу изменений.

---

## 5. Текущий рабочий статус

### Синхронизация

- [x] Phone → Watch: создание будильника в базовом сценарии работает.
- [x] Watch → Phone: создана инфраструктура полной мутации будильника.
- [x] CREATE / UPDATE / ENABLE / DISABLE / DELETE paths.
- [x] Runtime SNOOZE / DISMISS / RINGING paths.
- [x] Stable `syncId` / revision / timestamp / device identity.
- [x] Persistent Data Layer snapshot listener на телефоне.
- [x] Snapshot request с часов.
- [x] Периодическая reconciliation-задача.
- [ ] Проверить, что полный список из нескольких будильников всегда восстанавливается
      на обеих сторонах после cold start/reconnect.
- [ ] Проверить гонки: одновременное изменение одного будильника на телефоне и часах.
- [ ] Проверить удаление и повторное создание с тем же логическим объектом.
- [ ] Проверить offline → online reconciliation.

### Будильник на Wear OS

- [x] Локальный список будильников на часах.
- [x] Редактор будильника.
- [x] Включение/выключение.
- [x] Удаление.
- [ ] Полностью проверить реальное срабатывание всех повторов.
- [ ] Проверить snooze с часов → телефон → продолжающийся/перезапущенный звонок.
- [ ] Проверить dismiss с часов → телефон.
- [ ] Проверить срабатывание, когда телефон заблокирован.
- [ ] Проверить срабатывание, когда часы заблокированы.
- [ ] Проверить, что экран блокировки не блокирует WakeSync alarm UI.

---

## 6. КРИТИЧЕСКАЯ ПРОБЛЕМА: Media Controller

### Симптом

При срабатывании WakeSync-будильника на часах одновременно или сразу после него
открывается системная панель Media Controller так, будто была запущена музыка.

Из-за этого:

- WakeSync alarm UI может быть перекрыт;
- управление SNOOZE/DISMISS становится недоступным;
- создаётся впечатление, что будильник не сработал;
- проблема особенно заметна на заблокированном экране.

### Требуется отдельное расследование

НЕ считать это частью обычной синхронизации. Нужно проверить отдельно:

1. `WakeSyncAlarmFiringActivity` / alarm intent.
2. Все `PendingIntent`, `Intent` actions и extras, которые запускаются при RINGING.
3. `WakeSyncAlarmScheduler` и `AlarmService`.
4. MediaSession / MediaController / PlaybackState, которые могут случайно активироваться.
5. Foreground service и notification категории.
6. `Notification.Action` / media-style notification.
7. Activity launch mode и flags при запуске с locked screen.
8. Android/Wear OS роли alarm/full-screen intent.
9. Почему системный Media Controller получает событие именно в момент RINGING.

### Важно

Нельзя просто скрыть Media Controller визуально. Нужно найти источник события,
из-за которого Wear OS считает, что изменилось состояние медиаплеера.

---

## 7. Проблема с экраном блокировки

Текущий сценарий требует отдельной реализации и тестирования:

```text
AlarmManager
    ↓
WakeSync alarm trigger
    ↓
Full-screen / lock-screen alarm UI
    ↓
SNOOZE / DISMISS
    ↓
Sync runtime state to peer
```

Требование:

> При срабатывании будильника WakeSync должен иметь приоритет как alarm UI и корректно
> работать поверх/на экране блокировки, не превращаясь в media-control экран.

Нужно проверить Android 13+ notification/full-screen restrictions и Wear OS ограничения.

---

## 8. План разработки

1. [x] Форкнуть AlarmClockXtreme.
2. [x] Перенести Android/Wear базу в WakeSync.
3. [x] Переименовать приложение и branding в WakeSync.
4. [x] Изучить текущую схему Alarm и подготовить sync metadata.
5. [x] Добавить стабильный syncId + revision + updatedAt + source + device identity.
6. [x] Создать SyncTransport boundary.
7. [x] Реализовать Phone → Watch full Alarm sync.
8. [x] Реализовать Watch → Phone full Alarm sync.
9. [x] Create / Update / Enable / Disable / Delete.
10. [x] Dismiss / Snooze / Ringing state.
11. [x] Persistent Data Layer state listener.
12. [x] Snapshot request/reconciliation.
13. [x] Periodic reconciliation.
14. [ ] Починить Media Controller, который появляется при RINGING.
15. [ ] Починить full-screen/lock-screen alarm UI на Wear OS.
16. [ ] Полностью проверить SNOOZE/DISMISS при заблокированном телефоне.
17. [ ] Полностью проверить SNOOZE/DISMISS при заблокированных часах.
18. [ ] Проверить несколько будильников после cold start/reconnect.
19. [ ] Добавить offline queue + полноценную reconnect reconciliation.
20. [ ] Реальное длительное тестирование на Galaxy Watch.
21. [ ] Phase 2: Direct BLE GATT transport.

---

## 9. Правила проекта

- Не писать существующую alarm-инфраструктуру заново без причины.
- Не привязывать sync core к Google Play Services.
- Транспорт должен быть заменяемым.
- Все изменения схемы БД — через миграции.
- Для синхронизации использовать стабильный syncId.
- Комментарии внутри кода — на английском.
- Важные архитектурные решения фиксировать в `docs/`.
- Частые изменения фиксировать небольшими понятными коммитами.
- Сначала исправлять причину сбоя, а не маскировать системный UI.

---

## 10. Последний тестовый отчёт

Дата: 2026-08-24

Наблюдения:

- Будильник, созданный с телефона, приходит на часы.
- При наличии нескольких будильников часть списка на другой стороне может не
  восстанавливаться автоматически — требуется проверка reconciliation.
- При RINGING на часах снова появляется системный Media Controller.
- WakeSync alarm UI на экране блокировки работает некорректно/перекрывается.
- SNOOZE/DISMISS необходимо проверить после устранения Media Controller и lock-screen проблемы.

Следующая задача: **не менять sync-протокол вслепую, а отдельно локализовать Media Controller
и lock-screen alarm launch. После этого повторить двусторонний тест синхронизации.**
