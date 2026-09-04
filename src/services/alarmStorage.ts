import { AlarmItem, SyncPayload, SyncOperation, DeviceSource } from '../types';

export const INITIAL_ALARMS: AlarmItem[] = [
  {
    syncId: 'alarm-wake-001',
    hour: 7,
    minute: 0,
    label: 'Morning Workout',
    enabled: true,
    repeatDays: [1, 2, 3, 4, 5], // Mon-Fri
    snoozeDurationMinutes: 10,
    vibrationEnabled: true,
    volume: 85,
    revision: 1,
    updatedAt: Date.now() - 3600000,
    source: 'PHONE',
    originDeviceId: 'phone-pixel-8',
    isFiring: false,
    snoozedUntil: null,
  },
  {
    syncId: 'alarm-wake-002',
    hour: 8,
    minute: 30,
    label: 'Weekend Standup',
    enabled: false,
    repeatDays: [0, 6], // Sun, Sat
    snoozeDurationMinutes: 15,
    vibrationEnabled: true,
    volume: 70,
    revision: 1,
    updatedAt: Date.now() - 7200000,
    source: 'WATCH',
    originDeviceId: 'watch-galaxy-6',
    isFiring: false,
    snoozedUntil: null,
  },
  {
    syncId: 'alarm-wake-003',
    hour: 14,
    minute: 15,
    label: 'Medication reminder',
    enabled: true,
    repeatDays: [0, 1, 2, 3, 4, 5, 6], // Everyday
    snoozeDurationMinutes: 5,
    vibrationEnabled: true,
    volume: 90,
    revision: 2,
    updatedAt: Date.now() - 1800000,
    source: 'PHONE',
    originDeviceId: 'phone-pixel-8',
    isFiring: false,
    snoozedUntil: null,
  }
];

export function formatTime(hour: number, minute: number): { timeStr: string; period: string } {
  const period = hour >= 12 ? 'PM' : 'AM';
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  const minuteStr = minute < 10 ? `0${minute}` : `${minute}`;
  return {
    timeStr: `${h12}:${minuteStr}`,
    period,
  };
}

export const DAY_NAMES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

export function formatRepeatDays(days: number[]): string {
  if (days.length === 0) return 'Once';
  if (days.length === 7) return 'Every day';
  if (days.length === 5 && !days.includes(0) && !days.includes(6)) return 'Weekdays';
  if (days.length === 2 && days.includes(0) && days.includes(6)) return 'Weekends';
  return days.map(d => DAY_NAMES[d]).join(', ');
}

export function getNextTriggerRemaining(alarm: AlarmItem): string {
  if (!alarm.enabled) return 'Off';
  const now = new Date();
  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  const alarmMinutes = alarm.hour * 60 + alarm.minute;
  
  let diffMinutes = alarmMinutes - currentMinutes;
  if (diffMinutes <= 0) {
    diffMinutes += 24 * 60;
  }

  const hours = Math.floor(diffMinutes / 60);
  const mins = diffMinutes % 60;

  if (hours > 0) {
    return `in ${hours}h ${mins}m`;
  }
  return `in ${mins}m`;
}

/**
 * Builds standard protocol payload matching WakeSyncPeerController.kt (Protocol version 1)
 */
export function buildPayload(
  alarm: AlarmItem,
  operation: SyncOperation,
  source: DeviceSource,
  originDeviceId: string
): SyncPayload {
  return {
    protocolVersion: 1,
    syncId: alarm.syncId,
    operation,
    source,
    originDeviceId,
    revision: alarm.revision,
    timestamp: alarm.updatedAt,
    hour: alarm.hour,
    minute: alarm.minute,
    label: alarm.label,
    enabled: alarm.enabled,
    repeatDays: alarm.repeatDays,
    snoozeDurationMinutes: alarm.snoozeDurationMinutes,
    vibrationEnabled: alarm.vibrationEnabled,
    volume: alarm.volume,
    alarmToken: alarm.alarmToken,
  };
}

/**
 * Deterministic conflict resolution engine as specified in AI_HANDOFF.md Section 3:
 * Highest revision wins; fallback to latest updatedAt timestamp; fallback to stable originDeviceId.
 */
export function resolveConflict(local: AlarmItem | undefined, incoming: AlarmItem): {
  winner: 'incoming' | 'local';
  merged: AlarmItem;
} {
  if (!local) {
    return { winner: 'incoming', merged: incoming };
  }

  // Check revision
  if (incoming.revision > local.revision) {
    return { winner: 'incoming', merged: incoming };
  }
  if (incoming.revision < local.revision) {
    return { winner: 'local', merged: local };
  }

  // Same revision -> check timestamp
  if (incoming.updatedAt > local.updatedAt) {
    return { winner: 'incoming', merged: incoming };
  }
  if (incoming.updatedAt < local.updatedAt) {
    return { winner: 'local', merged: local };
  }

  // Tie-breaker: device ID alphabetical ordering
  if (incoming.originDeviceId.localeCompare(local.originDeviceId) > 0) {
    return { winner: 'incoming', merged: incoming };
  }

  return { winner: 'local', merged: local };
}
