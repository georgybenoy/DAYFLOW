import React, { useState } from 'react';
import { 
  CheckSquare, 
  Plus, 
  Clock, 
  Calendar, 
  AlertCircle, 
  Trash2, 
  ChevronDown, 
  ChevronUp, 
  CheckCircle2, 
  ListPlus,
  Filter
} from 'lucide-react';
import { useDayFlowStore } from '../store';
import { Priority, Task } from '../../domain/models';
import { Modal } from '../components/Modal';

export const TasksScreen: React.FC = () => {
  const { 
    tasks, 
    categories, 
    selectedDate, 
    createTask, 
    toggleTask, 
    deleteTask, 
    toggleSubtask 
  } = useDayFlowStore();

  const [activeTab, setActiveTab] = useState<'all' | 'today' | 'upcoming' | 'overdue' | 'completed'>('all');
  const [selectedPriority, setSelectedPriority] = useState<Priority | 'ALL'>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [expandedTaskIds, setExpandedTaskIds] = useState<Set<string>>(new Set());

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState(categories[0]?.id || 'cat_work');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [deadlineDate, setDeadlineDate] = useState('');
  const [deadlineTime, setDeadlineTime] = useState('');
  const [estimatedDurationMinutes, setEstimatedDurationMinutes] = useState(30);
  const [subtasksList, setSubtasksList] = useState<string[]>([]);
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [formError, setFormError] = useState('');

  const todayStr = new Date().toISOString().split('T')[0];

  const toggleExpand = (taskId: string) => {
    const next = new Set(expandedTaskIds);
    if (next.has(taskId)) {
      next.delete(taskId);
    } else {
      next.add(taskId);
    }
    setExpandedTaskIds(next);
  };

  // Filter logic
  const filteredTasks = tasks.filter(task => {
    // Tab filter
    if (activeTab === 'completed') {
      if (task.status !== 'COMPLETED') return false;
    } else {
      if (task.status === 'COMPLETED') return false; // non-completed for other tabs

      if (activeTab === 'today') {
        const isPlannedToday = task.plannedDate === selectedDate;
        const isDueToday = task.deadlineDate === selectedDate;
        if (!isPlannedToday && !isDueToday) return false;
      } else if (activeTab === 'upcoming') {
        if (!task.deadlineDate || task.deadlineDate <= selectedDate) return false;
      } else if (activeTab === 'overdue') {
        if (!task.deadlineDate || task.deadlineDate >= todayStr) return false;
      }
    }

    // Priority filter
    if (selectedPriority !== 'ALL' && task.priority !== selectedPriority) return false;

    // Category filter
    if (selectedCategory !== 'ALL' && task.categoryId !== selectedCategory) return false;

    return true;
  });

  const handleAddSubtaskInput = () => {
    if (!newSubtaskTitle.trim()) return;
    setSubtasksList([...subtasksList, newSubtaskTitle.trim()]);
    setNewSubtaskTitle('');
  };

  const handleRemoveSubtaskInput = (idx: number) => {
    setSubtasksList(subtasksList.filter((_, i) => i !== idx));
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setFormError('Task title is required');
      return;
    }

    try {
      await createTask({
        title: title.trim(),
        description: description.trim() || undefined,
        categoryId,
        priority,
        deadlineDate: deadlineDate || null,
        deadlineTime: deadlineTime || null,
        estimatedDurationMinutes: Number(estimatedDurationMinutes) || 30,
        plannedDate: selectedDate,
        status: 'PENDING',
      }, subtasksList);

      // Reset
      setTitle('');
      setDescription('');
      setSubtasksList([]);
      setIsCreateModalOpen(false);
      setFormError('');
    } catch (err: any) {
      setFormError(err.message || 'Failed to create task');
    }
  };

  const getPriorityBadge = (p: Priority) => {
    switch (p) {
      case 'URGENT':
        return <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-bold">! URGENT</span>;
      case 'HIGH':
        return <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-semibold">HIGH</span>;
      case 'MEDIUM':
        return <span className="text-[10px] px-2 py-0.5 rounded-full bg-accent-mint/20 text-accent-mint font-semibold">MEDIUM</span>;
      case 'LOW':
        return <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/10 text-slate-400 font-medium">LOW</span>;
    }
  };

  return (
    <div className="space-y-6 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <CheckSquare size={22} className="text-accent-mint" />
            Tasks & Commitments
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Offline-first task management with priority scoring & deadlines.
          </p>
        </div>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="px-3.5 py-2 rounded-xl bg-accent-mint text-dark-base font-semibold text-xs flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all"
        >
          <Plus size={16} />
          <span>New Task</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
        {[
          { id: 'all', label: 'All' },
          { id: 'today', label: 'Today' },
          { id: 'upcoming', label: 'Upcoming' },
          { id: 'overdue', label: 'Overdue' },
          { id: 'completed', label: 'Completed' },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`px-3 py-1.5 rounded-xl text-xs font-medium whitespace-nowrap transition-all ${
              activeTab === tab.id
                ? 'bg-accent-mint text-dark-base font-bold shadow-sm'
                : 'bg-dark-card text-slate-400 hover:text-white border border-white/5'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filters (Priority + Category) */}
      <div className="flex items-center gap-2 flex-wrap text-xs">
        <div className="flex items-center gap-1 bg-dark-card px-2 py-1 rounded-xl border border-white/5">
          <Filter size={12} className="text-slate-400" />
          <select
            value={selectedPriority}
            onChange={e => setSelectedPriority(e.target.value as any)}
            className="bg-transparent text-slate-300 text-xs focus:outline-none cursor-pointer"
          >
            <option value="ALL">All Priorities</option>
            <option value="URGENT">Urgent (!)</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>

        <div className="flex items-center gap-1 bg-dark-card px-2 py-1 rounded-xl border border-white/5">
          <select
            value={selectedCategory}
            onChange={e => setSelectedCategory(e.target.value)}
            className="bg-transparent text-slate-300 text-xs focus:outline-none cursor-pointer"
          >
            <option value="ALL">All Categories</option>
            {categories.map(c => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Tasks List */}
      {filteredTasks.length > 0 ? (
        <div className="space-y-3">
          {filteredTasks.map(task => {
            const isCompleted = task.status === 'COMPLETED';
            const isExpanded = expandedTaskIds.has(task.id);
            const isOverdue = task.deadlineDate && task.deadlineDate < todayStr && !isCompleted;
            const category = categories.find(c => c.id === task.categoryId);
            const subtasks = task.subtasks || [];
            const completedSubtasks = subtasks.filter(s => s.isCompleted).length;

            return (
              <div
                key={task.id}
                className={`bg-dark-card border rounded-2xl p-4 transition-all ${
                  isCompleted 
                    ? 'border-white/5 opacity-60' 
                    : isOverdue 
                    ? 'border-rose-500/30 bg-rose-950/10' 
                    : 'border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3 flex-1">
                    {/* Checkbox button */}
                    <button
                      onClick={() => toggleTask(task.id)}
                      className={`w-6 h-6 rounded-lg flex items-center justify-center border mt-0.5 flex-shrink-0 transition-all ${
                        isCompleted
                          ? 'bg-accent-mint border-accent-mint text-dark-base font-bold'
                          : 'border-white/20 hover:border-accent-mint text-transparent'
                      }`}
                    >
                      ✓
                    </button>

                    <div className="space-y-1.5 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className={`text-sm font-semibold ${isCompleted ? 'line-through text-slate-400' : 'text-white'}`}>
                          {task.title}
                        </span>
                        {getPriorityBadge(task.priority)}
                        {category && (
                          <span
                            className="text-[10px] px-2 py-0.5 rounded-full font-medium"
                            style={{ backgroundColor: `${category.color}20`, color: category.color }}
                          >
                            {category.name}
                          </span>
                        )}
                      </div>

                      {task.description && (
                        <p className="text-xs text-slate-400 leading-relaxed">
                          {task.description}
                        </p>
                      )}

                      {/* Metadata */}
                      <div className="flex items-center gap-3 text-xs text-slate-400 flex-wrap pt-1 font-mono">
                        {task.deadlineDate && (
                          <span className={`flex items-center gap-1 ${isOverdue ? 'text-rose-400 font-bold' : ''}`}>
                            <Calendar size={13} />
                            {isOverdue ? 'OVERDUE: ' : 'Due: '}{task.deadlineDate} {task.deadlineTime || ''}
                          </span>
                        )}
                        <span className="flex items-center gap-1">
                          <Clock size={13} />
                          {task.estimatedDurationMinutes}m
                        </span>
                        {subtasks.length > 0 && (
                          <button
                            onClick={() => toggleExpand(task.id)}
                            className="flex items-center gap-1 text-accent-mint hover:underline font-medium"
                          >
                            <span>Subtasks ({completedSubtasks}/{subtasks.length})</span>
                            {isExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <button
                    onClick={() => deleteTask(task.id)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors flex-shrink-0"
                    title="Delete task"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                {/* Subtasks dropdown */}
                {isExpanded && subtasks.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-white/5 space-y-2 pl-9">
                    {subtasks.map(sub => (
                      <div
                        key={sub.id}
                        onClick={() => toggleSubtask(sub.id, !sub.isCompleted)}
                        className="flex items-center gap-2 cursor-pointer group"
                      >
                        <div
                          className={`w-4 h-4 rounded flex items-center justify-center border text-[9px] font-bold ${
                            sub.isCompleted
                              ? 'bg-accent-mint border-accent-mint text-dark-base'
                              : 'border-white/20 group-hover:border-accent-mint text-transparent'
                          }`}
                        >
                          ✓
                        </div>
                        <span className={`text-xs ${sub.isCompleted ? 'line-through text-slate-500' : 'text-slate-300'}`}>
                          {sub.title}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="bg-dark-card border border-dashed border-white/10 rounded-2xl p-8 text-center space-y-2">
          <CheckSquare size={28} className="mx-auto text-slate-500" />
          <h4 className="text-sm font-semibold text-white">No tasks in this view</h4>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Stay on top of your daily execution. Add tasks with deadlines, duration estimates, and subtasks.
          </p>
        </div>
      )}

      {/* Create Task Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create New Task"
      >
        <form onSubmit={handleCreateTask} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Task Title *</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="e.g. Implement Room Database Migrations"
              className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Description</label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={2}
              placeholder="Context or acceptance criteria..."
              className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-mint"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Priority</label>
              <select
                value={priority}
                onChange={e => setPriority(e.target.value as Priority)}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              >
                <option value="URGENT">Urgent (!)</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Category</label>
              <select
                value={categoryId}
                onChange={e => setCategoryId(e.target.value)}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              >
                {categories.map(c => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Deadline Date</label>
              <input
                type="date"
                value={deadlineDate}
                onChange={e => setDeadlineDate(e.target.value)}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Deadline Time</label>
              <input
                type="time"
                value={deadlineTime}
                onChange={e => setDeadlineTime(e.target.value)}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Duration (mins)</label>
              <input
                type="number"
                min="5"
                step="5"
                value={estimatedDurationMinutes}
                onChange={e => setEstimatedDurationMinutes(Number(e.target.value))}
                className="w-full bg-dark-card border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-accent-mint"
              />
            </div>
          </div>

          {/* Subtasks Builder */}
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Subtasks (Checklist)</label>
            <div className="flex items-center gap-2 mb-2">
              <input
                type="text"
                value={newSubtaskTitle}
                onChange={e => setNewSubtaskTitle(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddSubtaskInput();
                  }
                }}
                placeholder="Add subtask step..."
                className="flex-1 bg-dark-card border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent-mint"
              />
              <button
                type="button"
                onClick={handleAddSubtaskInput}
                className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-medium"
              >
                Add
              </button>
            </div>
            {subtasksList.length > 0 && (
              <div className="space-y-1.5 max-h-32 overflow-y-auto">
                {subtasksList.map((st, idx) => (
                  <div key={idx} className="flex items-center justify-between bg-dark-card px-2.5 py-1.5 rounded-lg text-xs text-white border border-white/5">
                    <span className="line-clamp-1">{st}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveSubtaskInput(idx)}
                      className="text-slate-400 hover:text-rose-400 ml-2"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Submit */}
          <div className="pt-2 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsCreateModalOpen(false)}
              className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-accent-mint text-dark-base font-bold text-xs shadow-md hover:scale-105 active:scale-95 transition-all"
            >
              Create Task
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
