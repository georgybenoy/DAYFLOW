import React from 'react';
import { Sun, Bell, CheckSquare, Flame, BarChart3, LucideIcon } from 'lucide-react';
import { useDayFlowStore, ScreenType } from '../store';

export const BottomNav: React.FC = () => {
  const { activeScreen, setActiveScreen, tasks, habits, reminders, habitLogs, selectedDate } = useDayFlowStore();

  // Calculate badges
  const pendingTasks = tasks.filter(t => t.status === 'PENDING').length;
  
  const todayDateObj = new Date(selectedDate);
  const jsDay = todayDateObj.getDay();
  const dayNum = jsDay === 0 ? 7 : jsDay;
  const scheduledHabits = habits.filter(h => !h.archived && h.scheduledDays.includes(dayNum));
  const doneHabitIds = new Set(habitLogs.filter(l => l.date === selectedDate && l.isCompleted).map(l => l.habitId));
  const pendingHabits = scheduledHabits.filter(h => !doneHabitIds.has(h.id)).length;

  const activeReminders = reminders.filter(r => !r.isPaused).length;

  const navItems: { id: ScreenType; label: string; icon: LucideIcon; badge?: number }[] = [
    { id: 'today', label: 'Today', icon: Sun },
    { id: 'reminders', label: 'Reminders', icon: Bell, badge: activeReminders > 0 ? activeReminders : undefined },
    { id: 'tasks', label: 'Tasks', icon: CheckSquare, badge: pendingTasks > 0 ? pendingTasks : undefined },
    { id: 'habits', label: 'Habits', icon: Flame, badge: pendingHabits > 0 ? pendingHabits : undefined },
    { id: 'insights', label: 'Insights', icon: BarChart3 },
  ];

  return (
    <nav 
      className="fixed bottom-0 left-0 right-0 z-40 bg-dark-base/90 backdrop-blur-lg border-t border-white/5 pt-2 px-4"
      style={{ paddingBottom: 'max(env(safe-area-inset-bottom), 0.5rem)' }}
    >
      <div className="max-w-md mx-auto flex items-center justify-around">
        {navItems.map(item => {
          const isActive = activeScreen === item.id;
          const Icon = item.icon;

          return (
            <button
              key={item.id}
              onClick={() => setActiveScreen(item.id)}
              className={`relative flex flex-col items-center justify-center py-1 px-3 rounded-2xl transition-all duration-200 ${
                isActive
                  ? 'text-accent-mint'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {/* Active Pill Glow */}
              {isActive && (
                <div className="absolute inset-0 bg-accent-mint/10 rounded-2xl border border-accent-mint/20 -z-10" />
              )}

              <div className="relative">
                <Icon size={20} />
                {item.badge !== undefined && (
                  <span className="absolute -top-1 -right-2 min-w-4 h-4 px-1 rounded-full bg-accent-mint text-dark-base text-[9px] font-bold font-mono flex items-center justify-center">
                    {item.badge}
                  </span>
                )}
              </div>
              <span className={`text-[10px] mt-1 font-medium tracking-tight ${isActive ? 'font-bold' : ''}`}>
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
