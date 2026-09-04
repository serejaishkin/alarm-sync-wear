import React, { useState, useEffect } from 'react';
import { AlarmItem, ProtocolLog, DeviceSource } from './types';
import { INITIAL_ALARMS, resolveConflict } from './services/alarmStorage';
import { dataTransport } from './services/dataTransport';
import { PhoneSimulator } from './components/PhoneSimulator';
import { WatchSimulator } from './components/WatchSimulator';
import { ProtocolInspector } from './components/ProtocolInspector';
import { AlarmModal } from './components/AlarmModal';
import { DiagnosticsModal } from './components/DiagnosticsModal';
import { Header } from './components/Header';
import { ArrowLeftRight, BellRing } from 'lucide-react';

export const App: React.FC = () => {
  const [phoneAlarms, setPhoneAlarms] = useState<AlarmItem[]>(() => {
    const saved = localStorage.getItem('wakesync_phone_alarms');
    return saved ? JSON.parse(saved) : INITIAL_ALARMS;
  });

  const [watchAlarms, setWatchAlarms] = useState<AlarmItem[]>(() => {
    const saved = localStorage.getItem('wakesync_watch_alarms');
    return saved ? JSON.parse(saved) : INITIAL_ALARMS;
  });

  const [logs, setLogs] = useState<ProtocolLog[]>([]);
  const [isConnected, setIsConnected] = useState<boolean>(true);
  const [firingAlarm, setFiringAlarm] = useState<AlarmItem | null>(null);
  
  // Modals
  const [isAlarmModalOpen, setIsAlarmModalOpen] = useState<boolean>(false);
  const [editingAlarm, setEditingAlarm] = useState<AlarmItem | null>(null);
  const [modalSource, setModalSource] = useState<DeviceSource>('PHONE');
  const [isDiagnosticsOpen, setIsDiagnosticsOpen] = useState<boolean>(false);

  // Bug simulation toggle from Section 6
  const [simulateMediaBug, setSimulateMediaBug] = useState<boolean>(false);

  // Save to local storage
  useEffect(() => {
    localStorage.setItem('wakesync_phone_alarms', JSON.stringify(phoneAlarms));
  }, [phoneAlarms]);

  useEffect(() => {
    localStorage.setItem('wakesync_watch_alarms', JSON.stringify(watchAlarms));
  }, [watchAlarms]);

  // Subscribe to data transport logs
  useEffect(() => {
    const unsubscribeLogs = dataTransport.subscribeLogs(log => {
      setLogs(prev => [log, ...prev].slice(0, 150));
    });

    // Initial announcement log
    setLogs([
      {
        id: 'init-0',
        timestamp: Date.now(),
        channel: 'DataClient',
        path: '/wakesync/ready',
        source: 'PHONE',
        operation: 'SNAPSHOT_SYNC',
        summary: 'WakeSync Data Layer initialized. Phone & Wear OS nodes connected.',
        payload: { protocolVersion: 1, activeAlarmsCount: phoneAlarms.length },
        status: 'delivered',
      }
    ]);

    return () => {
      unsubscribeLogs();
    };
  }, []);

  // Handle incoming sync dispatched from transport
  useEffect(() => {
    const unsubscribeSync = dataTransport.subscribeSync((from, operation, item) => {
      if (operation === 'RINGING') {
        setFiringAlarm(item);
        return;
      }

      if (operation === 'SNOOZE' || operation === 'DISMISS') {
        setFiringAlarm(null);
        return;
      }

      // Update peer side
      const targetSetter = from === 'PHONE' ? setWatchAlarms : setPhoneAlarms;
      targetSetter(currentAlarms => {
        const existing = currentAlarms.find(a => a.syncId === item.syncId);
        
        if (operation === 'DELETE') {
          return currentAlarms.map(a => a.syncId === item.syncId ? { ...a, deleted: true, revision: item.revision } : a);
        }

        const { winner, merged } = resolveConflict(existing, item);
        if (winner === 'incoming') {
          if (existing) {
            return currentAlarms.map(a => a.syncId === item.syncId ? merged : a);
          } else {
            return [...currentAlarms, merged];
          }
        }
        return currentAlarms;
      });
    });

    return () => {
      unsubscribeSync();
    };
  }, []);

  // Phone alarm toggled
  const handleToggleFromPhone = (syncId: string) => {
    setPhoneAlarms(prev => {
      return prev.map(alarm => {
        if (alarm.syncId === syncId) {
          const updated: AlarmItem = {
            ...alarm,
            enabled: !alarm.enabled,
            revision: alarm.revision + 1,
            updatedAt: Date.now(),
            source: 'PHONE',
            originDeviceId: 'phone-pixel-8',
          };
          dataTransport.dispatch('PHONE', updated.enabled ? 'ENABLE' : 'DISABLE', updated);
          return updated;
        }
        return alarm;
      });
    });
  };

  // Watch alarm toggled
  const handleToggleFromWatch = (syncId: string) => {
    setWatchAlarms(prev => {
      return prev.map(alarm => {
        if (alarm.syncId === syncId) {
          const updated: AlarmItem = {
            ...alarm,
            enabled: !alarm.enabled,
            revision: alarm.revision + 1,
            updatedAt: Date.now(),
            source: 'WATCH',
            originDeviceId: 'watch-galaxy-6',
          };
          dataTransport.dispatch('WATCH', updated.enabled ? 'ENABLE' : 'DISABLE', updated);
          return updated;
        }
        return alarm;
      });
    });
  };

  // Delete alarm
  const handleDeleteFromPhone = (syncId: string) => {
    const target = phoneAlarms.find(a => a.syncId === syncId);
    if (!target) return;
    const tombstone: AlarmItem = {
      ...target,
      deleted: true,
      revision: target.revision + 1,
      updatedAt: Date.now(),
      source: 'PHONE',
    };
    setPhoneAlarms(prev => prev.filter(a => a.syncId !== syncId));
    dataTransport.dispatch('PHONE', 'DELETE', tombstone);
  };

  const handleDeleteFromWatch = (syncId: string) => {
    const target = watchAlarms.find(a => a.syncId === syncId);
    if (!target) return;
    const tombstone: AlarmItem = {
      ...target,
      deleted: true,
      revision: target.revision + 1,
      updatedAt: Date.now(),
      source: 'WATCH',
    };
    setWatchAlarms(prev => prev.filter(a => a.syncId !== syncId));
    dataTransport.dispatch('WATCH', 'DELETE', tombstone);
  };

  // Save / Update alarm from modal
  const handleSaveAlarm = (alarm: AlarmItem) => {
    const isNew = !editingAlarm;
    const sourceSetter = modalSource === 'PHONE' ? setPhoneAlarms : setWatchAlarms;
    
    sourceSetter(prev => {
      const exists = prev.some(a => a.syncId === alarm.syncId);
      if (exists) {
        return prev.map(a => a.syncId === alarm.syncId ? alarm : a);
      }
      return [...prev, alarm];
    });

    dataTransport.dispatch(modalSource, isNew ? 'CREATE' : 'UPDATE', alarm);
  };

  // Trigger test ringing on both devices
  const handleTriggerRing = (alarm: AlarmItem) => {
    setFiringAlarm(alarm);
    dataTransport.dispatch(alarm.source, 'RINGING', alarm);
  };

  // Snooze
  const handleSnooze = (alarm: AlarmItem) => {
    setFiringAlarm(null);
    dataTransport.dispatch('PHONE', 'SNOOZE', alarm);
  };

  // Dismiss
  const handleDismiss = (alarm: AlarmItem) => {
    setFiringAlarm(null);
    dataTransport.dispatch('WATCH', 'DISMISS', alarm);
  };

  // Toggle connection state
  const handleToggleConnection = () => {
    const next = !isConnected;
    setIsConnected(next);
    dataTransport.setConnected(next);
  };

  // Force manual reconciliation
  const handleForceReconcile = () => {
    // Both sides merge latest revisions
    const allIds = Array.from(new Set([
      ...phoneAlarms.map(a => a.syncId),
      ...watchAlarms.map(a => a.syncId)
    ]));

    const mergedList: AlarmItem[] = [];
    allIds.forEach(id => {
      const phoneItem = phoneAlarms.find(a => a.syncId === id);
      const watchItem = watchAlarms.find(a => a.syncId === id);
      if (phoneItem && watchItem) {
        const { merged } = resolveConflict(phoneItem, watchItem);
        mergedList.push(merged);
      } else if (phoneItem) {
        mergedList.push(phoneItem);
      } else if (watchItem) {
        mergedList.push(watchItem);
      }
    });

    setPhoneAlarms(mergedList);
    setWatchAlarms(mergedList);

    setLogs(prev => [
      {
        id: `reconcile-${Date.now()}`,
        timestamp: Date.now(),
        channel: 'DataClient',
        path: '/wakesync/snapshot/reconcile',
        source: 'PHONE',
        operation: 'SNAPSHOT_SYNC',
        summary: `Bidirectional snapshot reconciliation complete: ${mergedList.length} alarms verified & harmonized.`,
        payload: { alarmsTotal: mergedList.length, syncTimestamp: Date.now() },
        status: 'delivered',
      },
      ...prev,
    ]);
  };

  // Reset to initial
  const handleResetData = () => {
    setPhoneAlarms(INITIAL_ALARMS);
    setWatchAlarms(INITIAL_ALARMS);
    setFiringAlarm(null);
    dataTransport.offlineQueue = [];
    dataTransport.setConnected(true);
    setIsConnected(true);
    setLogs([
      {
        id: `reset-${Date.now()}`,
        timestamp: Date.now(),
        channel: 'DataClient',
        path: '/wakesync/reset',
        source: 'PHONE',
        operation: 'SNAPSHOT_SYNC',
        summary: 'Alarms and sync state reset to initial baseline values.',
        payload: { totalAlarms: INITIAL_ALARMS.length },
        status: 'delivered',
      }
    ]);
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col font-sans">
      <Header
        onResetData={handleResetData}
        onOpenDiagnostics={() => setIsDiagnosticsOpen(true)}
        isConnected={isConnected}
        totalAlarms={phoneAlarms.filter(a => !a.deleted).length}
      />

      {/* Main workspace */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 flex flex-col gap-6">
        {/* Info banner explaining dual sync */}
        <div className="p-3.5 bg-neutral-900/90 border border-neutral-800 rounded-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2.5 text-neutral-300">
            <ArrowLeftRight className="w-4 h-4 text-blue-400 shrink-0" />
            <span>
              <strong>Real-time 2-Way Sync Engine:</strong> Change or toggle any alarm on the Phone or Watch. Notice how changes propagate instantaneously over Google Wearable DataClient and MessageClient channels.
            </span>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => {
                const sample = phoneAlarms[0];
                if (sample) handleTriggerRing(sample);
              }}
              className="px-3 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 font-medium flex items-center gap-1.5 transition"
            >
              <BellRing className="w-3.5 h-3.5" />
              <span>Simulate Alarm Firing</span>
            </button>
          </div>
        </div>

        {/* Dual Device Stage */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center justify-items-center py-2">
          {/* Phone Device */}
          <PhoneSimulator
            alarms={phoneAlarms}
            isConnected={isConnected}
            onToggleAlarm={handleToggleFromPhone}
            onDeleteAlarm={handleDeleteFromPhone}
            onEditAlarm={alarm => {
              setEditingAlarm(alarm);
              setModalSource('PHONE');
              setIsAlarmModalOpen(true);
            }}
            onAddNew={() => {
              setEditingAlarm(null);
              setModalSource('PHONE');
              setIsAlarmModalOpen(true);
            }}
            onTriggerRing={handleTriggerRing}
            firingAlarm={firingAlarm}
            onSnooze={handleSnooze}
            onDismiss={handleDismiss}
          />

          {/* Wear OS Watch Device */}
          <WatchSimulator
            alarms={watchAlarms}
            isConnected={isConnected}
            onToggleAlarm={handleToggleFromWatch}
            onDeleteAlarm={handleDeleteFromWatch}
            onAddNew={() => {
              setEditingAlarm(null);
              setModalSource('WATCH');
              setIsAlarmModalOpen(true);
            }}
            onTriggerRing={handleTriggerRing}
            firingAlarm={firingAlarm}
            onSnooze={handleSnooze}
            onDismiss={handleDismiss}
            simulateMediaBug={simulateMediaBug}
            onToggleMediaBug={() => setSimulateMediaBug(!simulateMediaBug)}
          />
        </div>

        {/* Protocol Inspector Drawer */}
        <div className="mt-2">
          <ProtocolInspector
            logs={logs}
            isConnected={isConnected}
            onToggleConnection={handleToggleConnection}
            onForceReconcile={handleForceReconcile}
            onClearLogs={() => setLogs([])}
            queuedCount={dataTransport.offlineQueue.length}
          />
        </div>
      </main>

      {/* Modals */}
      <AlarmModal
        alarm={editingAlarm}
        isOpen={isAlarmModalOpen}
        onClose={() => setIsAlarmModalOpen(false)}
        onSave={handleSaveAlarm}
        source={modalSource}
      />

      <DiagnosticsModal
        isOpen={isDiagnosticsOpen}
        onClose={() => setIsDiagnosticsOpen(false)}
        simulateMediaBug={simulateMediaBug}
        onToggleMediaBug={() => setSimulateMediaBug(!simulateMediaBug)}
      />
    </div>
  );
};
