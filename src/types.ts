export type DeviceSource = 'PHONE' | 'WATCH';

export type SyncOperation = 
  | 'CREATE' 
  | 'UPDATE' 
  | 'ENABLE' 
  | 'DISABLE' 
  | 'DELETE' 
  | 'RINGING' 
  | 'SNOOZE' 
  | 'DISMISS'
  | 'SNAPSHOT_SYNC';

export interface AlarmItem {
  syncId: string;
  hour: number;
  minute: number;
  label: string;
  enabled: boolean;
  repeatDays: number[]; // 0: Sun, 1: Mon, ..., 6: Sat
  snoozeDurationMinutes: number;
  vibrationEnabled: boolean;
  volume: number;
  alarmToken?: string;
  revision: number;
  updatedAt: number;
  source: DeviceSource;
  originDeviceId: string;
  // Runtime ringing states
  isFiring?: boolean;
  snoozedUntil?: number | null;
  deleted?: boolean;
}

export interface SyncPayload {
  protocolVersion: number;
  syncId: string;
  operation: SyncOperation;
  source: DeviceSource;
  originDeviceId: string;
  revision: number;
  timestamp: number;
  hour?: number;
  minute?: number;
  label?: string;
  enabled?: boolean;
  repeatDays?: number[];
  snoozeDurationMinutes?: number;
  vibrationEnabled?: boolean;
  volume?: number;
  alarmToken?: string;
}

export interface ProtocolLog {
  id: string;
  timestamp: number;
  channel: 'DataClient' | 'MessageClient';
  path: string;
  source: DeviceSource;
  operation: SyncOperation;
  summary: string;
  payload: Record<string, unknown>;
  status: 'delivered' | 'queued' | 'conflict_resolved';
}

export interface DeviceState {
  deviceId: string;
  deviceName: string;
  deviceType: DeviceSource;
  alarms: AlarmItem[];
  isLocked: boolean;
  firingAlarmId: string | null;
  activeView: 'list' | 'tile' | 'editor';
}
