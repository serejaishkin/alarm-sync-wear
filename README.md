# WakeSync — Phone ↔ Wear OS Alarm Synchronization

WakeSync provides bidirectional, conflict-resolved synchronization of alarms between an Android smartphone and a Wear OS smartwatch. A logical alarm exists synchronously across both devices: creating, updating, toggling, deleting, snoozing, or dismissing an alarm on one device immediately propagates to the peer.

## Architecture

```
                 WakeSync Core Engine
                          │
                 AlarmSyncRepository
                          │
           ┌──────────────┴──────────────┐
           │                             │
     Phone Adapter                 Wear OS Adapter
           │                             │
      AlarmManager                  AlarmManager
           │                             │
           └──────────────┬──────────────┘
                          │
                    SyncTransport
                          │
          Google Wearable Data Layer (v1.0)
          ├─ DataClient: /wakesync/alarm/state/{syncId} (Persistent)
          └─ MessageClient: /wakesync/alarm/mutation (Live actions)
```

## Features

- **Bidirectional CRUD**: Full synchronization of alarms (time, label, repeat days, snooze duration, volume, vibration).
- **Deterministic Conflict Resolution**: 3-tier resolution:
  1. `revision`: Incremented upon each edit; highest revision wins.
  2. `timestamp`: Fallback to epoch timestamp if revisions match.
  3. `originDeviceId`: Lexical tie-breaker if timestamps match.
- **Dual-Device Interactive Simulator**:
  - **Android Phone View**: Material You alarm manager with quick toggles, live state, and full-screen alarm trigger.
  - **Wear OS Smartwatch View**: Circular wrist face featuring the Next Alarm Tile (`NextAlarmTileService`), wrist alarm list, and wrist ringing overlay.
- **Offline Resiliency & Reconciliation**:
  - Mutations made while offline are queued locally.
  - Automatic snapshot reconciliation harmonizes all alarms once the connection resumes.
- **Media Controller & Lock-Screen Diagnostic Lab**:
  - Reproduces and documents the fix for the Wear OS Media Controller interruption bug during alarm triggers.
  - Demonstrates Android 13+ Full-Screen Intent configuration and `USAGE_ALARM` audio routing.

## Development

```bash
npm install
npm run dev
```
Dev server runs on `http://0.0.0.0:3000`.
