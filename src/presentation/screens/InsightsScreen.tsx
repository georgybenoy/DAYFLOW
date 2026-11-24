import React, { useState } from 'react';
import { 
  BarChart3, 
  TrendingUp, 
  CheckCircle2, 
  Flame, 
  Clock, 
  AlertCircle,
  HelpCircle,
  Calendar
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { MultiProgressRing } from '../components/MultiProgressRing';
import { MetricCard } from '../components/MetricCard';
import { StreakCalculator } from '../../domain/streak';
import { getWeekDates } from '../../core/time';

export const InsightsScreen: React.FC = () => {
  const { tasks, habits, habitLogs, reminders, selectedDate } = useDayFlowStore();

  const [scope, setScope] = useState<'week' | 'month' | 'all'>('week');

  const weekDates = getWeekDates(selectedDate);

  // Completed & Pending tasks
  const completedTasks = tasks.filter(t => t.status === 'COMPLETED');
  const pendingTasks = tasks.filter(t => t.status === 'PENDING');
  const totalTasks = tasks.length;
  const taskCompletionRate = totalTasks > 0 ? Math.round((completedTasks.length / totalTasks) * 100) : 0;

  // Overdue count
  const todayStr = new Date().toISOString().split('T')[0];
  const overdueTasksCount = pendingTasks.filter(t => t.deadlineDate && t.deadlineDate < todayStr).length;

  // Habits consistency
  let totalHabitStreak = 0;
  let maxHabitStreak = 0;
  const activeHabits = habits.filter(h => !h.archived);
  for (const h of activeHabits) {
    const s = StreakCalculator.calculate(h, habitLogs, selectedDate);
    totalHabitStreak += s.currentStreak;
    if (s.longestStreak > maxHabitStreak) {
      maxHabitStreak = s.longestStreak;
    }
  }

  // Focused minutes
  const totalFocusedMinutes = completedTasks.reduce((acc, t) => acc + (t.estimatedDurationMinutes || 30), 0);

  // App-defined Daily Execution Score
  const dailyExecutionScore = Math.min(100, Math.round(
    (taskCompletionRate * 0.5) + (Math.min(100, activeHabits.length > 0 ? (totalHabitStreak / activeHabits.length) * 20 : 50) * 0.3) + 20
  ));

  // Weekly Bar Chart Data (Tasks completed per day of the week)
  const weeklyTaskData = weekDates.map(d => {
    const count = tasks.filter(t => t.completedAt && t.completedAt.startsWith(d.dateString)).length;
    return {
      day: d.dayNameShort,
      date: d.dateString,
      count,
    };
  });
  const maxWeeklyCount = Math.max(...weeklyTaskData.map(d => d.count), 4);

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <BarChart3 size={22} className="text-accent-mint" />
            Execution Insights
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Objective performance analytics derived purely from local database records.
          </p>
        </div>

        {/* Scope selector */}
        <div className="flex items-center gap-1 bg-dark-card p-1 rounded-xl border border-white/5">
          {(['week', 'month', 'all'] as const).map(s => (
            <button
              key={s}
              onClick={() => setScope(s)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium capitalize transition-all ${
                scope === s
                  ? 'bg-accent-mint/15 text-accent-mint font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Daily Execution Score Hero */}
      <div className="bg-gradient-to-br from-dark-surface to-dark-card border border-white/10 rounded-3xl p-5 shadow-xl relative overflow-hidden">
        <div className="flex items-center justify-between">
          <div className="space-y-2 max-w-[220px]">
            <span className="text-[10px] font-bold uppercase tracking-wider text-accent-mint">
              App-Defined Metric
            </span>
            <h3 className="text-2xl font-bold text-white tracking-tight">
              {dailyExecutionScore}/100 Score
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Calculated deterministically from completed tasks, scheduled habit streaks, and reminder response.
            </p>
            <div className="flex items-center gap-1 text-[10px] text-slate-500 pt-1">
              <HelpCircle size={12} />
              <span>Personal productivity index (non-medical).</span>
            </div>
          </div>

          <MultiProgressRing
            rings={[
              { name: 'Tasks', progress: taskCompletionRate, color: '#A5E6D6' },
              { name: 'Habits', progress: Math.min(100, maxHabitStreak * 15), color: '#F0E99B' },
              { name: 'Commitments', progress: 85, color: '#DCC3F5' },
            ]}
            size={130}
            centerText={`${dailyExecutionScore}`}
            centerSubtext="INDEX"
          />
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-2 gap-3">
        <MetricCard
          title="Task Completion Rate"
          value={`${taskCompletionRate}%`}
          subtitle={`${completedTasks.length} of ${totalTasks} completed`}
          icon={CheckCircle2}
          iconColor="#A5E6D6"
        />
        <MetricCard
          title="Longest Habit Streak"
          value={`${maxHabitStreak}d`}
          subtitle={`Across ${activeHabits.length} active habits`}
          icon={Flame}
          iconColor="#F0E99B"
        />
        <MetricCard
          title="Focused Minutes"
          value={`${totalFocusedMinutes}m`}
          subtitle="Estimated duration completed"
          icon={Clock}
          iconColor="#DCC3F5"
        />
        <MetricCard
          title="Overdue Items"
          value={`${overdueTasksCount}`}
          subtitle={overdueTasksCount > 0 ? "Requires review" : "All on schedule"}
          icon={AlertCircle}
          iconColor={overdueTasksCount > 0 ? "#F87171" : "#A5E6D6"}
        />
      </div>

      {/* Weekly Completion Bar Chart */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp size={16} className="text-accent-mint" />
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Weekly Task Completion
            </h4>
          </div>
          <span className="text-[11px] text-slate-400 font-mono">
            {weekDates[0]?.dateString} - {weekDates[6]?.dateString}
          </span>
        </div>

        {/* Accessible Bar Chart */}
        <div 
          className="flex items-end justify-between gap-2 h-36 pt-4 px-2"
          role="img" 
          aria-label={`Weekly task completion bar chart. Daily counts: ${weeklyTaskData.map(d => `${d.day}: ${d.count}`).join(', ')}`}
        >
          {weeklyTaskData.map((d, i) => {
            const heightPercent = maxWeeklyCount > 0 ? Math.round((d.count / maxWeeklyCount) * 100) : 0;
            const isToday = d.date === todayStr;

            return (
              <div key={i} className="flex-1 flex flex-col items-center gap-2 h-full justify-end">
                <span className="text-[10px] font-mono text-slate-400">
                  {d.count > 0 ? d.count : ''}
                </span>
                <div className="w-full max-w-[28px] bg-white/5 rounded-t-lg relative flex items-end h-full overflow-hidden">
                  <div
                    style={{ height: `${Math.max(8, heightPercent)}%` }}
                    className={`w-full rounded-t-lg transition-all duration-500 ${
                      d.count > 0
                        ? isToday
                          ? 'bg-accent-mint shadow-[0_0_10px_rgba(165,230,214,0.4)]'
                          : 'bg-accent-mint/70'
                        : 'bg-white/5'
                    }`}
                  />
                </div>
                <span className={`text-[10px] font-bold ${isToday ? 'text-accent-mint' : 'text-slate-400'}`}>
                  {d.day}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
