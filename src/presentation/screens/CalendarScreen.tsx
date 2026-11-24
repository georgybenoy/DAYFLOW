import React, { useState } from 'react';
import { 
  Calendar as CalendarIcon, 
  ChevronLeft, 
  ChevronRight, 
  Clock, 
  CheckCircle2, 
  Flame, 
  Bell, 
  Filter,
  Compass
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { getMonthMatrix, getWeekDates, formatDateDisplay } from '../../core/time';

export const CalendarScreen: React.FC = () => {
  const { 
    selectedDate, 
    setSelectedDate, 
    tasks, 
    habits, 
    habitLogs, 
    reminders, 
    dailyPlan,
    toggleTask,
    setActiveScreen
  } = useDayFlowStore();

  const [viewMode, setViewMode] = useState<'day' | 'week' | 'month'>('month');
  const [filterType, setFilterType] = useState<'ALL' | 'TASK' | 'DEADLINE' | 'HABIT' | 'REMINDER'>('ALL');

  // Month state
  const currentDate = new Date(selectedDate);
  const [viewYear, setViewYear] = useState(currentDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(currentDate.getMonth());

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June', 
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewMonth(11);
      setViewYear(viewYear - 1);
    } else {
      setViewMonth(viewMonth - 1);
    }
  };

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0);
      setViewYear(viewYear + 1);
    } else {
      setViewMonth(viewMonth + 1);
    }
  };

  const handleJumpToday = () => {
    const today = new Date().toISOString().split('T')[0];
    const tDate = new Date();
    setViewYear(tDate.getFullYear());
    setViewMonth(tDate.getMonth());
    setSelectedDate(today);
  };

  // Day Agenda Items
  const dayTasks = tasks.filter(t => t.plannedDate === selectedDate || t.deadlineDate === selectedDate);
  const dayHabits = habits.filter(h => {
    const jsDay = new Date(selectedDate).getDay();
    const dNum = jsDay === 0 ? 7 : jsDay;
    return !h.archived && h.scheduledDays.includes(dNum);
  });
  const dayReminders = reminders.filter(r => !r.isPaused);

  // Month matrix
  const matrix = getMonthMatrix(viewYear, viewMonth);

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <CalendarIcon size={22} className="text-accent-mint" />
            Calendar & Agenda
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Unified chronological schedule with recurrence mapping.
          </p>
        </div>

        {/* View mode toggle */}
        <div className="flex items-center gap-1 bg-dark-card p-1 rounded-xl border border-white/5">
          {(['month', 'week', 'day'] as const).map(v => (
            <button
              key={v}
              onClick={() => setViewMode(v)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium capitalize transition-all ${
                viewMode === v
                  ? 'bg-accent-mint text-dark-base font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {v}
            </button>
          ))}
        </div>
      </div>

      {/* Month Navigation & Today Button */}
      <div className="flex items-center justify-between bg-dark-card border border-white/5 rounded-2xl p-3">
        <div className="flex items-center gap-2">
          <button
            onClick={handlePrevMonth}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/5"
          >
            <ChevronLeft size={18} />
          </button>
          <span className="text-sm font-bold text-white font-mono">
            {monthNames[viewMonth]} {viewYear}
          </span>
          <button
            onClick={handleNextMonth}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/5"
          >
            <ChevronRight size={18} />
          </button>
        </div>

        <button
          onClick={handleJumpToday}
          className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-all"
        >
          Jump to Today
        </button>
      </div>

      {/* Month Grid View */}
      {viewMode === 'month' && (
        <div className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-2">
          {/* Weekday headers */}
          <div className="grid grid-cols-7 gap-1 text-center">
            {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(day => (
              <span key={day} className="text-[10px] font-bold text-slate-400 uppercase py-1">
                {day}
              </span>
            ))}
          </div>

          {/* Matrix days */}
          <div className="grid grid-cols-7 gap-1">
            {matrix.map((dayItem, idx) => {
              const dateStr = dayItem.dateString;
              const isSelected = dateStr === selectedDate;
              const isToday = dateStr === new Date().toISOString().split('T')[0];
              const dayTasksCount = tasks.filter(t => t.deadlineDate === dateStr || t.plannedDate === dateStr).length;

              return (
                <button
                  key={dateStr || idx}
                  onClick={() => setSelectedDate(dateStr)}
                  className={`h-12 rounded-xl p-1 flex flex-col items-center justify-between transition-all border ${
                    isSelected
                      ? 'bg-accent-mint/20 border-accent-mint text-white font-bold'
                      : isToday
                      ? 'border-accent-mint/50 bg-white/5 text-slate-200'
                      : dayItem.inMonth
                      ? 'border-transparent hover:border-white/10 text-slate-300'
                      : 'border-transparent text-slate-600 opacity-40'
                  }`}
                >
                  <span className="text-[11px] font-mono">
                    {dayItem.dayNumber}
                  </span>
                  {dayTasksCount > 0 && (
                    <div className="flex items-center gap-0.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-accent-mint" />
                      {dayTasksCount > 1 && (
                        <span className="text-[8px] font-mono text-accent-mint">
                          {dayTasksCount}
                        </span>
                      )}
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Selected Day Agenda */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold uppercase tracking-wider text-white">
            Agenda for {formatDateDisplay(selectedDate)}
          </h3>
          <span className="text-xs text-slate-400 font-mono">
            {dayTasks.length} task(s) • {dayHabits.length} habit(s)
          </span>
        </div>

        {/* Agenda Cards */}
        <div className="space-y-2.5">
          {/* Plan items if approved */}
          {dailyPlan && dailyPlan.items.length > 0 && (
            <div className="bg-accent-mint/5 border border-accent-mint/20 rounded-2xl p-3.5 space-y-2">
              <div className="flex items-center justify-between text-xs text-accent-mint font-semibold">
                <span className="flex items-center gap-1.5">
                  <Compass size={14} />
                  <span>Approved Daily Plan Timeline</span>
                </span>
                <span className="font-mono">{dailyPlan.items.length} items</span>
              </div>
              <div className="space-y-1.5 pt-1">
                {dailyPlan.items.map(item => (
                  <div key={item.id} className="flex items-center justify-between text-xs text-white bg-dark-card/60 p-2 rounded-xl border border-white/5">
                    <span className="font-medium line-clamp-1">{item.title}</span>
                    <span className="text-slate-400 font-mono text-[11px] flex-shrink-0 ml-2">
                      {item.startTime} - {item.endTime}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tasks scheduled or due */}
          {dayTasks.map(task => (
            <div
              key={task.id}
              className="bg-dark-card border border-white/5 rounded-2xl p-3.5 flex items-center justify-between gap-3"
            >
              <div className="flex items-center gap-3">
                <button
                  onClick={() => toggleTask(task.id)}
                  className={`w-6 h-6 rounded-lg flex items-center justify-center border text-xs font-bold ${
                    task.status === 'COMPLETED'
                      ? 'bg-accent-mint border-accent-mint text-dark-base'
                      : 'border-white/20 text-transparent hover:border-accent-mint'
                  }`}
                >
                  ✓
                </button>
                <div>
                  <p className={`text-xs font-semibold ${task.status === 'COMPLETED' ? 'line-through text-slate-500' : 'text-white'}`}>
                    {task.title}
                  </p>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {task.deadlineDate === selectedDate ? `Deadline: ${task.deadlineTime || 'Today'}` : `Planned: ${task.plannedStartTime || 'Flexible'}`}
                  </span>
                </div>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-400 font-mono">
                {task.priority}
              </span>
            </div>
          ))}

          {dayTasks.length === 0 && (!dailyPlan || dailyPlan.items.length === 0) && (
            <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-6 text-center">
              <p className="text-xs text-slate-400">No scheduled tasks or plan items for this date.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
