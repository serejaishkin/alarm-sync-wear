import { AlarmItem, ProtocolLog, SyncOperation, DeviceSource } from '../types';
import { buildPayload } from './alarmStorage';

type LogListener = (log: ProtocolLog) => void;
type SyncListener = (
  from: DeviceSource,
  operation: SyncOperation,
  alarm: AlarmItem,
  rawPayload: Record<string, unknown>
) => void;

class WearableDataTransport {
  private logListeners: Set<LogListener> = new Set();
  private syncListeners: Set<SyncListener> = new Set();
  public isConnected: boolean = true;
  public offlineQueue: Array<{
    from: DeviceSource;
    operation: SyncOperation;
    alarm: AlarmItem;
  }> = [];

  public subscribeLogs(fn: LogListener) {
    this.logListeners.add(fn);
    return () => this.logListeners.delete(fn);
  }

  public subscribeSync(fn: SyncListener) {
    this.syncListeners.add(fn);
    return () => this.syncListeners.delete(fn);
  }

  private emitLog(log: ProtocolLog) {
    this.logListeners.forEach(listener => listener(log));
  }

  public setConnected(connected: boolean) {
    this.isConnected = connected;
    if (connected && this.offlineQueue.length > 0) {
      // Flush queue upon reconnect
      const queued = [...this.offlineQueue];
      this.offlineQueue = [];
      
      this.emitLog({
        id: `recon-${Date.now()}`,
        timestamp: Date.now(),
        channel: 'DataClient',
        path: '/wakesync/reconciliation',
        source: 'PHONE',
        operation: 'SNAPSHOT_SYNC',
        summary: `Connection restored. Flushing ${queued.length} queued state mutations...`,
        payload: { queuedOperationsCount: queued.length },
        status: 'delivered'
      });

      queued.forEach(item => {
        this.dispatch(item.from, item.operation, item.alarm);
      });
    }
  }

  public dispatch(from: DeviceSource, operation: SyncOperation, alarm: AlarmItem) {
    const isLiveAction = ['RINGING', 'SNOOZE', 'DISMISS'].includes(operation);
    const channel = isLiveAction ? 'MessageClient' : 'DataClient';
    const path = isLiveAction 
      ? '/wakesync/alarm/mutation' 
      : `/wakesync/alarm/state/${alarm.syncId}`;

    const payload = buildPayload(
      alarm,
      operation,
      from,
      from === 'PHONE' ? 'phone-pixel-8' : 'watch-galaxy-6'
    );

    if (!this.isConnected) {
      // In offline mode: queue persistent data; live message fails
      if (!isLiveAction) {
        this.offlineQueue.push({ from, operation, alarm });
      }

      this.emitLog({
        id: `log-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
        timestamp: Date.now(),
        channel,
        path,
        source: from,
        operation,
        summary: !isLiveAction 
          ? `[OFFLINE] Queued for delivery when peer reconnects (${operation} ${alarm.label || 'Alarm'})`
          : `[OFFLINE] MessageClient failed: peer not reachable (${operation})`,
        payload: payload as unknown as Record<string, unknown>,
        status: 'queued'
      });
      return;
    }

    // Deliver immediately
    this.emitLog({
      id: `log-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
      timestamp: Date.now(),
      channel,
      path,
      source: from,
      operation,
      summary: `${from === 'PHONE' ? 'Phone ➔ Watch' : 'Watch ➔ Phone'}: ${operation} (${alarm.label || 'Alarm'} ${alarm.hour}:${alarm.minute < 10 ? '0' : ''}${alarm.minute}) rev:${alarm.revision}`,
      payload: payload as unknown as Record<string, unknown>,
      status: 'delivered'
    });

    this.syncListeners.forEach(listener => {
      listener(from, operation, alarm, payload as unknown as Record<string, unknown>);
    });
  }
}

export const dataTransport = new WearableDataTransport();
