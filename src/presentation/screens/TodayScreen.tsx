import React from 'react';
import { 
  Sparkles, 
  ArrowRight, 
  CheckCircle2, 
  Clock, 
  Flame, 
  Bell, 
  Plus, 
  Moon, 
  CalendarDays,
  AlertCircle
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { MultiProgressRing } from '../components/MultiProgressRing';
import { getWeekDates, formatDateDisplay } from '../../core/time';
import { BriefingGenerator } from '../../domain/briefing';
import { StreakCalculator } from '../../domain/streak';

export const TodayScreen: React.FC = () => {
  const { 
    selectedDate, 
    setSelectedDate, 
    setActiveScreen, 
    tasks, 
    habits, 
    habitLogs, 
    reminders, 
    dailyPlan, 
    toggleTask,
    logHabit,
    togglePlanItemCompleted
  } = useDayFlowStore();

  const weekDates = getWeekDates(selectedDate);

  // Day calculations
  const todayDateObj = new Date(selectedDate);
  const jsDay = todayDateObj.getDay();
  const dayNum = jsDay === 0 ? 7 : jsDay;

  // Tasks for today
  const pendingTasks = tasks.filter(t => t.status === 'PENDING');
  const todayDeadlines = tasks.filter(t => t.deadlineDate === selectedDate && t.status === 'PENDING');
  const completedTodayTasks = tasks.filter(t => t.completedAt && t.completedAt.startsWith(selectedDate));

  // Habits for today
  const scheduledHabits = habits.filter(h => !h.archived && h.scheduledDays.includes(dayNum));
  const doneHabitIds = new Set(
    habitLogs.filter(l => l.date === selectedDate && l.isCompleted).map(l => l.habitId)
  );
  const completedHabitsCount = scheduledHabits.filter(h => doneHabitIds.has(h.id)).length;

  // Reminders for today
  const activeReminders = reminders.filter(r => !r.isPaused);

  // Progress metrics
  const totalTasksCount = pendingTasks.length + completedTodayTasks.length;
  const taskProgress = totalTasksCount > 0 ? (completedTodayTasks.length / totalTasksCount) * 100 : 0;
  const habitProgress = scheduledHabits.length > 0 ? (completedHabitsCount / scheduledHabits.length) * 100 : 0;
  const reminderProgress = 100; // Reminders scheduled

  // Daily Execution Score (Weighted formula: Tasks 40%, Habits 40%, Reminders 20%)
  const dailyExecutionScore = Math.round((taskProgress * 0.4) + (habitProgress * 0.4) + (reminderProgress * 0.2));

  // Briefing
  const briefing = BriefingGenerator.generate({
    date: selectedDate,
    tasks,
    habits,
    habitLogs,
    reminders,
  });

  return (
    <div className="space-y-6 pb-24">
      {/* 7-Day Date Selector Strip */}
      <div className="flex items-center justify-between gap-1 overflow-x-auto py-2 px-1 no-scrollbar">
        {weekDates.map(d => {
          const isSelected = d.dateString === selectedDate;
          const isTodayReal = d.dateString === new Date().toISOString().split('T')[0];

          return (
            <button
              key={d.dateString}
              onClick={() => setSelectedDate(d.dateString)}
              className={`flex-1 min-w-[44px] py-2.5 px-1 rounded-2xl flex flex-col items-center justify-center transition-all ${
                isSelected
                  ? 'bg-accent-mint text-dark-base shadow-[0_2px_12px_rgba(165,230,214,0.3)] font-bold scale-105'
                  : 'bg-dark-card/60 text-slate-400 hover:text-white hover:bg-dark-card border border-white/5'
              }`}
            >
              <span className={`text-[10px] uppercase font-semibold ${isSelected ? 'text-dark-base' : 'text-slate-400'}`}>
                {d.dayNameShort}
              </span>
              <span className="text-sm font-bold font-mono mt-0.5">
                {d.dayNumber}
              </span>
              {isTodayReal && !isSelected && (
                <span className="w-1 h-1 rounded-full bg-accent-mint mt-1" />
              )}
            </button>
          );
        })}
      </div>

      {/* Daily Execution Score Hero Card */}
      <div className="bg-gradient-to-br from-dark-surface to-dark-card border border-white/10 rounded-3xl p-5 shadow-xl relative overflow-hidden">
        <div className="flex items-center justify-between relative z-10">
          <div className="space-y-1.5 max-w-[200px]">
            <span className="text-[11px] font-bold uppercase tracking-wider text-accent-mint">
              Execution Score
            </span>
            <h2 className="text-2xl font-bold tracking-tight text-white">
              {dailyExecutionScore >= 80 ? 'Optimal Flow' : dailyExecutionScore >= 50 ? 'Steady Progress' : 'Start Today'}
            </h2>
            <p className="text-xs text-slate-400 leading-relaxed">
              {briefing.summaryText}
            </p>
            <div className="pt-2 flex items-center gap-3 text-[10px] text-slate-300">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-accent-mint" />
                Tasks ({Math.round(taskProgress)}%)
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-accent-yellow" />
                Habits ({Math.round(habitProgress)}%)
              </span>
            </div>
          </div>

          <MultiProgressRing
            rings={[
              { name: 'Tasks', progress: taskProgress, color: '#A5E6D6' },
              { name: 'Habits', progress: habitProgress, color: '#F0E99B' },
              { name: 'Reminders', progress: reminderProgress, color: '#DCC3F5' },
            ]}
            size={130}
            centerText={`${dailyExecutionScore}`}
            centerSubtext="SCORE"
          />
        </div>
      </div>

      {/* Morning Briefing Banner */}
      <div
        onClick={() => setActiveScreen('briefing')}
        className="bg-dark-card border border-accent-mint/20 hover:border-accent-mint/40 rounded-2xl p-4 cursor-pointer transition-all flex items-center justify-between group"
      >
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-accent-mint/10 text-accent-mint flex items-center justify-center">
            <Sparkles size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="text-sm font-semibold text-white">Daily Briefing</h4>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-accent-mint/10 text-accent-mint font-semibold">
                READY
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5 line-clamp-1">
              {briefing.greeting} • {todayDeadlines.length} due today • {scheduledHabits.length} habits
            </p>
          </div>
        </div>
        <ArrowRight size={18} className="text-slate-400 group-hover:text-accent-mint group-hover:translate-x-1 transition-all" />
      </div>

      {/* Urgent Deadlines Alert if any */}
      {todayDeadlines.length > 0 && (
        <div className="bg-rose-950/30 border border-rose-500/30 rounded-2xl p-4 flex items-start gap-3">
          <AlertCircle size={20} className="text-rose-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h5 className="text-xs font-bold text-rose-300 uppercase tracking-wide">
              {todayDeadlines.length} Deadline{todayDeadlines.length > 1 ? 's' : ''} Due Today
            </h5>
            <div className="mt-2 space-y-1.5">
              {todayDeadlines.map(t => (
                <div key={t.id} className="flex items-center justify-between text-xs text-white">
                  <span className="font-medium line-clamp-1">{t.title}</span>
                  <span className="text-[11px] font-mono text-rose-300">{t.deadlineTime || 'Today'}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Today's Schedule & Planned Items */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-accent-mint" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-white">Today's Schedule</h3>
          </div>
          <button
            onClick={() => setActiveScreen('plan')}
            className="text-xs text-accent-mint hover:underline font-medium flex items-center gap-1"
          >
            <span>{dailyPlan?.status === 'APPROVED' ? 'View Approved Plan' : 'Generate Plan'}</span>
            <ArrowRight size={12} />
          </button>
        </div>

        {dailyPlan && dailyPlan.items.length > 0 ? (
          <div className="bg-dark-card border border-white/5 rounded-2xl divide-y divide-white/5 overflow-hidden">
            {dailyPlan.items.slice(0, 5).map(item => (
              <div 
                key={item.id} 
                className={`p-3.5 flex items-center justify-between gap-3 transition-colors ${
                  item.isCompleted ? 'bg-white/[0.02] opacity-60' : 'hover:bg-white/[0.03]'
                }`}
              >
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => togglePlanItemCompleted(item.id)}
                    className={`w-6 h-6 rounded-lg flex items-center justify-center border transition-all ${
                      item.isCompleted 
                        ? 'bg-accent-mint border-accent-mint text-dark-base' 
                        : 'border-white/20 hover:border-accent-mint text-transparent'
                    }`}
                  >
                    ✓
                  </button>
                  <div>
                    <p className={`text-sm font-medium ${item.isCompleted ? 'line-through text-slate-400' : 'text-white'}`}>
                      {item.title}
                    </p>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {item.startTime} - {item.endTime} ({item.durationMinutes}m) • {item.itemType}
                    </span>
                  </div>
                </div>
              </div>
            ))}
            {dailyPlan.items.length > 5 && (
              <div className="p-2.5 text-center bg-white/[0.01]">
                <button
                  onClick={() => setActiveScreen('plan')}
                  className="text-xs text-accent-mint font-medium"
                >
                  +{dailyPlan.items.length - 5} more items in full plan
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-6 text-center space-y-2">
            <p className="text-xs text-slate-400">No scheduled plan generated for {formatDateDisplay(selectedDate)} yet.</p>
            <button
              onClick={() => setActiveScreen('plan')}
              className="px-4 py-2 rounded-xl bg-accent-mint text-dark-base font-semibold text-xs inline-flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all"
            >
              <Sparkles size={14} />
              <span>Generate Daily Plan</span>
            </button>
          </div>
        )}
      </div>

      {/* Today's Habits Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Flame size={16} className="text-accent-yellow" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-white">Today's Habits</h3>
          </div>
          <button
            onClick={() => setActiveScreen('habits')}
            className="text-xs text-slate-400 hover:text-white font-medium"
          >
            Manage ({completedHabitsCount}/{scheduledHabits.length})
          </button>
        </div>

        {scheduledHabits.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {scheduledHabits.map(habit => {
              const isDone = doneHabitIds.has(habit.id);
              const streak = StreakCalculator.calculate(habit, habitLogs, selectedDate);

              return (
                <div
                  key={habit.id}
                  className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between gap-3 ${
                    isDone 
                      ? 'bg-accent-mint/5 border-accent-mint/30' 
                      : 'bg-dark-card border-white/5 hover:border-white/10'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => {
                        logHabit(
                          habit.id,
                          selectedDate,
                          isDone ? 0 : habit.targetValue,
                          !isDone
                        );
                      }}
                      className={`w-7 h-7 rounded-xl flex items-center justify-center border font-bold text-xs transition-all ${
                        isDone
                          ? 'bg-accent-mint border-accent-mint text-dark-base shadow-[0_0_10px_rgba(165,230,214,0.3)]'
                          : 'border-white/20 text-transparent hover:border-accent-mint'
                      }`}
                    >
                      ✓
                    </button>
                    <div>
                      <p className={`text-xs font-semibold ${isDone ? 'line-through text-slate-400' : 'text-white'}`}>
                        {habit.title}
                      </p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] text-accent-yellow font-bold font-mono">
                          🔥 {streak.currentStreak}d streak
                        </span>
                        {habit.unit && (
                          <span className="text-[10px] text-slate-400">
                            {habit.targetValue} {habit.unit}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="bg-dark-card border border-white/5 rounded-2xl p-4 text-center">
            <p className="text-xs text-slate-400">No habits scheduled for this day.</p>
          </div>
        )}
      </div>

      {/* Evening Review Banner */}
      <div
        onClick={() => setActiveScreen('review')}
        className="bg-gradient-to-r from-purple-950/40 to-dark-card border border-purple-500/20 hover:border-purple-500/40 rounded-2xl p-4 cursor-pointer transition-all flex items-center justify-between group"
      >
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-purple-500/10 text-accent-lavender flex items-center justify-center">
            <Moon size={20} />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Evening Review</h4>
            <p className="text-xs text-slate-400 mt-0.5">
              Review completed work, carry forward unfinished tasks, and plan tomorrow.
            </p>
          </div>
        </div>
        <ArrowRight size={18} className="text-slate-400 group-hover:text-accent-lavender group-hover:translate-x-1 transition-all" />
      </div>
    </div>
  );
};
