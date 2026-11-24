import React from 'react';

interface WeeklyDotsStripProps {
  // array of 7 booleans for Mon through Sun
  completedDays: boolean[];
  // optionally highlight today's index (0=Mon, 6=Sun)
  todayIndex?: number;
  // compact or normal size
  size?: 'sm' | 'md';
}

export const WeeklyDotsStrip: React.FC<WeeklyDotsStripProps> = ({
  completedDays,
  todayIndex,
  size = 'md',
}) => {
  const days = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  return (
    <div className="flex items-center justify-between gap-1 w-full max-w-xs">
      {days.map((day, idx) => {
        const isDone = completedDays[idx] || false;
        const isToday = todayIndex === idx;

        return (
          <div key={idx} className="flex flex-col items-center gap-1 flex-1">
            <span className={`text-[10px] font-medium ${isToday ? 'text-accent-mint font-bold' : 'text-slate-400'}`}>
              {day}
            </span>
            <div
              className={`rounded-full transition-all duration-300 flex items-center justify-center ${
                size === 'sm' ? 'w-4 h-4 text-[8px]' : 'w-6 h-6 text-[10px]'
              } ${
                isDone
                  ? 'bg-accent-mint text-dark-base shadow-[0_0_8px_rgba(165,230,214,0.3)] font-bold'
                  : isToday
                  ? 'border-2 border-accent-mint/60 bg-dark-card text-slate-300'
                  : 'bg-white/5 border border-white/10 text-slate-600'
              }`}
            >
              {isDone ? '✓' : ''}
            </div>
          </div>
        );
      })}
    </div>
  );
};
