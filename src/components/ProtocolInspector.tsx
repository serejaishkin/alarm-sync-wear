import React, { useState } from 'react';
import { ProtocolLog } from '../types';
import { 
  Activity, 
  Wifi, 
  WifiOff, 
  RotateCw, 
  Trash2, 
  Code, 
  CheckCircle, 
  Clock, 
  ChevronDown, 
  ChevronUp,
  Radio,
  FileJson
} from 'lucide-react';

interface ProtocolInspectorProps {
  logs: ProtocolLog[];
  isConnected: boolean;
  onToggleConnection: () => void;
  onForceReconcile: () => void;
  onClearLogs: () => void;
  queuedCount: number;
}

export const ProtocolInspector: React.FC<ProtocolInspectorProps> = ({
  logs,
  isConnected,
  onToggleConnection,
  onForceReconcile,
  onClearLogs,
  queuedCount,
}) => {
  const [selectedLog, setSelectedLog] = useState<ProtocolLog | null>(null);
  const [filterChannel, setFilterChannel] = useState<'ALL' | 'DataClient' | 'MessageClient'>('ALL');
  const [isExpanded, setIsExpanded] = useState<boolean>(true);

  const filteredLogs = logs.filter(l => {
    if (filterChannel === 'ALL') return true;
    return l.channel === filterChannel;
  });

  return (
    <div 
      id="protocol-inspector"
      className="bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden shadow-xl flex flex-col"
    >
      {/* Header */}
      <div className="p-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/80">
        <div className="flex items-center gap-2">
          <Activity className="w-5 h-5 text-blue-400" />
          <h2 className="font-semibold text-sm text-neutral-100">
            Wearable Data Layer & Sync Protocol Inspector
          </h2>
          <span className="text-[11px] px-2 py-0.5 rounded-full bg-neutral-800 text-neutral-400 font-mono">
            v1.0 (DataClient + MessageClient)
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Online/Offline Toggle */}
          <button
            id="toggle-connection-btn"
            onClick={onToggleConnection}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border flex items-center gap-1.5 transition ${
              isConnected
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20'
                : 'bg-amber-500/10 border-amber-500/30 text-amber-400 hover:bg-amber-500/20'
            }`}
            title={isConnected ? 'Disconnect peer (simulate offline mode)' : 'Reconnect peer'}
          >
            {isConnected ? <Wifi className="w-3.5 h-3.5" /> : <WifiOff className="w-3.5 h-3.5" />}
            <span>{isConnected ? 'Peer Connected' : `Offline (${queuedCount} queued)`}</span>
          </button>

          {/* Reconcile Snapshot button */}
          <button
            id="force-reconcile-btn"
            onClick={onForceReconcile}
            className="px-3 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-medium rounded-lg border border-neutral-700 flex items-center gap-1.5 transition"
            title="Force snapshot reconciliation exchange"
          >
            <RotateCw className="w-3.5 h-3.5" />
            <span>Reconcile Snapshot</span>
          </button>

          {/* Clear */}
          <button
            id="clear-logs-btn"
            onClick={onClearLogs}
            className="p-1.5 text-neutral-500 hover:text-neutral-300 hover:bg-neutral-800 rounded-lg transition"
            title="Clear protocol logs"
          >
            <Trash2 className="w-4 h-4" />
          </button>

          {/* Minimize / expand */}
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-1.5 text-neutral-400 hover:text-white rounded-lg hover:bg-neutral-800 transition"
          >
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="grid grid-cols-1 lg:grid-cols-3 divide-y lg:divide-y-0 lg:divide-x divide-neutral-800 max-h-[360px]">
          {/* Logs feed list */}
          <div className="lg:col-span-2 flex flex-col h-[320px] overflow-hidden">
            {/* Filter tabs */}
            <div className="px-3 py-2 bg-neutral-950/60 border-b border-neutral-800/80 flex items-center justify-between text-xs">
              <div className="flex items-center gap-1.5">
                <span className="text-neutral-500 font-medium">Filter:</span>
                {(['ALL', 'DataClient', 'MessageClient'] as const).map(ch => (
                  <button
                    key={ch}
                    onClick={() => setFilterChannel(ch)}
                    className={`px-2 py-0.5 rounded text-[11px] font-medium transition ${
                      filterChannel === ch
                        ? 'bg-blue-600 text-white'
                        : 'text-neutral-400 hover:text-neutral-200'
                    }`}
                  >
                    {ch}
                  </button>
                ))}
              </div>
              <span className="text-neutral-500 text-[11px]">
                {filteredLogs.length} events logged
              </span>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto p-2 space-y-1.5 scrollbar-thin scrollbar-thumb-neutral-800">
              {filteredLogs.length === 0 ? (
                <div className="h-full flex flex-col items-center justify-center text-neutral-500 text-xs py-8">
                  <Radio className="w-8 h-8 text-neutral-700 mb-1" />
                  <span>No protocol transmissions yet</span>
                  <span className="text-[10px] text-neutral-600">
                    Interact with either Phone or Watch to see Wearable sync packets
                  </span>
                </div>
              ) : (
                filteredLogs.map(log => {
                  const isSelected = selectedLog?.id === log.id;
                  const time = new Date(log.timestamp).toLocaleTimeString([], {
                    hour12: false,
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                  });

                  return (
                    <div
                      key={log.id}
                      onClick={() => setSelectedLog(log)}
                      className={`p-2 rounded-lg cursor-pointer border text-xs transition font-mono ${
                        isSelected
                          ? 'bg-blue-950/40 border-blue-600/60 text-blue-200'
                          : 'bg-neutral-950/60 border-neutral-800/80 text-neutral-300 hover:border-neutral-700'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[10px] mb-1">
                        <div className="flex items-center gap-1.5 font-sans">
                          <span className={`px-1.5 py-0.2 rounded font-semibold ${
                            log.channel === 'DataClient' 
                              ? 'bg-indigo-500/20 text-indigo-400 border border-indigo-500/30' 
                              : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                          }`}>
                            {log.channel}
                          </span>
                          <span className="text-neutral-500 font-mono">{time}</span>
                        </div>

                        <span className={`text-[9px] px-1.5 py-0.2 rounded uppercase font-semibold ${
                          log.status === 'delivered' 
                            ? 'text-emerald-400 bg-emerald-500/10' 
                            : 'text-amber-400 bg-amber-500/10'
                        }`}>
                          {log.status}
                        </span>
                      </div>

                      <div className="truncate text-neutral-200 text-[11px] font-sans">
                        {log.summary}
                      </div>

                      <div className="text-[10px] text-neutral-500 truncate mt-0.5">
                        Path: {log.path}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Selected Payload Viewer */}
          <div className="p-3 bg-neutral-950 flex flex-col h-[320px] overflow-hidden">
            <div className="flex items-center justify-between pb-2 mb-2 border-b border-neutral-800 text-xs text-neutral-400">
              <span className="flex items-center gap-1.5 font-medium">
                <FileJson className="w-3.5 h-3.5 text-blue-400" />
                Packet Payload Inspector
              </span>
              {selectedLog && (
                <span className="text-[10px] text-neutral-500 font-mono">
                  {selectedLog.operation}
                </span>
              )}
            </div>

            {selectedLog ? (
              <div className="flex-1 overflow-y-auto font-mono text-[11px] text-neutral-300 bg-neutral-900/60 p-2.5 rounded-lg border border-neutral-800/80 scrollbar-thin scrollbar-thumb-neutral-800">
                <div className="mb-2 text-[10px] text-neutral-400 font-sans flex flex-col gap-0.5 border-b border-neutral-800 pb-1.5">
                  <div><strong>Channel:</strong> {selectedLog.channel}</div>
                  <div><strong>Path:</strong> {selectedLog.path}</div>
                  <div><strong>Source:</strong> {selectedLog.source}</div>
                </div>
                <pre className="text-blue-300">
                  {JSON.stringify(selectedLog.payload, null, 2)}
                </pre>
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-neutral-600 text-xs text-center p-4">
                <Code className="w-8 h-8 text-neutral-700 mb-1.5" />
                <span>Select any packet on the left to inspect its raw JSON payload</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
