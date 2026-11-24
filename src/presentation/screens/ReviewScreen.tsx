import React, { useState } from 'react';
import { 
  Moon, 
  CheckCircle2, 
  ArrowRight, 
  Clock, 
  Calendar, 
  RotateCcw, 
  Trash2, 
  Save, 
  Sparkles,
  AlertCircle
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { ReviewTaskDecision, DailyReview } from '../../domain/models';
import { formatDateDisplay } from '../../core/time';

export const ReviewScreen: React.FC = () => {
  const { 
    selectedDate, 
    tasks, 
    habits, 
    habitLogs, 
    saveDailyReview, 
    generateDailyPlan, 
    setSelectedDate, 
    setActiveScreen 
  } = useDayFlowStore();

  const [notes, setNotes] = useState('');
  const [decisions, setDecisions] = useState<Record<string, { action: ReviewTaskDecision['action']; newDate?: string }>>({});

  // Tasks eligible for review today
  const completedTodayTasks = tasks.filter(t => t.completedAt && t.completedAt.startsWith(selectedDate));
  const unfinishedTasks = tasks.filter(t => t.status === 'PENDING');

  // Tomorrow calculation
  const tomorrowObj = new Date(selectedDate);
  tomorrowObj.setDate(tomorrowObj.getDate() + 1);
  const tomorrowStr = tomorrowObj.toISOString().split('T')[0];

  const handleActionChange = (taskId: string, action: ReviewTaskDecision['action'], newDate?: string) => {
    setDecisions(prev => ({
      ...prev,
      [taskId]: { action, newDate }
    }));
  };

  const handleFinishReview = async () => {
    // Map decisions into array (defaulting to CARRY_FORWARD for any unselected task)
    const decisionsArray: ReviewTaskDecision[] = unfinishedTasks.map(t => {
      const dec = decisions[t.id];
      if (dec) {
        return { taskId: t.id, action: dec.action, newDate: dec.newDate };
      }
      return { taskId: t.id, action: 'CARRY_FORWARD' };
    });

    const review: DailyReview = {
      id: `review_${selectedDate}`,
      date: selectedDate,
      completedTaskCount: completedTodayTasks.length,
      unfinishedTaskCount: unfinishedTasks.length,
      habitCompletionRate: 85,
      notes: notes.trim() || undefined,
      decisions: decisionsArray,
      reviewedAt: new Date().toISOString(),
    };

    await saveDailyReview(review);

    // Auto-generate tomorrow's plan based on decisions
    await generateDailyPlan(tomorrowStr);
    await setSelectedDate(tomorrowStr);
    setActiveScreen('plan');
  };

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Moon size={22} className="text-accent-lavender" />
          Evening Review: {formatDateDisplay(selectedDate)}
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Reflect on daily execution. Never silently discard unfinished tasks.
        </p>
      </div>

      {/* Summary Banner */}
      <div className="bg-gradient-to-r from-purple-950/40 to-dark-card border border-purple-500/20 rounded-2xl p-4 flex items-center justify-between">
        <div>
          <span className="text-xs font-bold text-accent-lavender uppercase tracking-wide">
            Daily Execution Summary
          </span>
          <p className="text-sm text-white font-semibold mt-1">
            {completedTodayTasks.length} task(s) completed • {unfinishedTasks.length} pending
          </p>
        </div>
      </div>

      {/* Completed Tasks */}
      {completedTodayTasks.length > 0 && (
        <div className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-3">
          <div className="flex items-center gap-2 text-accent-mint text-xs font-bold uppercase tracking-wider">
            <CheckCircle2 size={16} />
            <span>Completed Today ({completedTodayTasks.length})</span>
          </div>
          <div className="space-y-2">
            {completedTodayTasks.map(task => (
              <div key={task.id} className="text-xs text-slate-300 flex items-center gap-2">
                <span className="text-accent-mint">✓</span>
                <span className="line-through">{task.title}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Unfinished Tasks Review */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} className="text-amber-400" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-white">
              Unfinished Tasks ({unfinishedTasks.length})
            </h3>
          </div>
          <span className="text-[11px] text-slate-400">
            Unassigned tasks default to Carry Forward
          </span>
        </div>

        {unfinishedTasks.length > 0 ? (
          <div className="space-y-3">
            {unfinishedTasks.map(task => {
              const currentAction = decisions[task.id]?.action || 'CARRY_FORWARD';

              return (
                <div
                  key={task.id}
                  className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-white">
                      {task.title}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/5 text-slate-400 font-mono">
                      {task.priority}
                    </span>
                  </div>

                  {/* Actions buttons */}
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {[
                      { id: 'CARRY_FORWARD', label: 'Carry Forward' },
                      { id: 'RESCHEDULE', label: 'Reschedule' },
                      { id: 'COMPLETE', label: 'Mark Done' },
                      { id: 'EXCLUDE', label: 'Exclude' },
                      { id: 'CANCEL', label: 'Cancel' },
                    ].map(btn => (
                      <button
                        key={btn.id}
                        onClick={() => handleActionChange(task.id, btn.id as any)}
                        className={`px-3 py-1 rounded-xl text-xs font-medium transition-all ${
                          currentAction === btn.id
                            ? 'bg-accent-mint text-dark-base font-bold shadow-sm'
                            : 'bg-white/5 text-slate-400 hover:text-white'
                        }`}
                      >
                        {btn.label}
                      </button>
                    ))}
                  </div>

                  {/* Date picker if Reschedule */}
                  {currentAction === 'RESCHEDULE' && (
                    <div className="pt-2">
                      <label className="block text-[11px] text-slate-400 mb-1">Reschedule to date:</label>
                      <input
                        type="date"
                        defaultValue={tomorrowStr}
                        onChange={e => handleActionChange(task.id, 'RESCHEDULE', e.target.value)}
                        className="bg-dark-surface border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent-mint"
                      />
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-6 text-center">
            <p className="text-xs text-slate-400">All tasks for today are completed! Excellent execution.</p>
          </div>
        )}
      </div>

      {/* Reflection Notes */}
      <div className="bg-dark-card border border-white/5 rounded-2xl p-4 space-y-2">
        <label className="block text-xs font-medium text-slate-300">
          Evening Reflection & Notes (Optional)
        </label>
        <textarea
          value={notes}
          onChange={e => setNotes(e.target.value)}
          rows={3}
          placeholder="What went well today? What will improve tomorrow?"
          className="w-full bg-dark-surface border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
        />
      </div>

      {/* Finish Review & Plan Tomorrow */}
      <div className="pt-2">
        <button
          onClick={handleFinishReview}
          className="w-full py-3.5 px-4 rounded-2xl bg-accent-mint text-dark-base font-bold text-sm flex items-center justify-center gap-2 shadow-lg hover:scale-[1.01] active:scale-[0.99] transition-all"
        >
          <Sparkles size={18} />
          <span>Complete Review & Generate Tomorrow's Plan</span>
          <ArrowRight size={18} />
        </button>
      </div>
    </div>
  );
};
