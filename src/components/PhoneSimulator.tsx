import React from 'react';
import { AlarmItem } from '../types';
import { formatTime, formatRepeatDays, getNextTriggerRemaining } from '../services/alarmStorage';
import { 
  Wifi, 
  Battery, 
  Plus, 
  Bell, 
  Trash2, 
  Edit3, 
  Play, 
  Smartphone,
  CheckCircle2,
  Volume2
} from 'lucide-react';

interface PhoneSimulatorProps {
  alarms: AlarmItem[];
  isConnected: boolean;
  onToggleAlarm: (syncId: string) => void;
  onDeleteAlarm: (syncId: string) => void;
  onEditAlarm: (alarm: AlarmItem) => void;
  onAddNew: () => void;
  onTriggerRing: (alarm: AlarmItem) => void;
  firingAlarm: AlarmItem | null;
  onSnooze: (alarm: AlarmItem) => void;
  onDismiss: (alarm: AlarmItem) => void;
}

export const PhoneSimulator: React.FC<PhoneSimulatorProps> = ({
  alarms,
  isConnected,
  onToggleAlarm,
  onDeleteAlarm,
  onEditAlarm,
  onAddNew,
  onTriggerRing,
  firingAlarm,
  onSnooze,
  onDismiss,
}) => {
  const activeAlarms = alarms.filter(a => !a.deleted);

  return (
    <div className="flex flex-col items-center">
      {/* Device label & status */}
      <div className="flex items-center justify-between w-full max-w-[340px] px-2 mb-2">
        <div className="flex items-center gap-1.5 text-xs font-medium text-neutral-300">
          <Smartphone className="w-4 h-4 text-blue-400" />
          <span>Android Phone (Pixel 8)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span 
            className={`w-2 h-2 rounded-full ${
              isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'
            }`} 
          />
          <span className="text-[11px] text-neutral-400">
            {isConnected ? 'Wear DataLayer Live' : 'Offline / Queued'}
          </span>
        </div>
      </div>

      {/* Phone chassis */}
      <div 
        id="phone-device-frame"
        className="w-[340px] h-[640px] bg-neutral-900 border-[6px] border-neutral-700/80 rounded-[42px] overflow-hidden shadow-2xl relative flex flex-col select-none ring-1 ring-white/10"
      >
        {/* Status Bar */}
        <div className="h-9 px-6 bg-neutral-950 flex items-center justify-between text-xs text-neutral-300 font-medium z-20 shrink-0 border-b border-neutral-800/60">
          <div className="flex items-center gap-1">
            <span>07:45</span>
          </div>
          {/* Front Camera Notch */}
          <div className="w-16 h-4 bg-black rounded-full flex items-center justify-center">
            <div className="w-2.5 h-2.5 rounded-full bg-neutral-900 border border-neutral-700"></div>
          </div>
          <div className="flex items-center gap-1.5 text-neutral-400">
            <span className="text-[10px] font-bold text-neutral-400">5G</span>
            <Wifi className="w-3.5 h-3.5" />
            <Battery className="w-4 h-4 text-emerald-400" />
          </div>
        </div>

        {/* Firing Alarm Full-screen View */}
        {firingAlarm ? (
          <div 
            id="phone-ringing-screen"
            className="absolute inset-0 z-30 bg-neutral-950/95 backdrop-blur-md flex flex-col items-center justify-between p-6 text-center animate-in fade-in duration-200"
          >
            <div className="pt-12">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-500/20 text-red-400 text-xs font-semibold mb-6 border border-red-500/30 animate-pulse">
                <Bell className="w-3.5 h-3.5 animate-bounce" /> ALARM RINGING
              </div>
              <div className="text-5xl font-mono font-bold tracking-tight text-white mb-2">
                {formatTime(firingAlarm.hour, firingAlarm.minute).timeStr}
                <span className="text-xl ml-1 text-neutral-400">
                  {formatTime(firingAlarm.hour, firingAlarm.minute).period}
                </span>
              </div>
              <h3 className="text-lg font-medium text-neutral-200">{firingAlarm.label}</h3>
              <p className="text-xs text-neutral-400 mt-1">Synced across Phone & Watch</p>
            </div>

            {/* Pulse graphic */}
            <div className="relative my-auto flex items-center justify-center">
              <div className="absolute w-36 h-36 rounded-full bg-blue-500/20 animate-ping" />
              <div className="absolute w-28 h-28 rounded-full bg-blue-500/30 animate-pulse" />
              <div className="relative w-20 h-20 rounded-full bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-500/50">
                <Bell className="w-10 h-10 text-white" />
              </div>
            </div>

            {/* Actions */}
            <div className="w-full space-y-3 pb-6">
              <button
                id="phone-snooze-btn"
                onClick={() => onSnooze(firingAlarm)}
                className="w-full py-3.5 px-4 bg-neutral-800 hover:bg-neutral-700 text-neutral-100 font-semibold rounded-2xl transition border border-neutral-700 shadow-md text-sm active:scale-98"
              >
                Snooze ({firingAlarm.snoozeDurationMinutes || 10}m)
              </button>
              <button
                id="phone-dismiss-btn"
                onClick={() => onDismiss(firingAlarm)}
                className="w-full py-3.5 px-4 bg-red-600 hover:bg-red-500 text-white font-semibold rounded-2xl transition shadow-lg shadow-red-600/30 text-sm active:scale-98"
              >
                Dismiss Alarm
              </button>
            </div>
          </div>
        ) : (
          /* Normal Alarm App Content */
          <div className="flex-1 flex flex-col overflow-hidden bg-neutral-950">
            {/* Top Bar */}
            <div className="px-5 pt-3 pb-2 flex items-center justify-between border-b border-neutral-900">
              <div>
                <h1 className="text-lg font-bold text-neutral-100 tracking-tight">WakeSync Alarms</h1>
                <p className="text-[11px] text-neutral-400 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3 text-blue-400" /> Auto-sync enabled
                </p>
              </div>
              <button
                id="phone-add-alarm-btn"
                onClick={onAddNew}
                className="w-9 h-9 rounded-full bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center transition shadow-md shadow-blue-600/30 active:scale-95"
                title="Add new alarm"
              >
                <Plus className="w-5 h-5" />
              </button>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2.5 scrollbar-thin scrollbar-thumb-neutral-800">
              {activeAlarms.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-center p-6 text-neutral-500">
                  <Bell className="w-10 h-10 text-neutral-700 mb-2" />
                  <p className="text-sm font-medium text-neutral-400">No alarms created</p>
                  <p className="text-xs text-neutral-600 mt-1">
                    Tap the plus button above or add one directly from the Wear OS watch!
                  </p>
                </div>
              ) : (
                activeAlarms.map(alarm => {
                  const { timeStr, period } = formatTime(alarm.hour, alarm.minute);
                  const remaining = getNextTriggerRemaining(alarm);

                  return (
                    <div
                      key={alarm.syncId}
                      id={`phone-alarm-card-${alarm.syncId}`}
                      className={`p-3.5 rounded-2xl border transition-all ${
                        alarm.enabled
                          ? 'bg-neutral-900/90 border-neutral-800/90 hover:border-neutral-700'
                          : 'bg-neutral-900/40 border-neutral-900/80 opacity-60'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="flex items-baseline gap-1">
                            <span className="text-2xl font-mono font-bold text-neutral-100">
                              {timeStr}
                            </span>
                            <span className="text-xs font-semibold text-neutral-400">
                              {period}
                            </span>
                          </div>
                          <div className="text-xs font-medium text-neutral-300 mt-0.5 truncate max-w-[150px]">
                            {alarm.label}
                          </div>
                        </div>

                        {/* Toggle switch */}
                        <button
                          id={`phone-toggle-${alarm.syncId}`}
                          type="button"
                          onClick={() => onToggleAlarm(alarm.syncId)}
                          className={`w-12 h-6.5 rounded-full p-1 transition-colors flex items-center ${
                            alarm.enabled ? 'bg-blue-600 justify-end' : 'bg-neutral-800 justify-start'
                          }`}
                        >
                          <div className="w-4.5 h-4.5 rounded-full bg-white shadow-sm" />
                        </button>
                      </div>

                      {/* Footer & details */}
                      <div className="mt-2.5 pt-2 border-t border-neutral-800/60 flex items-center justify-between text-[11px]">
                        <div className="flex items-center gap-1.5 text-neutral-400">
                          <span>{formatRepeatDays(alarm.repeatDays)}</span>
                          <span className="text-neutral-600">•</span>
                          <span className={alarm.enabled ? 'text-blue-400 font-medium' : 'text-neutral-500'}>
                            {remaining}
                          </span>
                        </div>

                        <div className="flex items-center gap-1 text-neutral-400">
                          {alarm.enabled && (
                            <button
                              id={`phone-test-ring-${alarm.syncId}`}
                              onClick={() => onTriggerRing(alarm)}
                              title="Trigger Ringing test"
                              className="p-1 hover:text-amber-400 hover:bg-neutral-800 rounded transition"
                            >
                              <Play className="w-3.5 h-3.5" />
                            </button>
                          )}
                          <button
                            id={`phone-edit-${alarm.syncId}`}
                            onClick={() => onEditAlarm(alarm)}
                            title="Edit"
                            className="p-1 hover:text-neutral-200 hover:bg-neutral-800 rounded transition"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            id={`phone-delete-${alarm.syncId}`}
                            onClick={() => onDeleteAlarm(alarm.syncId)}
                            title="Delete"
                            className="p-1 hover:text-red-400 hover:bg-neutral-800 rounded transition"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {/* Sync info tag */}
                      <div className="mt-1.5 flex items-center justify-between text-[9px] text-neutral-500">
                        <span>Rev {alarm.revision} • Source: {alarm.source}</span>
                        <span>{alarm.volume}% vol</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Bottom Navigation Indicator Bar */}
            <div className="h-6 flex items-center justify-center bg-neutral-950 shrink-0">
              <div className="w-28 h-1 bg-neutral-700 rounded-full" />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
