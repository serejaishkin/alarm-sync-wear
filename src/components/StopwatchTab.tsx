import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw, Flag } from 'lucide-react';
import { soundEffects } from '../services/soundEffects';

interface LapRecord {
  lapIndex: number;
  lapTimeMs: number;
  overallTimeMs: number;
}

export const StopwatchTab: React.FC = () => {
  const [elapsedMs, setElapsedMs] = useState(0);
  const [isRunning, setIsRunning] = useState(false);
  const [laps, setLaps] = useState<LapRecord[]>([]);

  const startTimestampRef = useRef<number>(0);
  const accumulatedMsRef = useRef<number>(0);
  const animationFrameRef = useRef<number | null>(null);

  useEffect(() => {
    if (isRunning) {
      startTimestampRef.current = performance.now();
      const update = () => {
        const now = performance.now();
        const currentDelta = now - startTimestampRef.current;
        setElapsedMs(accumulatedMsRef.current + currentDelta);
        animationFrameRef.current = requestAnimationFrame(update);
      };
      animationFrameRef.current = requestAnimationFrame(update);
    } else if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }
    return () => {
      if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
    };
  }, [isRunning]);

  const handleStart = () => {
    setIsRunning(true);
    soundEffects.playClickSound();
  };

  const handlePause = () => {
    setIsRunning(false);
    accumulatedMsRef.current = elapsedMs;
    soundEffects.playClickSound();
  };

  const handleReset = () => {
    setIsRunning(false);
    accumulatedMsRef.current = 0;
    setElapsedMs(0);
    setLaps([]);
    soundEffects.playClickSound();
  };

  const handleLap = () => {
    const lastOverall = laps.length > 0 ? laps[0].overallTimeMs : 0;
    const lapTime = elapsedMs - lastOverall;
    const newLap: LapRecord = {
      lapIndex: laps.length + 1,
      lapTimeMs: lapTime,
      overallTimeMs: elapsedMs,
    };
    setLaps([newLap, ...laps]);
    soundEffects.playClickSound();
  };

  const formatMs = (totalMs: number) => {
    const mins = Math.floor(totalMs / 60000);
    const secs = Math.floor((totalMs % 60000) / 1000);
    const hundredths = Math.floor((totalMs % 1000) / 10);
    return {
      minutes: `${mins < 10 ? '0' : ''}${mins}`,
      seconds: `${secs < 10 ? '0' : ''}${secs}`,
      hundredths: `${hundredths < 10 ? '0' : ''}${hundredths}`,
    };
  };

  const { minutes, seconds, hundredths } = formatMs(elapsedMs);

  // Identify fastest and slowest laps if at least 2 laps
  let fastestLapIndex = -1;
  let slowestLapIndex = -1;
  if (laps.length >= 2) {
    let minTime = Infinity;
    let maxTime = -Infinity;
    laps.forEach(l => {
      if (l.lapTimeMs < minTime) {
        minTime = l.lapTimeMs;
        fastestLapIndex = l.lapIndex;
      }
      if (l.lapTimeMs > maxTime) {
        maxTime = l.lapTimeMs;
        slowestLapIndex = l.lapIndex;
      }
    });
  }

  // Animation ring degree based on seconds
  const currentSecondsFloat = (elapsedMs % 60000) / 1000;
  const ringPercent = (currentSecondsFloat / 60) * 100;
  const radius = 78;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (ringPercent / 100) * circumference;

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-neutral-950 text-neutral-100">
      {/* Top Bar */}
      <div className="px-5 pt-3 pb-2 flex items-center justify-between border-b border-neutral-900 shrink-0">
        <div>
          <h1 className="text-lg font-bold text-neutral-100 tracking-tight">Секундомер</h1>
          <p className="text-[11px] text-neutral-400">Точность до 0.01 сек</p>
        </div>
        {laps.length > 0 && (
          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-neutral-900 border border-neutral-800 text-neutral-400">
            Кругов: {laps.length}
          </span>
        )}
      </div>

      {/* Main Dial Display */}
      <div className="py-4 px-4 flex flex-col items-center justify-center shrink-0 border-b border-neutral-900/60 bg-neutral-900/20">
        <div className="relative w-44 h-44 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90">
            <circle
              cx="88"
              cy="88"
              r={radius}
              stroke="currentColor"
              strokeWidth="5"
              fill="transparent"
              className="text-neutral-800/80"
            />
            <circle
              cx="88"
              cy="88"
              r={radius}
              stroke="currentColor"
              strokeWidth="5"
              fill="transparent"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
              className="text-blue-500 transition-all duration-75"
            />
          </svg>

          {/* Large Digits Inside Dial */}
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <div className="flex items-baseline font-mono font-bold tracking-tight text-white">
              <span className="text-3xl text-neutral-100">{minutes}:{seconds}</span>
              <span className="text-lg font-semibold text-blue-400 ml-1 font-mono">.{hundredths}</span>
            </div>
            <span className="text-[10px] text-neutral-500 mt-0.5 font-medium">
              {isRunning ? 'Идет замер' : elapsedMs > 0 ? 'Пауза' : 'Готов'}
            </span>
          </div>
        </div>
      </div>

      {/* Laps List */}
      <div className="flex-1 overflow-y-auto px-4 py-2 space-y-1.5 scrollbar-thin scrollbar-thumb-neutral-800">
        {laps.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center text-neutral-600 text-xs py-8">
            <Flag className="w-8 h-8 text-neutral-800 mb-1.5" />
            <span>Нажмите "Круг", чтобы фиксировать промежуточные отметки</span>
          </div>
        ) : (
          laps.map(lap => {
            const lapTimeFormatted = formatMs(lap.lapTimeMs);
            const overallFormatted = formatMs(lap.overallTimeMs);
            const isFastest = lap.lapIndex === fastestLapIndex;
            const isSlowest = lap.lapIndex === slowestLapIndex;

            return (
              <div
                key={lap.lapIndex}
                className={`px-3 py-2 rounded-xl text-xs flex items-center justify-between border ${
                  isFastest
                    ? 'bg-emerald-950/30 border-emerald-500/30 text-emerald-300'
                    : isSlowest
                    ? 'bg-amber-950/30 border-amber-500/30 text-amber-300'
                    : 'bg-neutral-900/60 border-neutral-800/80 text-neutral-300'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-neutral-400">#{lap.lapIndex}</span>
                  {isFastest && <span className="text-[10px] font-bold text-emerald-400">Лучший</span>}
                  {isSlowest && <span className="text-[10px] font-bold text-amber-400">Худший</span>}
                </div>

                <div className="flex items-center gap-3 font-mono">
                  <span className="font-semibold">
                    +{lapTimeFormatted.minutes}:{lapTimeFormatted.seconds}.{lapTimeFormatted.hundredths}
                  </span>
                  <span className="text-neutral-500 text-[11px]">
                    {overallFormatted.minutes}:{overallFormatted.seconds}.{overallFormatted.hundredths}
                  </span>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Bottom Action Controls */}
      <div className="px-6 py-3.5 border-t border-neutral-900 bg-neutral-950 flex items-center justify-between shrink-0">
        <button
          onClick={handleReset}
          disabled={elapsedMs === 0}
          className="p-3 rounded-full bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-white transition disabled:opacity-40"
          title="Сброс"
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
            className="w-14 h-14 rounded-full bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center transition shadow-lg shadow-blue-600/30 active:scale-95"
            title="Старт"
          >
            <Play className="w-6 h-6 ml-0.5" />
          </button>
        )}

        <button
          onClick={handleLap}
          disabled={!isRunning}
          className="p-3 rounded-full bg-neutral-900 hover:bg-neutral-800 text-neutral-300 hover:text-white transition disabled:opacity-40"
          title="Круг"
        >
          <Flag className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
