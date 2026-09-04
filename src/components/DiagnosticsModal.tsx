import React, { useState } from 'react';
import { X, AlertTriangle, CheckCircle2, FileCode, Shield, Layers, HelpCircle } from 'lucide-react';

interface DiagnosticsModalProps {
  isOpen: boolean;
  onClose: () => void;
  simulateMediaBug: boolean;
  onToggleMediaBug: () => void;
}

export const DiagnosticsModal: React.FC<DiagnosticsModalProps> = ({
  isOpen,
  onClose,
  simulateMediaBug,
  onToggleMediaBug,
}) => {
  if (!isOpen) return null;

  const [activeTab, setActiveTab] = useState<'media_bug' | 'lockscreen' | 'conflict_res'>('media_bug');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-xs p-4">
      <div 
        id="diagnostics-modal"
        className="bg-neutral-900 border border-neutral-800 rounded-2xl w-full max-w-3xl max-h-[85vh] flex flex-col overflow-hidden shadow-2xl text-neutral-100 animate-in fade-in zoom-in-95 duration-150"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-950/60 shrink-0">
          <div className="flex items-center gap-2.5">
            <AlertTriangle className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="font-semibold text-base">WakeSync Architecture & Diagnostic Lab</h2>
              <p className="text-xs text-neutral-400">Analysis & Solutions from AI_HANDOFF.md Sections 6 & 7</p>
            </div>
          </div>
          <button
            id="close-diagnostics-btn"
            onClick={onClose}
            className="p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 px-6 pt-3 border-b border-neutral-800 bg-neutral-950/30 shrink-0 text-xs">
          <button
            onClick={() => setActiveTab('media_bug')}
            className={`pb-2.5 px-1 font-medium transition border-b-2 ${
              activeTab === 'media_bug'
                ? 'border-amber-400 text-amber-300'
                : 'border-transparent text-neutral-400 hover:text-neutral-200'
            }`}
          >
            1. Media Controller Bug Analysis
          </button>
          <button
            onClick={() => setActiveTab('lockscreen')}
            className={`pb-2.5 px-1 font-medium transition border-b-2 ${
              activeTab === 'lockscreen'
                ? 'border-blue-400 text-blue-300'
                : 'border-transparent text-neutral-400 hover:text-neutral-200'
            }`}
          >
            2. Lock Screen Full-Screen Intent
          </button>
          <button
            onClick={() => setActiveTab('conflict_res')}
            className={`pb-2.5 px-1 font-medium transition border-b-2 ${
              activeTab === 'conflict_res'
                ? 'border-emerald-400 text-emerald-300'
                : 'border-transparent text-neutral-400 hover:text-neutral-200'
            }`}
          >
            3. Conflict Resolution & Sync Spec
          </button>
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5 text-xs text-neutral-300 leading-relaxed scrollbar-thin scrollbar-thumb-neutral-800">
          {activeTab === 'media_bug' && (
            <div className="space-y-4">
              <div className="p-3.5 bg-amber-500/10 border border-amber-500/25 rounded-xl">
                <h3 className="font-semibold text-amber-300 text-sm mb-1 flex items-center gap-1.5">
                  <AlertTriangle className="w-4 h-4" /> Root Cause of Wear OS Media Controller Hijack
                </h3>
                <p className="text-neutral-300">
                  On Wear OS (Galaxy Watch & Pixel Watch), when an alarm triggers, Wear OS monitors audio streams and notification metadata. If the audio is played using <code>USAGE_MEDIA</code> or if the foreground service notification inadvertently inherits <code>MediaStyle</code> or activates a <code>MediaSession</code>, Wear OS launches its system Media Controller widget directly over the ringing alarm!
                </p>
              </div>

              {/* Code comparison */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div className="p-3 bg-red-950/30 border border-red-900/50 rounded-xl">
                  <span className="text-red-400 font-semibold flex items-center gap-1 mb-2">
                    ❌ Problematic Implementation
                  </span>
                  <pre className="text-[11px] font-mono text-red-200 bg-neutral-950 p-2.5 rounded-lg overflow-x-auto">
{`// 1. Audio stream wrong usage:
val audioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA) // ❌ Triggers Wear OS media session!
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

// 2. Notification category missing:
val notification = NotificationCompat.Builder(ctx, channelId)
    .setContentTitle("Alarm")
    // Missing CATEGORY_ALARM or has MediaStyle
    .build()`}
                  </pre>
                </div>

                <div className="p-3 bg-emerald-950/30 border border-emerald-900/50 rounded-xl">
                  <span className="text-emerald-400 font-semibold flex items-center gap-1 mb-2">
                    ✅ Fixed WakeSync Implementation
                  </span>
                  <pre className="text-[11px] font-mono text-emerald-200 bg-neutral-950 p-2.5 rounded-lg overflow-x-auto">
{`// 1. Explicit ALARM audio usage:
val audioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM) // ✅ Wear OS treats strictly as alarm
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

// 2. Notification must set CATEGORY_ALARM:
val notification = NotificationCompat.Builder(ctx, channelId)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setFullScreenIntent(fullScreenPendingIntent, true)
    .build()`}
                  </pre>
                </div>
              </div>

              {/* Interactive toggle in modal */}
              <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-xl flex items-center justify-between">
                <div>
                  <div className="font-semibold text-neutral-200">Simulator Reproduction Mode:</div>
                  <div className="text-[11px] text-neutral-400">
                    {simulateMediaBug 
                      ? 'Currently simulating the buggy Media Controller popup on watch trigger.'
                      : 'Currently running clean fixed Wear OS alarm UI.'}
                  </div>
                </div>
                <button
                  id="diagnostics-toggle-bug-btn"
                  onClick={onToggleMediaBug}
                  className={`px-4 py-2 rounded-lg font-semibold text-xs transition ${
                    simulateMediaBug
                      ? 'bg-amber-600 hover:bg-amber-500 text-white'
                      : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                  }`}
                >
                  {simulateMediaBug ? 'Disable Bug (Switch to Fix)' : 'Enable Bug Simulation'}
                </button>
              </div>
            </div>
          )}

          {activeTab === 'lockscreen' && (
            <div className="space-y-4">
              <div className="p-3.5 bg-blue-500/10 border border-blue-500/25 rounded-xl">
                <h3 className="font-semibold text-blue-300 text-sm mb-1 flex items-center gap-1.5">
                  <Shield className="w-4 h-4" /> Android 13+ Lock Screen & Full-Screen Intent Requirements
                </h3>
                <p>
                  To ensure WakeSync alarms pop up cleanly even when the watch or phone is locked:
                </p>
                <ul className="list-disc pl-5 mt-2 space-y-1 text-neutral-300">
                  <li><code>USE_FULL_SCREEN_INTENT</code> permission must be declared in AndroidManifest.xml.</li>
                  <li>Target Activity must set <code>showWhenLocked="true"</code> and <code>turnScreenOn="true"</code> in manifest, or call <code>setShowWhenLocked(true)</code> and <code>setTurnScreenOn(true)</code> in <code>onCreate()</code>.</li>
                  <li>KeyguardManager dismissal: <code>requestDismissKeyguard()</code> enables seamless Snooze/Dismiss actions without requiring user passcode unlock first.</li>
                </ul>
              </div>

              <div className="bg-neutral-950 p-3 rounded-xl border border-neutral-800 font-mono text-[11px]">
                <div className="text-neutral-400 mb-1 font-sans font-semibold">WakeSyncAlarmFiringActivity.kt implementation:</div>
                <pre className="text-blue-300 overflow-x-auto">
{`override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguard = getSystemService(KeyguardManager::class.java)
        keyguard?.requestDismissKeyguard(this, null)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }
}`}
                </pre>
              </div>
            </div>
          )}

          {activeTab === 'conflict_res' && (
            <div className="space-y-4">
              <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/25 rounded-xl">
                <h3 className="font-semibold text-emerald-300 text-sm mb-1 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4" /> Deterministic Conflict Resolution Algorithm
                </h3>
                <p>
                  WakeSync implements a 3-tier deterministic conflict resolution protocol for bidirectional mutations between Phone and Watch:
                </p>
                <div className="mt-3 space-y-2 font-mono text-[11px]">
                  <div className="p-2 bg-neutral-950 rounded border border-neutral-800">
                    <span className="text-emerald-400 font-bold">Tier 1 — Revision Counter:</span>
                    <p className="font-sans text-neutral-300 mt-0.5">
                      Every mutation (time, label, toggle, delete) increments <code>revision = revision + 1</code>. Highest revision always wins.
                    </p>
                  </div>
                  <div className="p-2 bg-neutral-950 rounded border border-neutral-800">
                    <span className="text-blue-400 font-bold">Tier 2 — Epoch Timestamp:</span>
                    <p className="font-sans text-neutral-300 mt-0.5">
                      If revisions are identical, the latest <code>updatedAt</code> timestamp wins.
                    </p>
                  </div>
                  <div className="p-2 bg-neutral-950 rounded border border-neutral-800">
                    <span className="text-purple-400 font-bold">Tier 3 — Origin Device ID:</span>
                    <p className="font-sans text-neutral-300 mt-0.5">
                      Deterministic tie-breaker using lexical ordering of <code>originDeviceId</code>.
                    </p>
                  </div>
                </div>
              </div>

              <div className="p-3 bg-neutral-950 border border-neutral-800 rounded-xl">
                <span className="font-semibold text-neutral-200">Reconciliation & Tombstones:</span>
                <p className="text-neutral-400 text-xs mt-1">
                  Deleted alarms are maintained with a tombstone entry so that an offline peer reconnecting cannot accidentally resurrect a deleted alarm via an old revision snapshot.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-800 bg-neutral-950/80 flex items-center justify-between shrink-0">
          <span className="text-[11px] text-neutral-500">WakeSync Specification v1.0 • Google Wearable Data Layer</span>
          <button
            id="close-diagnostics-footer-btn"
            onClick={onClose}
            className="px-4 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold rounded-lg transition"
          >
            Close Lab
          </button>
        </div>
      </div>
    </div>
  );
};
