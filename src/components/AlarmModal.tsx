import React, { useState } from 'react';
import { AlarmItem, DeviceSource } from '../types';
import { DAY_NAMES } from '../services/alarmStorage';
import { X, Clock, Bell, Volume2, Calendar } from 'lucide-react';

interface AlarmModalProps {
  alarm?: AlarmItem | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (alarm: AlarmItem) => void;
  source: DeviceSource;
}

export const AlarmModal: React.FC<AlarmModalProps> = ({
  alarm,
  isOpen,
  onClose,
  onSave,
  source,
}) => {
  if (!isOpen) return null;

  const [hour, setHour] = useState<number>(alarm ? alarm.hour : 7);
  const [minute, setMinute] = useState<number>(alarm ? alarm.minute : 0);
  const [label, setLabel] = useState<string>(alarm ? alarm.label : '');
  const [repeatDays, setRepeatDays] = useState<number[]>(alarm ? alarm.repeatDays : [1, 2, 3, 4, 5]);
  const [snoozeDuration, setSnoozeDuration] = useState<number>(alarm ? alarm.snoozeDurationMinutes : 10);
  const [vibrationEnabled, setVibrationEnabled] = useState<boolean>(alarm ? alarm.vibrationEnabled : true);
  const [volume, setVolume] = useState<number>(alarm ? alarm.volume : 80);

  const toggleDay = (dayIndex: number) => {
    if (repeatDays.includes(dayIndex)) {
      setRepeatDays(repeatDays.filter(d => d !== dayIndex));
    } else {
      setRepeatDays([...repeatDays, dayIndex].sort());
    }
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    const newAlarm: AlarmItem = {
      syncId: alarm ? alarm.syncId : `alarm-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
      hour: Number(hour),
      minute: Number(minute),
      label: label.trim() || 'Alarm',
      enabled: true,
      repeatDays,
      snoozeDurationMinutes: snoozeDuration,
      vibrationEnabled,
      volume,
      revision: alarm ? alarm.revision + 1 : 1,
      updatedAt: Date.now(),
      source,
      originDeviceId: source === 'PHONE' ? 'phone-pixel-8' : 'watch-galaxy-6',
      isFiring: false,
      snoozedUntil: null,
    };
    onSave(newAlarm);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
      <div 
        id="alarm-edit-modal"
        className="bg-neutral-900 border border-neutral-800 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl text-neutral-100 animate-in fade-in zoom-in-95 duration-150"
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
          <div className="flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-400" />
            <h2 className="font-semibold text-lg">
              {alarm ? 'Edit Alarm' : 'New Alarm'}
            </h2>
          </div>
          <span className="text-xs px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300 font-medium">
            Created on {source === 'PHONE' ? 'Phone' : 'Wear OS'}
          </span>
          <button
            id="close-alarm-modal-btn"
            onClick={onClose}
            className="p-1 rounded-lg text-neutral-400 hover:text-neutral-100 hover:bg-neutral-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSave} className="p-5 space-y-5">
          {/* Time pickers */}
          <div className="flex items-center justify-center gap-3 bg-neutral-950 p-4 rounded-xl border border-neutral-800/80">
            <div className="flex flex-col items-center">
              <label className="text-xs text-neutral-400 mb-1">Hour (0-23)</label>
              <input
                id="alarm-hour-input"
                type="number"
                min={0}
                max={23}
                value={hour}
                onChange={e => setHour(Math.min(23, Math.max(0, parseInt(e.target.value) || 0)))}
                className="w-18 text-center text-3xl font-mono font-bold bg-neutral-800 border border-neutral-700 rounded-lg py-2 focus:outline-none focus:border-blue-500"
              />
            </div>
            <span className="text-3xl font-bold text-neutral-500 mt-5">:</span>
            <div className="flex flex-col items-center">
              <label className="text-xs text-neutral-400 mb-1">Minute (0-59)</label>
              <input
                id="alarm-minute-input"
                type="number"
                min={0}
                max={59}
                value={minute}
                onChange={e => setMinute(Math.min(59, Math.max(0, parseInt(e.target.value) || 0)))}
                className="w-18 text-center text-3xl font-mono font-bold bg-neutral-800 border border-neutral-700 rounded-lg py-2 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          {/* Label input */}
          <div>
            <label className="block text-xs font-medium text-neutral-400 mb-1.5 flex items-center gap-1.5">
              <Bell className="w-3.5 h-3.5" /> Alarm Label
            </label>
            <input
              id="alarm-label-input"
              type="text"
              placeholder="e.g. Work, Workout, Medication"
              value={label}
              onChange={e => setLabel(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:border-blue-500 placeholder:text-neutral-600"
            />
          </div>

          {/* Repeat days */}
          <div>
            <label className="block text-xs font-medium text-neutral-400 mb-2 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5" /> Repeat Days
            </label>
            <div className="flex items-center justify-between gap-1">
              {DAY_NAMES.map((day, idx) => {
                const isSelected = repeatDays.includes(idx);
                return (
                  <button
                    key={day}
                    type="button"
                    id={`repeat-day-${day.toLowerCase()}`}
                    onClick={() => toggleDay(idx)}
                    className={`w-9 h-9 text-xs font-semibold rounded-lg transition-all ${
                      isSelected
                        ? 'bg-blue-600 text-white shadow-xs shadow-blue-500/50'
                        : 'bg-neutral-800 text-neutral-400 hover:bg-neutral-700'
                    }`}
                  >
                    {day[0]}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Snooze & Volume */}
          <div className="grid grid-cols-2 gap-3 pt-1">
            <div>
              <label className="block text-xs font-medium text-neutral-400 mb-1.5">
                Snooze Duration
              </label>
              <select
                id="alarm-snooze-select"
                value={snoozeDuration}
                onChange={e => setSnoozeDuration(Number(e.target.value))}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500"
              >
                <option value={5}>5 minutes</option>
                <option value={10}>10 minutes</option>
                <option value={15}>15 minutes</option>
                <option value={20}>20 minutes</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-neutral-400 mb-1.5 flex items-center gap-1">
                <Volume2 className="w-3.5 h-3.5" /> Volume ({volume}%)
              </label>
              <input
                id="alarm-volume-slider"
                type="range"
                min={20}
                max={100}
                value={volume}
                onChange={e => setVolume(Number(e.target.value))}
                className="w-full mt-2 accent-blue-500"
              />
            </div>
          </div>

          {/* Vibration toggle */}
          <div className="flex items-center justify-between py-1 border-t border-neutral-800/80">
            <span className="text-xs text-neutral-300">Vibration alert</span>
            <input
              id="alarm-vibration-toggle"
              type="checkbox"
              checked={vibrationEnabled}
              onChange={e => setVibrationEnabled(e.target.checked)}
              className="w-4 h-4 accent-blue-500 rounded cursor-pointer"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-neutral-800">
            <button
              type="button"
              id="cancel-alarm-btn"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-neutral-400 hover:text-white rounded-lg hover:bg-neutral-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              id="save-alarm-submit-btn"
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition shadow-md shadow-blue-600/30"
            >
              Save & Broadcast Sync
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
