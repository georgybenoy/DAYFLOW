import React, { useState } from 'react';
import { 
  Flame, 
  Plus, 
  CheckCircle2, 
  Calendar, 
  Clock, 
  Award, 
  Trash2,
  TrendingUp,
  BarChart2
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { Habit, HabitTrackingType } from '../../domain/models';
import { StreakCalculator } from '../../domain/streak';
import { WeeklyDotsStrip } from '../components/WeeklyDotsStrip';
import { ProgressRing } from '../components/ProgressRing';
import { Modal } from '../components/Modal';
import { getMonthMatrix, getWeekDates } from '../../core/time';

export const HabitsScreen: React.FC = () => {
  const { 
    habits, 
    habitLogs, 
    categories, 
    selectedDate, 
    createHabit, 
    logHabit, 
    archiveHabit 
  } = useDayFlowStore();

  const [selectedHabitForHeatmap, setSelectedHabitForHeatmap] = useState<Habit | null>(null);
  const [loggingHabit, setLoggingHabit] = useState<Habit | null>(null);
  const [logValueInput, setLogValueInput] = useState<number>(0);
  const [logNotesInput, setLogNotesInput] = useState<string>('');

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 'cat_fitness');
  const [trackingType, setTrackingType] = useState<HabitTrackingType>('BINARY');
  const [targetValue, setTargetValue] = useState(1);
  const [unit, setUnit] = useState('');
  const [scheduledDays, setScheduledDays] = useState<number[]>([1, 2, 3, 4, 5, 6, 7]);
  const [reminderEnabled, setReminderEnabled] = useState(false);
  const [reminderTime, setReminderTime] = useState('08:00');
  const [formError, setFormError] = useState('');

  const weekDates = getWeekDates(selectedDate);
  const activeHabits = habits.filter(h => !h.archived);

  // Today calculations
  const todayDateObj = new Date(selectedDate);
  const jsDay = todayDateObj.getDay();
  const todayDayNum = jsDay === 0 ? 7 : jsDay;

  const toggleDay = (day: number) => {
    if (scheduledDays.includes(day)) {
      setScheduledDays(scheduledDays.filter(d => d !== day));
    } else {
      setScheduledDays([...scheduledDays, day].sort());
    }
  };

  const handleOpenLogModal = (habit: Habit) => {
    const existingLog = habitLogs.find(l => l.habitId === habit.id && l.date === selectedDate);
    setLoggingHabit(habit);
    setLogValueInput(existingLog ? existingLog.value : habit.targetValue);
    setLogNotesInput(existingLog?.notes || '');
  };

  const handleSaveLog = async () => {
    if (!loggingHabit) return;
    const isCompleted = logValueInput >= loggingHabit.targetValue;
    await logHabit(
      loggingHabit.id,
      selectedDate,
      logValueInput,
      isCompleted,
      logNotesInput || undefined
    );
    setLoggingHabit(null);
  };

  const handleCreateHabit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setFormError('Title is required');
      return;
    }

    try {
      await createHabit({
        title: title.trim(),
        description: description.trim() || undefined,
        categoryId,
        trackingType,
        targetValue: trackingType === 'BINARY' ? 1 : Number(targetValue),
        unit: trackingType === 'BINARY' ? undefined : unit.trim(),
        scheduledDays,
        reminderTime: reminderEnabled ? reminderTime : null,
        reminderEnabled,
      });

      setTitle('');
      setDescription('');
      setUnit('');
      setIsCreateModalOpen(false);
      setFormError('');
    } catch (err: any) {
      setFormError(err.message || 'Failed to create habit');
    }
  };

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Flame size={22} className="text-accent-yellow" />
            Habit Engine
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Consistency tracking with scheduled-day streak preservation.
          </p>
        </div>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="px-3.5 py-2 rounded-xl bg-accent-mint text-dark-base font-semibold text-xs flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all"
        >
          <Plus size={16} />
          <span>New Habit</span>
        </button>
      </div>

      {/* Habit List */}
      {activeHabits.length > 0 ? (
        <div className="space-y-3">
          {activeHabits.map(habit => {
            const category = categories.find(c => c.id === habit.categoryId);
            const logsForHabit = habitLogs.filter(l => l.habitId === habit.id);
            const streak = StreakCalculator.calculate(habit, logsForHabit, selectedDate);
            const todayLog = logsForHabit.find(l => l.date === selectedDate);
            const isCompletedToday = todayLog?.isCompleted || false;
            const isScheduledToday = habit.scheduledDays.includes(todayDayNum);

            // Compute 7 days status for weekly strip
            const completedDays = weekDates.map(d => {
              const log = logsForHabit.find(l => l.date === d.dateString);
              return log?.isCompleted || false;
            });

            return (
              <div
                key={habit.id}
                className="bg-dark-card border border-white/5 hover:border-white/10 rounded-2xl p-4 transition-all"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3 flex-1">
                    {/* Log check / trigger */}
                    {habit.trackingType === 'BINARY' ? (
                      <button
                        onClick={() => {
                          logHabit(
                            habit.id,
                            selectedDate,
                            isCompletedToday ? 0 : 1,
                            !isCompletedToday
                          );
                        }}
                        className={`w-8 h-8 rounded-xl flex items-center justify-center border font-bold text-sm transition-all mt-0.5 ${
                          isCompletedToday
                            ? 'bg-accent-mint border-accent-mint text-dark-base shadow-[0_0_12px_rgba(165,230,214,0.3)]'
                            : 'border-white/20 text-transparent hover:border-accent-mint'
                        }`}
                        title={isCompletedToday ? 'Mark Incomplete' : 'Mark Completed'}
                      >
                        ✓
                      </button>
                    ) : (
                      <button
                        onClick={() => handleOpenLogModal(habit)}
                        className={`px-2 py-1.5 rounded-xl border flex flex-col items-center justify-center text-xs font-mono transition-all ${
                          isCompletedToday
                            ? 'bg-accent-mint/20 border-accent-mint text-accent-mint'
                            : 'border-white/20 text-slate-300 hover:border-accent-mint'
                        }`}
                        title="Quick Log"
                      >
                        <span className="text-[10px] uppercase font-sans">LOG</span>
                        <span className="font-bold">{todayLog ? todayLog.value : 0}/{habit.targetValue}</span>
                      </button>
                    )}

                    <div className="space-y-1.5 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-semibold text-white">
                          {habit.title}
                        </span>
                        {category && (
                          <span
                            className="text-[10px] px-2 py-0.5 rounded-full font-medium"
                            style={{ backgroundColor: `${category.color}20`, color: category.color }}
                          >
                            {category.name}
                          </span>
                        )}
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-400 font-mono">
                          {habit.trackingType}
                        </span>
                        {!isScheduledToday && (
                          <span className="text-[10px] text-slate-500 italic">
                            (Rest day)
                          </span>
                        )}
                      </div>

                      {habit.description && (
                        <p className="text-xs text-slate-400">{habit.description}</p>
                      )}

                      {/* Streaks & Target */}
                      <div className="flex items-center gap-3 text-xs text-slate-300 flex-wrap pt-0.5">
                        <span className="flex items-center gap-1 text-accent-yellow font-bold font-mono">
                          🔥 {streak.currentStreak}d streak
                        </span>
                        <span className="text-slate-400 font-mono text-[11px]">
                          Best: {streak.longestStreak}d
                        </span>
                        {habit.unit && (
                          <span className="text-slate-400 text-[11px]">
                            Target: {habit.targetValue} {habit.unit}
                          </span>
                        )}
                        <button
                          onClick={() => setSelectedHabitForHeatmap(habit)}
                          className="flex items-center gap-1 text-accent-mint hover:underline text-[11px] font-medium"
                        >
                          <BarChart2 size={12} />
                          <span>Heatmap</span>
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Archive button */}
                  <button
                    onClick={() => archiveHabit(habit.id)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                    title="Archive habit"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>

                {/* Weekly Strip */}
                <div className="mt-3 pt-3 border-t border-white/5 flex items-center justify-between">
                  <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                    This Week
                  </span>
                  <WeeklyDotsStrip completedDays={completedDays} size="sm" />
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-8 text-center space-y-2">
          <Flame size={28} className="mx-auto text-slate-500" />
          <h4 className="text-sm font-semibold text-white">No habits tracked yet</h4>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Build healthy daily routines. Track binary checkboxes, numeric quantities, durations, or repetitions.
          </p>
        </div>
      )}

      {/* Monthly Heatmap Modal */}
      <Modal
        isOpen={!!selectedHabitForHeatmap}
        onClose={() => setSelectedHabitForHeatmap(null)}
        title={`Heatmap: ${selectedHabitForHeatmap?.title}`}
      >
        {selectedHabitForHeatmap && (
          <div className="space-y-4">
            <p className="text-xs text-slate-400">
              Monthly consistency overview. Scheduled days completed are highlighted in mint.
            </p>
            {/* 7x5 Month Grid Matrix */}
            <div className="grid grid-cols-7 gap-1.5 text-center">
              {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
                <div key={i} className="text-[10px] font-bold text-slate-500 py-1">
                  {d}
                </div>
              ))}
              {getMonthMatrix(new Date().getFullYear(), new Date().getMonth()).map((dayItem, idx) => {
                const dateStr = dayItem.dateString;
                const isLogged = habitLogs.some(
                  l => l.habitId === selectedHabitForHeatmap.id && l.date === dateStr && l.isCompleted
                );
                return (
                  <div
                    key={dateStr || idx}
                    className={`h-8 rounded-lg flex items-center justify-center text-xs font-mono font-medium border transition-colors ${
                      isLogged
                        ? 'bg-accent-mint text-dark-base border-accent-mint font-bold shadow-[0_0_8px_rgba(165,230,214,0.3)]'
                        : dayItem.inMonth
                        ? 'bg-dark-card border-white/5 text-slate-400'
                        : 'bg-white/[0.02] border-transparent text-slate-600 opacity-40'
                    }`}
                  >
                    {dayItem.dayNumber}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </Modal>

      {/* Quick Log Modal (for Quantity / Duration / Repetition) */}
      <Modal
        isOpen={!!loggingHabit}
        onClose={() => setLoggingHabit(null)}
        title={`Quick Log: ${loggingHabit?.title}`}
      >
        {loggingHabit && (
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">
                Value ({loggingHabit.unit || 'units'}) • Target: {loggingHabit.targetValue}
              </label>
              <input
                type="number"
                min="0"
                value={logValueInput}
                onChange={e => setLogValueInput(Number(e.target.value))}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-base font-mono text-white focus:outline-none focus:border-accent-mint"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Notes (Optional)</label>
              <input
                type="text"
                value={logNotesInput}
                onChange={e => setLogNotesInput(e.target.value)}
                placeholder="e.g. Read chapter 3"
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              />
            </div>

            <div className="pt-2 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setLoggingHabit(null)}
                className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveLog}
                className="px-5 py-2 rounded-xl bg-accent-mint text-dark-base font-bold text-xs shadow-md"
              >
                Save Log
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Create Habit Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create New Habit"
      >
        <form onSubmit={handleCreateHabit} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Habit Title *</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="e.g. Daily Technical Reading"
              className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Description</label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={2}
              placeholder="Why this habit matters..."
              className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Tracking Type</label>
              <select
                value={trackingType}
                onChange={e => setTrackingType(e.target.value as HabitTrackingType)}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              >
                <option value="BINARY">Binary (Yes/No)</option>
                <option value="QUANTITY">Quantity (e.g. 2000 ml)</option>
                <option value="DURATION">Duration (e.g. 30 mins)</option>
                <option value="REPETITION">Repetition (e.g. 10 reps)</option>
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

          {trackingType !== 'BINARY' && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Target Value</label>
                <input
                  type="number"
                  min="1"
                  value={targetValue}
                  onChange={e => setTargetValue(Number(e.target.value))}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Unit</label>
                <input
                  type="text"
                  value={unit}
                  onChange={e => setUnit(e.target.value)}
                  placeholder="e.g. pages, mins, ml"
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                />
              </div>
            </div>
          )}

          {/* Weekday selector */}
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
                        ? 'bg-accent-yellow text-dark-base shadow-sm'
                        : 'bg-white/5 text-slate-400 hover:text-white'
                    }`}
                  >
                    {d.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Reminder Toggle */}
          <div className="space-y-2 pt-1 border-t border-white/5">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={reminderEnabled}
                onChange={e => setReminderEnabled(e.target.checked)}
                className="w-4 h-4 rounded text-accent-mint bg-dark-card border-white/20 focus:ring-accent-mint"
              />
              <span className="text-xs text-slate-300 font-medium">Enable Daily Reminder</span>
            </label>

            {reminderEnabled && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Reminder Time</label>
                <input
                  type="time"
                  value={reminderTime}
                  onChange={e => setReminderTime(e.target.value)}
                  className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
                />
              </div>
            )}
          </div>

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
              Create Habit
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
