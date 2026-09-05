import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw, Plus, BellRing } from 'lucide-react';
import { soundEffects } from '../services/soundEffects';

export const TimerTab: React.FC = () => {
  const [totalSeconds, setTotalSeconds] = useState(300); // default 5m
  const [remainingSeconds, setRemainingSeconds] = useState(300);
  const [isRunning, setIsRunning] = useState(false);
  const [inputDigits, setInputDigits] = useState('');
  const [isFinished, setIsFinished] = useState(false);

  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (isRunning) {
      timerRef.current = setInterval(() => {
        setRemainingSeconds(prev => {
          if (prev <= 1) {
            setIsRunning(false);
            setIsFinished(true);
            soundEffects.playTimerAlert();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isRunning]);

  const handleStart = () => {
    if (remainingSeconds === 0) {
      setRemainingSeconds(totalSeconds);
    }
    setIsFinished(false);
    setIsRunning(true);
    soundEffects.playClickSound();
  };

  const handlePause = () => {
    setIsRunning(false);
    soundEffects.playClickSound();
  };

  const handleReset = () => {
    setIsRunning(false);
    setIsFinished(false);
    setRemainingSeconds(totalSeconds);
    soundEffects.playClickSound();
  };

  const handleAddMinute = () => {
    setRemainingSeconds(prev => prev + 60);
    setTotalSeconds(prev => prev + 60);
    soundEffects.playClickSound();
  };

  const handlePreset = (seconds: number) => {
    setIsRunning(false);
    setIsFinished(false);
    setTotalSeconds(seconds);
    setRemainingSeconds(seconds);
    setInputDigits('');
    soundEffects.playClickSound();
  };

  const handleDigitPress = (digit: string) => {
    if (isRunning) return;
    if (inputDigits.length >= 6) return;
    const newDigits = inputDigits + digit;
    setInputDigits(newDigits);
    
    // Parse HHMMSS
    const padded = newDigits.padStart(6, '0');
    const h = parseInt(padded.slice(0, 2), 10);
    const m = parseInt(padded.slice(2, 4), 10);
    const s = parseInt(padded.slice(4, 6), 10);
    const secs = h * 3600 + m * 60 + s;
    setTotalSeconds(secs);
    setRemainingSeconds(secs);
  };

  const handleClearDigits = () => {
    setInputDigits('');
    setTotalSeconds(0);
    setRemainingSeconds(0);
  };

  const formatDisplay = (sec: number) => {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    if (h > 0) {
      return `${h}:${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
    }
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const progress = totalSeconds > 0 ? (remainingSeconds / totalSeconds) * 100 : 0;
  const radius = 78;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (progress / 100) * circumference;

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-neutral-950 text-neutral-100">
      {/* Top Bar */}
      <div className="px-5 pt-3 pb-2 flex items-center justify-between border-b border-neutral-900 shrink-0">
        <div>
          <h1 className="text-lg font-bold text-neutral-100 tracking-tight">Таймер</h1>
          <p className="text-[11px] text-neutral-400">Google Clock Style</p>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
            {totalSeconds > 0 ? `${Math.round(totalSeconds / 60)} мин` : '0 мин'}
          </span>
        </div>
      </div>

      {/* Main Circular Timer Display */}
      <div className="flex-1 flex flex-col items-center justify-center p-4 relative">
        {/* Progress Ring */}
        <div className="relative w-48 h-48 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90">
            <circle
              cx="96"
              cy="96"
              r={radius}
              stroke="currentColor"
              strokeWidth="6"
              fill="transparent"
              className="text-neutral-800"
            />
            <circle
              cx="96"
              cy="96"
              r={radius}
              stroke="currentColor"
              strokeWidth="6"
              fill="transparent"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
              className={`transition-all duration-300 ${
                isFinished ? 'text-red-500 animate-pulse' : 'text-blue-500'
              }`}
            />
          </svg>

          {/* Time Digits Inside Ring */}
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            {isFinished ? (
              <div className="animate-bounce flex flex-col items-center">
                <BellRing className="w-8 h-8 text-red-400 mb-1 animate-pulse" />
                <span className="text-sm font-bold text-red-400 uppercase tracking-wider">Время вышло!</span>
              </div>
            ) : (
              <>
                <span className="text-3xl font-mono font-bold tracking-tight text-white">
                  {formatDisplay(remainingSeconds)}
                </span>
                <span className="text-[10px] text-neutral-400 mt-0.5">
                  {isRunning ? 'Идет отсчет' : remainingSeconds === 0 ? 'Готов к запуску' : 'На паузе'}
                </span>
              </>
            )}
          </div>
        </div>

        {/* Quick presets */}
        <div className="flex items-center gap-1.5 mt-3 flex-wrap justify-center">
          {[
            { label: '+1 мин', sec: 60 },
            { label: '+5 мин', sec: 300 },
            { label: '+10 мин', sec: 600 },
            { label: '+15 мин', sec: 900 },
          ].map(p => (
            <button
              key={p.sec}
              onClick={() => handlePreset(p.sec)}
              className="px-2.5 py-1 text-[11px] font-medium rounded-full bg-neutral-900 border border-neutral-800 hover:border-neutral-700 hover:bg-neutral-800 text-neutral-300 transition active:scale-95"
            >
              {p.label}
            </button>
          ))}
        </div>

        {/* Number Pad for quick entry when stopped */}
        {!isRunning && remainingSeconds === 0 && (
          <div className="grid grid-cols-3 gap-1.5 mt-3 w-48">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0'].map(d => (
              <button
                key={d}
                onClick={() => handleDigitPress(d)}
                className="py-1.5 bg-neutral-900 hover:bg-neutral-800 text-xs font-mono font-semibold rounded-lg border border-neutral-800 text-neutral-200"
              >
                {d}
              </button>
            ))}
            <button
              onClick={handleClearDigits}
              className="py-1.5 bg-neutral-900 hover:bg-neutral-800 text-[10px] font-semibold rounded-lg border border-neutral-800 text-neutral-400"
            >
              Сброс
            </button>
          </div>
        )}
      </div>

      {/* Bottom Controls */}
      <div className="px-6 py-4 border-t border-neutral-900 bg-neutral-950 flex items-center justify-between shrink-0">
        <button
          onClick={handleReset}
          disabled={remainingSeconds === totalSeconds && !isFinished}
          className="p-3 rounded-full bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-white transition disabled:opacity-40"
          title="Сбросить"
        >
          <RotateCcw className="w-5 h-5" />
        </button>

        {isRunning ? (
          <button
            onClick={handlePause}
            className="w-14 h-14 rounded-full bg-amber-600 hover:bg-amber-500 text-white flex items-center justify-center transition shadow-lg shadow-amber-600/30 active:scale-95"
            title="Пауза"
          >
            <Pause className="w-6 h-6" />
          </button>
        ) : (
          <button
            onClick={handleStart}
            disabled={totalSeconds === 0}
            className="w-14 h-14 rounded-full bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center transition shadow-lg shadow-blue-600/30 active:scale-95 disabled:opacity-50"
            title="Старт"
          >
            <Play className="w-6 h-6 ml-0.5" />
          </button>
        )}

        <button
          onClick={handleAddMinute}
          disabled={!isRunning && remainingSeconds === 0}
          className="px-3 py-2 rounded-2xl bg-neutral-900 hover:bg-neutral-800 text-xs font-semibold text-neutral-200 border border-neutral-800 flex items-center gap-1 transition active:scale-95 disabled:opacity-40"
          title="Добавить 1 минуту"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>1:00</span>
        </button>
      </div>
    </div>
  );
};
