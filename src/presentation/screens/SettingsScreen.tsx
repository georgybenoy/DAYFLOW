import React, { useState } from 'react';
import { 
  Settings as SettingsIcon, 
  Moon, 
  Sun, 
  Download, 
  Upload, 
  ShieldCheck, 
  Clock, 
  Calendar, 
  Volume2, 
  Vibrate, 
  FileText,
  AlertTriangle,
  Check
} from 'lucide-react';
import { useDayFlowStore } from '../store';

export const SettingsScreen: React.FC = () => {
  const { settings, updateSettings, exportBackup, importBackup } = useDayFlowStore();

  const [importStatus, setImportStatus] = useState<string>('');
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const json = await exportBackup();
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `dayflow_backup_${new Date().toISOString().split('T')[0]}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      alert('Export failed: ' + e.message);
    } finally {
      setIsExporting(false);
    }
  };

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const text = event.target?.result as string;
        const success = await importBackup(text);
        if (success) {
          setImportStatus('Backup restored successfully!');
        } else {
          setImportStatus('Failed to restore backup. Invalid format or version.');
        }
      } catch (err: any) {
        setImportStatus('Error importing: ' + err.message);
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <SettingsIcon size={22} className="text-accent-mint" />
          Settings & Local Backup
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Offline preferences, scheduling boundaries & versioned data export.
        </p>
      </div>

      {/* Preferences Section */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-white">General Preferences</h3>

        {/* Week start */}
        <div className="flex items-center justify-between">
          <div>
            <span className="text-sm font-semibold text-white">First Day of Week</span>
            <p className="text-xs text-slate-400">Used for weekly calendar strips & streak grids</p>
          </div>
          <select
            value={settings.weekStartDay}
            onChange={e => updateSettings({ weekStartDay: Number(e.target.value) as any })}
            className="bg-dark-surface border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent-mint"
          >
            <option value={1}>Monday</option>
            <option value={7}>Sunday</option>
          </select>
        </div>

        {/* Time format */}
        <div className="flex items-center justify-between border-t border-white/5 pt-3">
          <div>
            <span className="text-sm font-semibold text-white">Time Display Format</span>
            <p className="text-xs text-slate-400">24-hour vs 12-hour clock</p>
          </div>
          <select
            value={settings.timeFormat}
            onChange={e => updateSettings({ timeFormat: e.target.value as any })}
            className="bg-dark-surface border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent-mint"
          >
            <option value="24h">24-hour (14:30)</option>
            <option value="12h">12-hour (2:30 PM)</option>
          </select>
        </div>

        {/* Daily Schedule Window */}
        <div className="grid grid-cols-2 gap-3 border-t border-white/5 pt-3">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Day Start Time</label>
            <input
              type="time"
              value={settings.dayStartTime}
              onChange={e => updateSettings({ dayStartTime: e.target.value })}
              className="w-full bg-dark-surface border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Day End Time</label>
            <input
              type="time"
              value={settings.dayEndTime}
              onChange={e => updateSettings({ dayEndTime: e.target.value })}
              className="w-full bg-dark-surface border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
            />
          </div>
        </div>
      </div>

      {/* Notifications & Alarms */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-5 space-y-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-white">Alarm & Notification Reliability</h3>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Volume2 size={16} className="text-accent-mint" />
            <span className="text-sm text-white font-medium">Notification Sounds</span>
          </div>
          <input
            type="checkbox"
            checked={settings.soundEnabled}
            onChange={e => updateSettings({ soundEnabled: e.target.checked })}
            className="w-4 h-4 rounded text-accent-mint bg-dark-surface border-white/20"
          />
        </div>

        <div className="flex items-center justify-between border-t border-white/5 pt-3">
          <div className="flex items-center gap-2">
            <Vibrate size={16} className="text-accent-mint" />
            <span className="text-sm text-white font-medium">Vibration Alerts</span>
          </div>
          <input
            type="checkbox"
            checked={settings.vibrationEnabled}
            onChange={e => updateSettings({ vibrationEnabled: e.target.checked })}
            className="w-4 h-4 rounded text-accent-mint bg-dark-surface border-white/20"
          />
        </div>

        <div className="flex items-center justify-between border-t border-white/5 pt-3">
          <div>
            <div className="flex items-center gap-1.5">
              <ShieldCheck size={16} className="text-accent-mint" />
              <span className="text-sm text-white font-medium">Exact Alarm Scheduling</span>
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Uses system AlarmManager when packaged for Android. Inexact fallback disclosed when restricted.
            </p>
          </div>
          <input
            type="checkbox"
            checked={settings.exactAlarmsEnabled}
            onChange={e => updateSettings({ exactAlarmsEnabled: e.target.checked })}
            className="w-4 h-4 rounded text-accent-mint bg-dark-surface border-white/20"
          />
        </div>
      </div>

      {/* Local Backup & Restore */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-5 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold uppercase tracking-wider text-white">Local Backup & Restoration</h3>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-accent-mint/10 text-accent-mint font-semibold">
            OFFLINE ONLY
          </span>
        </div>

        <p className="text-xs text-slate-400 leading-relaxed">
          DAYFLOW has no backend, no authentication, and no cloud synchronization. All data lives entirely in your local IndexedDB storage. Export JSON backups regularly to safeguard your productivity records.
        </p>

        {importStatus && (
          <div className={`p-3 rounded-xl text-xs ${
            importStatus.includes('successfully')
              ? 'bg-accent-mint/10 text-accent-mint border border-accent-mint/20'
              : 'bg-rose-500/10 text-rose-300 border border-rose-500/20'
          }`}>
            {importStatus}
          </div>
        )}

        <div className="flex items-center gap-3 pt-2">
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="flex-1 py-2.5 px-4 rounded-xl bg-accent-mint text-dark-base font-bold text-xs flex items-center justify-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all"
          >
            <Download size={15} />
            <span>{isExporting ? 'Exporting...' : 'Export Backup JSON'}</span>
          </button>

          <label className="flex-1 py-2.5 px-4 rounded-xl bg-white/10 hover:bg-white/15 text-white font-semibold text-xs flex items-center justify-center gap-2 cursor-pointer transition-all">
            <Upload size={15} />
            <span>Restore Backup JSON</span>
            <input
              type="file"
              accept=".json"
              onChange={handleImportFile}
              className="hidden"
            />
          </label>
        </div>
      </div>
    </div>
  );
};
