import React from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: LucideIcon;
  iconColor?: string;
  trend?: {
    value: string;
    isPositive: boolean;
  };
  onClick?: () => void;
  className?: string;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  iconColor = '#A5E6D6',
  trend,
  onClick,
  className = '',
}) => {
  return (
    <div
      onClick={onClick}
      className={`bg-dark-card border border-white/5 rounded-2xl p-4 flex flex-col justify-between transition-all duration-200 ${
        onClick ? 'cursor-pointer hover:border-white/10 active:scale-[0.99]' : ''
      } ${className}`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium text-slate-400 tracking-wide">{title}</span>
        {Icon && (
          <div
            className="w-8 h-8 rounded-xl flex items-center justify-center bg-white/5"
            style={{ color: iconColor }}
          >
            <Icon size={16} />
          </div>
        )}
      </div>

      <div className="flex items-baseline gap-2">
        <span className="text-2xl font-bold font-mono tracking-tight text-white">{value}</span>
        {trend && (
          <span
            className={`text-xs font-semibold ${
              trend.isPositive ? 'text-accent-mint' : 'text-rose-400'
            }`}
          >
            {trend.value}
          </span>
        )}
      </div>

      {subtitle && (
        <span className="text-[11px] text-slate-400 mt-1 line-clamp-1">{subtitle}</span>
      )}
    </div>
  );
};
