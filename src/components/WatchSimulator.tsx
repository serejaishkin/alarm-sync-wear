import React, { useState } from 'react';
import { AlarmItem } from '../types';
import { formatTime, formatRepeatDays, getNextTriggerRemaining } from '../services/alarmStorage';
import { 
  Watch, 
  Bell, 
  Play, 
  Trash2, 
  Plus, 
  ChevronRight, 
  Layers, 
  List, 
  Music,
  RotateCcw,
  Check,
  AlertTriangle
} from 'lucide-react';

interface WatchSimulatorProps {
  alarms: AlarmItem[];
  isConnected: boolean;
  onToggleAlarm: (syncId: string) => void;
  onDeleteAlarm: (syncId: string) => void;
  onAddNew: () => void;
  onTriggerRing: (alarm: AlarmItem) => void;
  firingAlarm: AlarmItem | null;
  onSnooze: (alarm: AlarmItem) => void;
  onDismiss: (alarm: AlarmItem) => void;
  simulateMediaBug: boolean;
  onToggleMediaBug: () => void;
}

export const WatchSimulator: React.FC<WatchSimulatorProps> = ({
  alarms,
  isConnected,
  onToggleAlarm,
  onDeleteAlarm,
  onAddNew,
  onTriggerRing,
  firingAlarm,
  onSnooze,
  onDismiss,
  simulateMediaBug,
  onToggleMediaBug,
}) => {
  const [viewMode, setViewMode] = useState<'tile' | 'list'>('tile');
  const activeAlarms = alarms.filter(a => !a.deleted);
  
  // Find next enabled alarm for the tile
  const enabledAlarms = activeAlarms.filter(a => a.enabled);
  const nextAlarm = enabledAlarms[0] || null;

  return (
    <div className="flex flex-col items-center">
      {/* Device label & controls */}
      <div className="flex items-center justify-between w-full max-w-[340px] px-2 mb-2">
        <div className="flex items-center gap-1.5 text-xs font-medium text-neutral-300">
          <Watch className="w-4 h-4 text-blue-400" />
          <span>Wear OS Watch (Galaxy Watch 6)</span>
        </div>

        {/* View toggle (Tile vs App List) */}
        <div className="flex items-center bg-neutral-900 border border-neutral-800 rounded-lg p-0.5">
          <button
            id="watch-view-tile-btn"
            onClick={() => setViewMode('tile')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition ${
              viewMode === 'tile' ? 'bg-neutral-800 text-white shadow-xs' : 'text-neutral-400 hover:text-neutral-200'
            }`}
            title="Wear OS Next Alarm Tile"
          >
            Tile
          </button>
          <button
            id="watch-view-list-btn"
            onClick={() => setViewMode('list')}
            className={`px-2 py-1 text-[11px] rounded font-medium transition ${
              viewMode === 'list' ? 'bg-neutral-800 text-white shadow-xs' : 'text-neutral-400 hover:text-neutral-200'
            }`}
            title="Wear OS Alarm List"
          >
            App
          </button>
        </div>
      </div>

      {/* Watch outer strap connector & chassis */}
      <div className="relative flex flex-col items-center">
        {/* Top strap */}
        <div className="w-24 h-5 bg-neutral-800/90 rounded-t-lg border-t border-x border-neutral-700/60 shadow-inner" />

        {/* Watch circular body */}
        <div 
          id="watch-device-frame"
          className="w-[320px] h-[320px] rounded-full bg-neutral-950 border-[10px] border-neutral-800 relative overflow-hidden shadow-2xl ring-2 ring-neutral-700/50 flex items-center justify-center select-none"
        >
          {/* Subtle bezel ticks */}
          <div className="absolute inset-0 rounded-full border border-neutral-800 pointer-events-none opacity-40"></div>

          {/* Firing state */}
          {firingAlarm ? (
            simulateMediaBug ? (
              /* BUGGY BEHAVIOR AS DOCUMENTED IN AI_HANDOFF.MD SECTION 6:
                 Media Controller pops up on the wrist stealing focus from the Alarm UI! */
              <div 
                id="watch-media-bug-screen"
                className="w-full h-full bg-neutral-900 flex flex-col items-center justify-between p-6 text-center animate-in fade-in"
              >
                <div className="pt-2">
                  <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-400 bg-amber-400/10 px-2.5 py-0.5 rounded-full border border-amber-400/30">
                    <Music className="w-3 h-3" /> System Media Controller
                  </span>
                </div>

                <div className="flex flex-col items-center">
                  <div className="w-14 h-14 rounded-full bg-neutral-800 border border-neutral-700 flex items-center justify-center text-neutral-400 mb-2 shadow-inner">
                    <Music className="w-7 h-7 text-amber-400 animate-pulse" />
                  </div>
                  <p className="text-xs font-semibold text-neutral-200 truncate max-w-[190px]">
                    {firingAlarm.label || 'Alarm Audio Stream'}
                  </p>
                  <p className="text-[10px] text-red-400 font-medium mt-0.5">
                    ⚠️ Alarm UI obscured by MediaSession!
                  </p>
                </div>

                <div className="pb-2 space-y-1.5 w-full max-w-[200px]">
                  <button
                    id="watch-dismiss-bug-btn"
                    onClick={() => onDismiss(firingAlarm)}
                    className="w-full py-1.5 bg-neutral-800 hover:bg-neutral-700 text-[11px] font-medium text-neutral-200 rounded-full border border-neutral-700"
                  >
                    Dismiss via Notification
                  </button>
                  <button
                    onClick={onToggleMediaBug}
                    className="text-[9px] text-blue-400 underline hover:text-blue-300 block mx-auto"
                  >
                    Switch to Fixed Alarm Mode
                  </button>
                </div>
              </div>
            ) : (
              /* FIXED CLEAN WEAR OS ALARM RINGING UI */
              <div 
                id="watch-ringing-screen"
                className="w-full h-full bg-neutral-950 flex flex-col items-center justify-between p-5 text-center animate-in zoom-in-95 duration-150"
              >
                <div className="pt-2">
                  <span className="text-[10px] font-bold tracking-wider text-red-400 uppercase bg-red-500/20 px-2 py-0.5 rounded-full border border-red-500/30">
                    Alarm Ringing
                  </span>
                  <div className="text-3xl font-mono font-bold text-white mt-1">
                    {formatTime(firingAlarm.hour, firingAlarm.minute).timeStr}
                  </div>
                  <div className="text-[11px] font-medium text-neutral-300 truncate max-w-[160px]">
                    {firingAlarm.label}
                  </div>
                </div>

                {/* Pulsing Bell */}
                <div className="w-12 h-12 rounded-full bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/50 animate-bounce">
                  <Bell className="w-6 h-6" />
                </div>

                {/* Wrist touch actions */}
                <div className="w-full max-w-[190px] flex flex-col gap-1.5 pb-2">
                  <button
                    id="watch-snooze-btn"
                    onClick={() => onSnooze(firingAlarm)}
                    className="w-full py-2 px-3 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 font-semibold text-xs rounded-full border border-neutral-700 shadow-sm"
                  >
                    Snooze ({firingAlarm.snoozeDurationMinutes || 10}m)
                  </button>
                  <button
                    id="watch-dismiss-btn"
                    onClick={() => onDismiss(firingAlarm)}
                    className="w-full py-2 px-3 bg-red-600 hover:bg-red-500 text-white font-semibold text-xs rounded-full shadow-md shadow-red-600/30"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            )
          ) : viewMode === 'tile' ? (
            /* NEXT ALARM TILE VIEW (matching ACX NextAlarmTileService) */
            <div 
              id="watch-tile-view"
              className="w-full h-full p-6 flex flex-col items-center justify-between text-center bg-radial from-neutral-900 to-neutral-950"
            >
              {/* Tile header / Current Time */}
              <div className="pt-1 flex items-center justify-center gap-1 text-[11px] font-medium text-neutral-400">
                <Layers className="w-3 h-3 text-blue-400" />
                <span>Next Alarm Tile</span>
              </div>

              {nextAlarm ? (
                <div className="flex flex-col items-center">
                  <span className="text-[10px] text-blue-400 font-medium px-2 py-0.5 rounded-full bg-blue-500/15 mb-1">
                    {getNextTriggerRemaining(nextAlarm)}
                  </span>
                  <div className="text-4xl font-mono font-bold text-white tracking-tight">
                    {formatTime(nextAlarm.hour, nextAlarm.minute).timeStr}
                    <span className="text-xs ml-1 text-neutral-400">
                      {formatTime(nextAlarm.hour, nextAlarm.minute).period}
                    </span>
                  </div>
                  <span className="text-xs text-neutral-300 font-medium mt-1 truncate max-w-[160px]">
                    {nextAlarm.label}
                  </span>
                </div>
              ) : (
                <div className="flex flex-col items-center py-2">
                  <Bell className="w-6 h-6 text-neutral-600 mb-1" />
                  <span className="text-sm font-semibold text-neutral-300">No alarms</span>
                  <span className="text-[10px] text-neutral-500">Scheduled on phone or watch</span>
                </div>
              )}

              {/* Action buttons on tile */}
              <div className="pb-1 w-full max-w-[200px] flex items-center justify-center gap-1.5">
                {nextAlarm ? (
                  <>
                    <button
                      id="watch-tile-ring-test-btn"
                      onClick={() => onTriggerRing(nextAlarm)}
                      className="px-3 py-1.5 bg-blue-600/30 border border-blue-500/40 hover:bg-blue-600/50 text-blue-300 text-[11px] font-medium rounded-full flex items-center gap-1"
                      title="Test alarm ringing on watch"
                    >
                      <Play className="w-3 h-3" /> Test
                    </button>
                    <button
                      id="watch-tile-toggle-btn"
                      onClick={() => onToggleAlarm(nextAlarm.syncId)}
                      className="px-3 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-[11px] font-medium rounded-full border border-neutral-700"
                    >
                      Turn Off
                    </button>
                  </>
                ) : (
                  <button
                    id="watch-tile-create-btn"
                    onClick={onAddNew}
                    className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white text-[11px] font-medium rounded-full shadow-sm flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Add Alarm
                  </button>
                )}
              </div>
            </div>
          ) : (
            /* WEAR OS APP LIST VIEW */
            <div 
              id="watch-list-view"
              className="w-full h-full p-4 pt-6 flex flex-col overflow-hidden bg-neutral-950"
            >
              <div className="flex items-center justify-between px-3 mb-2 shrink-0">
                <span className="text-xs font-bold text-neutral-200">Alarms</span>
                <button
                  id="watch-add-alarm-btn"
                  onClick={onAddNew}
                  className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs hover:bg-blue-500"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Scrollable list inside circular mask */}
              <div className="flex-1 overflow-y-auto px-2 space-y-1.5 scrollbar-none">
                {activeAlarms.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-center p-3 text-neutral-500">
                    <p className="text-xs">No alarms found</p>
                  </div>
                ) : (
                  activeAlarms.map(alarm => {
                    const { timeStr, period } = formatTime(alarm.hour, alarm.minute);
                    return (
                      <div
                        key={alarm.syncId}
                        id={`watch-alarm-card-${alarm.syncId}`}
                        className={`p-2.5 rounded-xl border text-left transition ${
                          alarm.enabled 
                            ? 'bg-neutral-900 border-neutral-800' 
                            : 'bg-neutral-900/40 border-neutral-900 opacity-60'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="text-base font-mono font-bold text-white leading-tight">
                              {timeStr} <span className="text-[10px] text-neutral-400">{period}</span>
                            </div>
                            <div className="text-[10px] text-neutral-300 truncate max-w-[90px]">
                              {alarm.label}
                            </div>
                          </div>

                          <button
                            id={`watch-toggle-${alarm.syncId}`}
                            onClick={() => onToggleAlarm(alarm.syncId)}
                            className={`w-9 h-5 rounded-full p-0.5 flex items-center transition-colors ${
                              alarm.enabled ? 'bg-blue-600 justify-end' : 'bg-neutral-800 justify-start'
                            }`}
                          >
                            <div className="w-4 h-4 rounded-full bg-white shadow-xs" />
                          </button>
                        </div>

                        <div className="mt-1 flex items-center justify-between text-[9px] text-neutral-500 border-t border-neutral-800/50 pt-1">
                          <span>{formatRepeatDays(alarm.repeatDays)}</span>
                          <div className="flex items-center gap-1.5">
                            {alarm.enabled && (
                              <button
                                onClick={() => onTriggerRing(alarm)}
                                className="text-blue-400 hover:text-blue-300"
                                title="Ring"
                              >
                                Ring
                              </button>
                            )}
                            <button
                              onClick={() => onDeleteAlarm(alarm.syncId)}
                              className="text-red-400 hover:text-red-300"
                              title="Delete"
                            >
                              Del
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right hardware crown button */}
        <div 
          className="absolute right-[-10px] top-[140px] w-3 h-10 bg-neutral-700 rounded-r-md border-y border-r border-neutral-600 cursor-pointer shadow-md active:translate-x-[-1px]" 
          title="Hardware Crown Button (Back/Home)"
          onClick={() => setViewMode(viewMode === 'tile' ? 'list' : 'tile')}
        />

        {/* Bottom strap */}
        <div className="w-24 h-5 bg-neutral-800/90 rounded-b-lg border-b border-x border-neutral-700/60 shadow-inner" />
      </div>

      {/* Diagnostics toggle below watch */}
      <div className="mt-3 flex items-center gap-2">
        <button
          id="toggle-media-bug-simulator-btn"
          onClick={onToggleMediaBug}
          className={`px-3 py-1 text-[11px] rounded-full border transition flex items-center gap-1.5 ${
            simulateMediaBug 
              ? 'bg-amber-500/15 border-amber-500/40 text-amber-300' 
              : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200'
          }`}
        >
          <AlertTriangle className="w-3.5 h-3.5" />
          {simulateMediaBug ? 'Mode: Reproducing Media Bug' : 'Mode: Fixed Lock-Screen UI'}
        </button>
      </div>
    </div>
  );
};
