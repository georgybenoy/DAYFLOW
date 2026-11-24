import React, { useState } from 'react';
import { 
  Bell, 
  Plus, 
  Clock, 
  Calendar, 
  Repeat, 
  Pause, 
  Play, 
  Trash2, 
  CheckCircle2, 
  AlertCircle,
  ShieldCheck,
  ChevronRight
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { Reminder, ReminderType } from '../../domain/models';
import { RecurrenceCalculator } from '../../domain/recurrence';
import { Modal } from '../components/Modal';

export const RemindersScreen: React.FC = () => {
  const { 
    reminders, 
    categories, 
    createReminder, 
    toggleReminderPause, 
    deleteReminder,
    updateReminder 
  } = useDayFlowStore();

  const [activeTab, setActiveTab] = useState<'all' | 'active' | 'paused'>('all');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedReminderForPreview, setSelectedReminderForPreview] = useState<Reminder | null>(null);

  // Form State
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [type, setType] = useState<ReminderType>('DAILY');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 'cat_personal');
  const [targetDate, setTargetDate] = useState('');
  const [targetTime, setTargetTime] = useState('09:00');
  const [intervalMinutes, setIntervalMinutes] = useState(60);
  const [scheduledDays, setScheduledDays] = useState<number[]>([1, 2, 3, 4, 5]);
  const [isBlocking, setIsBlocking] = useState(false);
  const [formError, setFormError] = useState('');

  const filteredReminders = reminders.filter(r => {
    if (activeTab === 'active') return !r.isPaused;
    if (activeTab === 'paused') return r.isPaused;
    return true;
  });

  const toggleDay = (day: number) => {
    if (scheduledDays.includes(day)) {
      setScheduledDays(scheduledDays.filter(d => d !== day));
    } else {
      setScheduledDays([...scheduledDays, day].sort());
    }
  };

  const handleCreateReminder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setFormError('Title is required');
      return;
    }

    try {
      await createReminder({
        title: title.trim(),
        notes: notes.trim() || undefined,
        type,
        categoryId,
        targetDate: type === 'ONE_TIME' ? targetDate : null,
        targetTime: (type === 'DAILY' || type === 'WEEKLY' || type === 'CUSTOM_WEEKDAYS' || type === 'ONE_TIME') ? targetTime : null,
        scheduledDays: (type === 'WEEKLY' || type === 'CUSTOM_WEEKDAYS') ? scheduledDays : undefined,
        intervalMinutes: (type === 'FIXED_INTERVAL' || type === 'COMPLETION_BASED') ? intervalMinutes : null,
        isBlocking,
        isPaused: false,
      });

      // Reset
      setTitle('');
      setNotes('');
      setType('DAILY');
      setIsCreateModalOpen(false);
      setFormError('');
    } catch (err: any) {
      setFormError(err.message || 'Failed to create reminder');
    }
  };

  // Live preview candidate
  const previewCandidate: Reminder = {
    id: 'preview',
    title: title || 'New Reminder',
    type,
    categoryId,
    targetDate: targetDate || null,
    targetTime: targetTime || null,
    scheduledDays,
    intervalMinutes,
    isBlocking,
    isPaused: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const calculatedNextOccurrences = RecurrenceCalculator.getNextOccurrences(
    selectedReminderForPreview || previewCandidate,
    5
  );

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Bell size={22} className="text-accent-lavender" />
            Reminders
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Reliable scheduled notifications & recurrence engine.
          </p>
        </div>
        <button
          onClick={() => {
            setSelectedReminderForPreview(null);
            setIsCreateModalOpen(true);
          }}
          className="px-3.5 py-2 rounded-xl bg-accent-mint text-dark-base font-semibold text-xs flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all"
        >
          <Plus size={16} />
          <span>New Reminder</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 bg-dark-card p-1 rounded-xl border border-white/5 w-fit">
        {(['all', 'active', 'paused'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-all ${
              activeTab === tab
                ? 'bg-accent-mint/15 text-accent-mint font-semibold'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Reminders List */}
      {filteredReminders.length > 0 ? (
        <div className="space-y-3">
          {filteredReminders.map(reminder => {
            const nextOccurrence = RecurrenceCalculator.getNextOccurrence(reminder);
            const category = categories.find(c => c.id === reminder.categoryId);

            return (
              <div
                key={reminder.id}
                className={`bg-dark-card border rounded-2xl p-4 transition-all ${
                  reminder.isPaused 
                    ? 'border-white/5 opacity-60' 
                    : 'border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-semibold text-white">
                        {reminder.title}
                      </span>
                      {category && (
                        <span 
                          className="text-[10px] px-2 py-0.5 rounded-full font-medium"
                          style={{ backgroundColor: `${category.color}20`, color: category.color }}
                        >
                          {category.name}
                        </span>
                      )}
                      {reminder.isBlocking && (
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-bold">
                          BLOCKING
                        </span>
                      )}
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-400 font-mono">
                        {reminder.type.replace('_', ' ')}
                      </span>
                    </div>

                    {reminder.notes && (
                      <p className="text-xs text-slate-400">{reminder.notes}</p>
                    )}

                    {/* Occurrence Information */}
                    <div className="flex items-center gap-4 text-xs text-slate-300 pt-1">
                      {nextOccurrence ? (
                        <span className="flex items-center gap-1 text-accent-mint font-mono font-medium">
                          <Clock size={13} />
                          Next: {nextOccurrence.date} at {nextOccurrence.time}
                        </span>
                      ) : (
                        <span className="text-slate-500 text-xs">
                          {reminder.isPaused ? 'Paused' : 'No upcoming trigger'}
                        </span>
                      )}
                      <span className="flex items-center gap-1 text-slate-400 text-[11px]">
                        <ShieldCheck size={13} className="text-accent-mint" />
                        Exact alarm
                      </span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button
                      onClick={() => {
                        setSelectedReminderForPreview(reminder);
                        setIsCreateModalOpen(true);
                      }}
                      className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 text-xs font-mono"
                      title="Next 5 occurrences"
                    >
                      5 Occurrences
                    </button>
                    <button
                      onClick={() => toggleReminderPause(reminder.id)}
                      className={`p-2 rounded-lg transition-colors ${
                        reminder.isPaused
                          ? 'text-accent-mint hover:bg-accent-mint/10'
                          : 'text-amber-400 hover:bg-amber-400/10'
                      }`}
                      title={reminder.isPaused ? 'Resume Reminder' : 'Pause Reminder'}
                    >
                      {reminder.isPaused ? <Play size={16} /> : <Pause size={16} />}
                    </button>
                    <button
                      onClick={() => deleteReminder(reminder.id)}
                      className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Delete Reminder"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-8 text-center space-y-2">
          <Bell size={28} className="mx-auto text-slate-500" />
          <h4 className="text-sm font-semibold text-white">No reminders found</h4>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Create scheduled reminders to receive one-time, daily, weekly, or fixed-clock interval alarms.
          </p>
        </div>
      )}

      {/* Create / Preview Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => {
          setIsCreateModalOpen(false);
          setSelectedReminderForPreview(null);
        }}
        title={selectedReminderForPreview ? `Occurrences: ${selectedReminderForPreview.title}` : 'Create New Reminder'}
      >
        {selectedReminderForPreview ? (
          <div className="space-y-4">
            <p className="text-xs text-slate-400">
              Deterministic upcoming triggers calculated by DayFlow recurrence engine:
            </p>
            <div className="space-y-2">
              {calculatedNextOccurrences.length > 0 ? (
                calculatedNextOccurrences.map((occ, idx) => (
                  <div
                    key={idx}
                    className="p-3 bg-dark-card border border-white/5 rounded-xl flex items-center justify-between text-xs"
                  >
                    <span className="font-mono text-slate-400">#{idx + 1}</span>
                    <span className="text-white font-medium">{occ.date}</span>
                    <span className="text-accent-mint font-mono font-bold">{occ.time}</span>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-500">No upcoming occurrences calculated.</p>
              )}
            </div>
            <button
              onClick={() => setSelectedReminderForPreview(null)}
              className="w-full py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-medium text-xs transition-colors"
            >
              Close
            </button>
          </div>
        ) : (
          <form onSubmit={handleCreateReminder} className="space-y-4">
            {formError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
                {formError}
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Title *</label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="e.g. Daily Standup Sync"
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Notes (Optional)</label>
              <textarea
                value={notes}
                onChange={e => setNotes(e.target.value)}
                rows={2}
                placeholder="Additional instructions..."
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Recurrence Type</label>
                <select
                  value={type}
                  onChange={e => setType(e.target.value as ReminderType)}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                >
                  <option value="DAILY">Daily</option>
                  <option value="ONE_TIME">One-Time</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="CUSTOM_WEEKDAYS">Custom Weekdays</option>
                  <option value="FIXED_INTERVAL">Fixed Interval</option>
                  <option value="COMPLETION_BASED">Completion-Based</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Category</label>
                <select
                  value={categoryId}
                  onChange={e => setCategoryId(e.target.value)}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                >
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Target Date for One-time */}
            {type === 'ONE_TIME' && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Target Date</label>
                <input
                  type="date"
                  value={targetDate}
                  onChange={e => setTargetDate(e.target.value)}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                  required
                />
              </div>
            )}

            {/* Target Time */}
            {(type === 'DAILY' || type === 'WEEKLY' || type === 'CUSTOM_WEEKDAYS' || type === 'ONE_TIME') && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Target Time</label>
                <input
                  type="time"
                  value={targetTime}
                  onChange={e => setTargetTime(e.target.value)}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                  required
                />
              </div>
            )}

            {/* Weekdays picker */}
            {(type === 'WEEKLY' || type === 'CUSTOM_WEEKDAYS') && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Scheduled Days</label>
                <div className="flex items-center gap-1">
                  {[
                    { id: 1, label: 'M' },
                    { id: 2, label: 'T' },
                    { id: 3, label: 'W' },
                    { id: 4, label: 'T' },
                    { id: 5, label: 'F' },
                    { id: 6, label: 'S' },
                    { id: 7, label: 'S' },
                  ].map(d => {
                    const active = scheduledDays.includes(d.id);
                    return (
                      <button
                        type="button"
                        key={d.id}
                        onClick={() => toggleDay(d.id)}
                        className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition-all ${
                          active
                            ? 'bg-accent-mint text-dark-base shadow-sm'
                            : 'bg-white/5 text-slate-400 hover:text-white'
                        }`}
                      >
                        {d.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Interval in minutes */}
            {(type === 'FIXED_INTERVAL' || type === 'COMPLETION_BASED') && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Interval (minutes)</label>
                <input
                  type="number"
                  min="5"
                  step="5"
                  value={intervalMinutes}
                  onChange={e => setIntervalMinutes(Number(e.target.value))}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                  required
                />
              </div>
            )}

            {/* Blocking toggle */}
            <label className="flex items-center gap-2 cursor-pointer pt-1">
              <input
                type="checkbox"
                checked={isBlocking}
                onChange={e => setIsBlocking(e.target.checked)}
                className="w-4 h-4 rounded text-accent-mint bg-dark-card border-white/20 focus:ring-accent-mint"
              />
              <span className="text-xs text-slate-300 font-medium">
                Blocking Commitment (Blocks calendar & planner time slots)
              </span>
            </label>

            {/* Submit */}
            <div className="pt-2 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsCreateModalOpen(false)}
                className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-accent-mint text-dark-base font-bold text-xs shadow-md hover:scale-105 active:scale-95 transition-all"
              >
                Create Reminder
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};
