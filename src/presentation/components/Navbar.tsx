import React from 'react';
import { Calendar as CalendarIcon, Settings as SettingsIcon, Compass, Sparkles } from 'lucide-react';
import { useDayFlowStore } from '../store';
import { formatDateDisplay } from '../../core/time';

export const Navbar: React.FC = () => {
  const { selectedDate, activeScreen, setActiveScreen } = useDayFlowStore();

  const formattedDate = formatDateDisplay(selectedDate);

  return (
    <header 
      className="sticky top-0 z-30 bg-dark-base/80 backdrop-blur-md border-b border-white/5 px-4 pb-3"
      style={{ paddingTop: 'max(env(safe-area-inset-top), 0.75rem)' }}
    >
      <div className="max-w-4xl mx-auto flex items-center justify-between">
        {/* Brand */}
        <div 
          onClick={() => setActiveScreen('today')}
          className="flex items-center gap-2.5 cursor-pointer group"
        >
          <div className="w-8 h-8 rounded-xl bg-accent-mint/10 border border-accent-mint/20 flex items-center justify-center text-accent-mint group-hover:scale-105 transition-transform">
            <Sparkles size={16} />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-base font-bold tracking-tight text-white font-mono">DAYFLOW</span>
              <span className="w-1.5 h-1.5 rounded-full bg-accent-mint inline-block" />
            </div>
            <p className="text-[10px] text-slate-400 font-medium tracking-wide">
              {formattedDate}
            </p>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setActiveScreen('plan')}
            className={`px-3 py-1.5 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
              activeScreen === 'plan'
                ? 'bg-accent-mint text-dark-base font-semibold shadow-sm'
                : 'text-slate-300 hover:text-white hover:bg-white/5'
            }`}
            title="Daily Planner"
          >
            <Compass size={14} />
            <span className="hidden sm:inline">Planner</span>
          </button>

          <button
            onClick={() => setActiveScreen('calendar')}
            className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all ${
              activeScreen === 'calendar'
                ? 'bg-accent-mint text-dark-base font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/5'
            }`}
            title="Calendar View"
          >
            <CalendarIcon size={16} />
          </button>

          <button
            onClick={() => setActiveScreen('settings')}
            className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all ${
              activeScreen === 'settings'
                ? 'bg-accent-mint text-dark-base font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/5'
            }`}
            title="Settings"
          >
            <SettingsIcon size={16} />
          </button>
        </div>
      </div>
    </header>
  );
};
