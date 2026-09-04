import React from 'react';
import { 
  Bell, 
  Watch, 
  Smartphone, 
  RefreshCw, 
  ShieldCheck, 
  Layers, 
  Sparkles,
  HelpCircle
} from 'lucide-react';

interface HeaderProps {
  onResetData: () => void;
  onOpenDiagnostics: () => void;
  isConnected: boolean;
  totalAlarms: number;
}

export const Header: React.FC<HeaderProps> = ({
  onResetData,
  onOpenDiagnostics,
  isConnected,
  totalAlarms,
}) => {
  return (
    <header className="w-full border-b border-neutral-800/80 bg-neutral-950/70 backdrop-blur-md sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3.5 flex flex-col sm:flex-row items-center justify-between gap-3">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
                WakeSync
                <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/25">
                  Phone ↔ Wear OS Sync
                </span>
              </h1>
            </div>
            <p className="text-xs text-neutral-400">
              Bidirectional alarm replication with deterministic revision conflict resolution
            </p>
          </div>
        </div>

        {/* Quick controls */}
        <div className="flex items-center gap-2">
          {/* Diagnostic Lab */}
          <button
            id="open-diagnostics-btn"
            onClick={onOpenDiagnostics}
            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1.5 transition shadow-xs"
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Architecture & Diagnostic Lab</span>
          </button>

          {/* Reset Demo Data */}
          <button
            id="reset-demo-data-btn"
            onClick={onResetData}
            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-neutral-900 hover:bg-neutral-800 text-neutral-300 border border-neutral-800 flex items-center gap-1.5 transition"
            title="Reset to default synchronized alarms"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reset Demo</span>
          </button>
        </div>
      </div>
    </header>
  );
};
