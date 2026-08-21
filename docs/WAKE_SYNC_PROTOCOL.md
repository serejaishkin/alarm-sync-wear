# WakeSync protocol v1

WakeSync treats the phone and watch as two local alarm schedulers sharing one logical alarm state.

## Identity

The existing Alarm `id` remains a device-local Room primary key. It must not be used as cross-device identity.

The sync layer uses a stable UUID `syncId`. The same `syncId` identifies one logical alarm on the phone and watch.

The first integration keeps sync metadata outside the existing Alarm entity. After the contract is tested, a dedicated Room metadata table will persist the mapping without rewriting the existing alarm primary key.

## Mutation envelope

Each mutation contains:

- `protocolVersion`
- `deviceId`
- `syncId` (stable cross-device identity)
- `alarmId` (local id of the sender)
- `operation`
- `source`
- `revision`
- `timestamp`
- optional serialized alarm payload

## Operations

- `CREATE`
- `UPDATE`
- `ENABLE`
- `DISABLE`
- `DELETE`
- `SNOOZE`
- `DISMISS`
- `RINGING`

## Conflict resolution

For v1 the newest mutation wins:

1. higher `revision` wins;
2. if revisions are equal, newer `timestamp` wins;
3. equal revision and timestamp is treated as a duplicate.

## Transport

Transport is deliberately separated from the protocol. The first implementation can use Wear OS Data Layer; a direct Bluetooth/BLE transport can be added later without changing alarm-domain code.
