import React from 'react';
import { 
  Sparkles, 
  ArrowRight, 
  Calendar, 
  Clock, 
  Flame, 
  Bell, 
  CheckCircle2, 
  AlertCircle,
  Compass
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { BriefingGenerator } from '../../domain/briefing';
import { formatDateDisplay } from '../../core/time';

export const BriefingScreen: React.FC = () => {
  const { 
    selectedDate, 
    tasks, 
    habits, 
    habitLogs, 
    reminders, 
    setActiveScreen 
  } = useDayFlowStore();

  const briefing = BriefingGenerator.generate({
    date: selectedDate,
    tasks,
    habits,
    habitLogs,
    reminders,
  });

  return (
    <div className="space-y-6 pb-24">
      {/* Hero */}
      <div className="bg-gradient-to-br from-dark-surface to-dark-card border border-white/10 rounded-3xl p-6 space-y-3 relative overflow-hidden">
        <div className="flex items-center gap-2 text-accent-mint text-xs font-bold uppercase tracking-wider">
          <Sparkles size={16} />
          <span>Morning Briefing</span>
        </div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          {briefing.greeting}
        </h2>
        <p className="text-sm text-slate-300 font-medium">
          {formatDateDisplay(selectedDate)}
        </p>
        <p className="text-xs text-slate-400 leading-relaxed max-w-md">
          {briefing.summaryText}
        </p>

        <div className="pt-3">
          <button
            onClick={() => setActiveScreen('plan')}
            className="py-2.5 px-4 rounded-xl bg-accent-mint text-dark-base font-bold text-xs flex items-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all"
          >
            <Compass size={15} />
            <span>Open Today's Plan</span>
            <ArrowRight size={14} />
          </button>
        </div>
      </div>

      {/* Top Priority Focus */}
      <div className="space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-white flex items-center gap-1.5">
          <AlertCircle size={15} className="text-rose-400" />
          <span>Top Priority Tasks</span>
        </h3>
        {briefing.topTasks.length > 0 ? (
          <div className="space-y-2">
            {briefing.topTasks.map(task => (
              <div
                key={task.id}
                className="bg-dark-card border border-white/5 rounded-2xl p-3.5 flex items-center justify-between"
              >
                <div>
                  <h4 className="text-xs font-semibold text-white">{task.title}</h4>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {task.estimatedDurationMinutes} mins • {task.deadlineDate ? `Due ${task.deadlineDate}` : 'No deadline'}
                  </span>
                </div>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-bold">
                  {task.priority}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-4 text-center">
            <p className="text-xs text-slate-400">No urgent priority tasks pending.</p>
          </div>
        )}
      </div>

      {/* Habit Commitments */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-white flex items-center gap-1.5">
            <Flame size={15} className="text-accent-yellow" />
            <span>Today's Habit Checklist</span>
          </span>
          <span className="text-xs text-slate-400 font-mono">
            {briefing.habitsCompletedCount}/{briefing.habitsScheduledCount} Completed
          </span>
        </div>
        <p className="text-xs text-slate-400">
          Stay consistent with your scheduled habits to keep streaks active.
        </p>
      </div>

      {/* Reminders summary */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-purple-500/10 text-accent-lavender flex items-center justify-center">
            <Bell size={18} />
          </div>
          <div>
            <h4 className="text-xs font-semibold text-white">Active Reminders</h4>
            <p className="text-[11px] text-slate-400">{briefing.remindersCount} scheduled notification alarms</p>
          </div>
        </div>
        <button
          onClick={() => setActiveScreen('reminders')}
          className="text-xs text-accent-lavender hover:underline font-semibold"
        >
          View All
        </button>
      </div>
    </div>
  );
};
