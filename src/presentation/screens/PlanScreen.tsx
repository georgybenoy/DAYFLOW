import React, { useState } from 'react';
import { 
  Compass, 
  Sparkles, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  Calendar, 
  ArrowRight, 
  RotateCcw,
  ShieldCheck,
  Check
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { formatDateDisplay } from '../../core/time';

export const PlanScreen: React.FC = () => {
  const { 
    selectedDate, 
    setSelectedDate, 
    dailyPlan, 
    generateDailyPlan, 
    approveDailyPlan, 
    togglePlanItemCompleted 
  } = useDayFlowStore();

  const [isGenerating, setIsGenerating] = useState(false);

  const handleGenerate = async () => {
    setIsGenerating(true);
    try {
      await generateDailyPlan(selectedDate);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleApprove = async () => {
    if (dailyPlan) {
      await approveDailyPlan(selectedDate);
    }
  };

  const isApproved = dailyPlan?.status === 'APPROVED';

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Compass size={22} className="text-accent-mint" />
            Daily Planner
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Deterministic slot assignment • {formatDateDisplay(selectedDate)}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {dailyPlan && !isApproved && (
            <button
              onClick={handleApprove}
              className="px-3.5 py-2 rounded-xl bg-accent-mint text-dark-base font-bold text-xs flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all"
            >
              <Check size={16} />
              <span>Approve Plan</span>
            </button>
          )}

          <button
            onClick={handleGenerate}
            disabled={isGenerating}
            className={`px-3 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${
              isApproved 
                ? 'bg-white/10 text-white hover:bg-white/15'
                : 'bg-accent-mint/15 text-accent-mint hover:bg-accent-mint/25 border border-accent-mint/30'
            }`}
          >
            <RotateCcw size={14} className={isGenerating ? 'animate-spin' : ''} />
            <span>{isApproved ? 'Regenerate' : 'Generate Draft'}</span>
          </button>
        </div>
      </div>

      {/* Status Badge */}
      {dailyPlan && (
        <div className="flex items-center justify-between bg-dark-card border border-white/5 rounded-2xl p-4">
          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-bold font-mono px-2.5 py-1 rounded-full uppercase ${
              isApproved
                ? 'bg-accent-mint/20 text-accent-mint border border-accent-mint/30'
                : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
            }`}>
              {dailyPlan.status} PLAN
            </span>
            <span className="text-xs text-slate-400">
              {dailyPlan.items.length} items scheduled
            </span>
          </div>

          {dailyPlan.approvedAt && (
            <span className="text-[11px] font-mono text-slate-400">
              Approved at {new Date(dailyPlan.approvedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>
      )}

      {/* Warnings Banner if any */}
      {dailyPlan && dailyPlan.warnings.length > 0 && (
        <div className="bg-amber-950/30 border border-amber-500/30 rounded-2xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-amber-300 text-xs font-bold uppercase tracking-wider">
            <AlertTriangle size={15} />
            <span>Capacity Notice</span>
          </div>
          {dailyPlan.warnings.map((w, idx) => (
            <p key={idx} className="text-xs text-amber-200/90 leading-relaxed">
              {w}
            </p>
          ))}
        </div>
      )}

      {/* Planned Schedule Timeline */}
      {dailyPlan && dailyPlan.items.length > 0 ? (
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-white">
            Scheduled Timeline
          </h3>

          <div className="space-y-2.5">
            {dailyPlan.items.map((item, idx) => (
              <div
                key={item.id}
                className={`bg-dark-card border rounded-2xl p-3.5 flex items-center justify-between gap-3 transition-all ${
                  item.isCompleted 
                    ? 'border-white/5 opacity-60' 
                    : 'border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => togglePlanItemCompleted(item.id)}
                    className={`w-6 h-6 rounded-lg flex items-center justify-center border transition-all ${
                      item.isCompleted
                        ? 'bg-accent-mint border-accent-mint text-dark-base font-bold'
                        : 'border-white/20 hover:border-accent-mint text-transparent'
                    }`}
                  >
                    ✓
                  </button>

                  <div>
                    <p className={`text-sm font-semibold ${item.isCompleted ? 'line-through text-slate-400' : 'text-white'}`}>
                      {item.title}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-[11px] font-mono text-accent-mint font-medium">
                        {item.startTime} - {item.endTime}
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        ({item.durationMinutes} mins)
                      </span>
                      <span className="text-[10px] px-2 py-0.2 rounded bg-white/5 text-slate-400 font-mono">
                        {item.itemType}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-8 text-center space-y-3">
          <Compass size={32} className="mx-auto text-slate-500" />
          <h4 className="text-sm font-semibold text-white">No plan generated yet</h4>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            The deterministic planner evaluates priority scores, overdue deadlines, and availability windows to generate an optimal daily schedule.
          </p>
          <button
            onClick={handleGenerate}
            disabled={isGenerating}
            className="px-4 py-2.5 rounded-xl bg-accent-mint text-dark-base font-bold text-xs inline-flex items-center gap-2 shadow-md hover:scale-105 active:scale-95 transition-all"
          >
            <Sparkles size={16} />
            <span>Generate Today's Plan</span>
          </button>
        </div>
      )}

      {/* Unscheduled Tasks Section */}
      {dailyPlan && dailyPlan.unscheduledTasks.length > 0 && (
        <div className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold uppercase tracking-wider text-rose-300">
              Unscheduled Tasks ({dailyPlan.unscheduledTasks.length})
            </h4>
            <span className="text-[10px] text-slate-400">
              Preserved safely — never dropped
            </span>
          </div>

          <div className="space-y-2">
            {dailyPlan.unscheduledTasks.map(t => (
              <div key={t.id} className="text-xs text-slate-300 flex items-center justify-between bg-dark-surface p-2.5 rounded-xl border border-white/5">
                <span className="font-medium line-clamp-1">{t.title}</span>
                <span className="text-[10px] px-2 py-0.5 rounded bg-white/5 text-slate-400 font-mono">
                  {t.priority} • {t.estimatedDurationMinutes}m
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
