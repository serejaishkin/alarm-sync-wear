# WakeSync — Phone ↔ Wear OS Alarm Sync

Дата: 2026-08-21
Статус: планирование / подготовка базы

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

- Phone → Watch: публикуется только один `next alarm` снапшот через
  `/alarmclockxtreme/next_alarm`.
- Watch → Phone: skip / snooze / dismiss для уже звонящего будильника.
- Нет создания/редактирования/удаления/toggle будильника с часов.
- Нет полного локального объекта Alarm на часах.
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
WakeSync добавит это самостоятельно.

---

## 3. Direct BLE

Готовый проверенный open-source проект, который можно безопасно взять как основу
для прямого BLE GATT транспорта, пока не найден.

Поэтому Phase 1: Wearable Data Layer.

Phase 2: собственный Direct BLE GATT transport.

При этом транспорт должен быть отделён от AlarmSync Core, чтобы переход не требовал
переписывать доменную логику.

---

## 4. WakeSync architecture

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
                 /          \
        Wear Data Layer      BLE (Phase 2)
```

### Alarm identity

Используем UUID, а не локальный auto-increment ID.

```text
Alarm
├── id: UUID
├── time
├── days
├── label
├── enabled
├── version: Long
├── updatedAt: Long
└── updatedBy: phone | watch
```

Один `id` должен обозначать один логический будильник на обоих устройствах.

---

## 5. Sync protocol v1

Предварительные Data Layer paths:

```text
/alarmsync/alarm/put
/alarmsync/alarm/delete
/alarmsync/action
```

`alarm/put` передаёт полный Alarm JSON.

`alarm/delete` передаёт минимум `{ id, version }`.

`action` используется для runtime actions:

```text
DISMISS
SNOOZE
RINGING
```

### Conflict resolution

При получении Alarm:

```text
if incoming.version > local.version:
    apply(incoming)
elif incoming.version == local.version and incoming.updatedAt > local.updatedAt:
    apply(incoming)
else:
    ignore
```

При локальном изменении:

```text
version += 1
updatedAt = now()
updatedBy = localDevice
send()
```

Позже добавим offline queue/reconciliation.

---

## 6. Важное отличие от ACX

В ACX snooze/dismiss с часов завязаны на `AlarmService.activeAlarmId` телефона.
Это нормально для remote control звонящего будильника, но недостаточно для WakeSync.

WakeSync должен отдельно синхронизировать состояние объекта и runtime state.

Например:

```text
Phone: disable alarm #42
        ↓
Sync
        ↓
Watch: disable alarm #42
```

И наоборот.

---

## 7. План разработки

1. [ ] Форкнуть AlarmClockXtreme.
2. [ ] Перенести его Android/Wear базу в WakeSync.
3. [ ] Переименовать приложение и branding в WakeSync.
4. [ ] Изучить текущую схему `Alarm` и подготовить Room migration.
5. [ ] Добавить UUID + version + updatedAt + updatedBy.
6. [ ] Создать `AlarmSyncBridge` / `SyncTransport` интерфейсы.
7. [ ] Реализовать Phone → Watch full Alarm sync.
8. [ ] Реализовать Watch → Phone full Alarm sync.
9. [ ] Create / Update / Enable / Disable / Delete.
10. [ ] Dismiss / Snooze / Ringing state.
11. [ ] Offline queue + reconnect reconciliation.
12. [ ] Tile/complication отражают синхронизированный Alarm state.
13. [ ] Реальное тестирование на Galaxy Watch.
14. [ ] Phase 2: Direct BLE GATT transport.

---

## 8. Правила проекта

- Не писать существующую alarm-инфраструктуру заново без причины.
- Не привязывать sync core к Google Play Services.
- Транспорт должен быть заменяемым.
- Все изменения схемы БД — через миграции.
- Для синхронизации использовать UUID.
- Комментарии внутри кода — на английском.
- Важные архитектурные решения фиксировать в `docs/`.
- Частые изменения фиксировать небольшими понятными коммитами.
