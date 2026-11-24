import React from 'react';

interface RingData {
  progress: number; // 0 to 100
  color: string;
  name: string;
}

interface MultiProgressRingProps {
  rings: RingData[]; // from outer to inner
  size?: number;
  centerText?: string;
  centerSubtext?: string;
}

export const MultiProgressRing: React.FC<MultiProgressRingProps> = ({
  rings,
  size = 140,
  centerText,
  centerSubtext,
}) => {
  const strokeWidth = 8;
  const gap = 4;

  return (
    <div className="relative inline-flex items-center justify-center flex-shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="transform -rotate-90">
        {rings.map((ring, idx) => {
          const radius = (size / 2) - (strokeWidth / 2) - idx * (strokeWidth + gap);
          if (radius <= 0) return null;
          const circumference = 2 * Math.PI * radius;
          const clamped = Math.min(100, Math.max(0, ring.progress));
          const strokeDashoffset = circumference - (clamped / 100) * circumference;

          return (
            <g key={ring.name}>
              {/* Background Track */}
              <circle
                cx={size / 2}
                cy={size / 2}
                r={radius}
                stroke="rgba(255, 255, 255, 0.07)"
                strokeWidth={strokeWidth}
                fill="none"
              />
              {/* Active Progress */}
              <circle
                cx={size / 2}
                cy={size / 2}
                r={radius}
                stroke={ring.color}
                strokeWidth={strokeWidth}
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                fill="none"
                className="transition-all duration-500 ease-out"
              />
            </g>
          );
        })}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center select-none pointer-events-none">
        {centerText && (
          <span className="text-xl font-bold tracking-tight text-white font-mono">
            {centerText}
          </span>
        )}
        {centerSubtext && (
          <span className="text-[10px] uppercase tracking-wider text-slate-400 font-medium">
            {centerSubtext}
          </span>
        )}
      </div>
    </div>
  );
};
