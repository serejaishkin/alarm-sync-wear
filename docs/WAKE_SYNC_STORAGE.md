# WakeSync storage

## Local identity

`Alarm.id` remains the Room primary key and is local to each device.

WakeSync uses a separate `alarm_sync_metadata` table:

- `alarmId` — local Room alarm id;
- `syncId` — stable UUID shared by phone and watch;
- `revision` — logical change revision;
- `updatedAt` — timestamp used as a deterministic tie-breaker;
- `updatedBy` — source device identifier.

This avoids changing the existing Alarm primary key and keeps the WakeSync scheduling code compatible.

## Migration plan

The database is currently version 23. The next storage commit will add version 24 and create `alarm_sync_metadata`.

Existing alarms will receive a unique `syncId` during migration. The migration must be deterministic per row and must never modify the existing alarm id or alarm fields.

Only after migration is covered by Room migration tests will AlarmRepository start creating and updating sync metadata.
