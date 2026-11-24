import React, { useEffect } from 'react';
import { useDayFlowStore } from './presentation/store';
import { Navbar } from './presentation/components/Navbar';
import { BottomNav } from './presentation/components/BottomNav';
import { TodayScreen } from './presentation/screens/TodayScreen';
import { RemindersScreen } from './presentation/screens/RemindersScreen';
import { TasksScreen } from './presentation/screens/TasksScreen';
import { HabitsScreen } from './presentation/screens/HabitsScreen';
import { InsightsScreen } from './presentation/screens/InsightsScreen';
import { CalendarScreen } from './presentation/screens/CalendarScreen';
import { SettingsScreen } from './presentation/screens/SettingsScreen';
import { ReviewScreen } from './presentation/screens/ReviewScreen';
import { BriefingScreen } from './presentation/screens/BriefingScreen';
import { PlanScreen } from './presentation/screens/PlanScreen';
import { Sparkles } from 'lucide-react';
import { StatusBar, Style } from '@capacitor/status-bar';
import { Capacitor } from '@capacitor/core';

export const App: React.FC = () => {
  const { activeScreen, isLoading, initApp } = useDayFlowStore();

  useEffect(() => {
    initApp();

    if (Capacitor.isNativePlatform()) {
      StatusBar.setOverlaysWebView({ overlay: false }).catch(() => {});
      StatusBar.setStyle({ style: Style.Dark }).catch(() => {});
      StatusBar.setBackgroundColor({ color: '#07120F' }).catch(() => {});
    }
  }, [initApp]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-dark-base flex flex-col items-center justify-center text-white space-y-4">
        <div className="w-12 h-12 rounded-2xl bg-accent-mint/10 border border-accent-mint/30 flex items-center justify-center text-accent-mint animate-pulse">
          <Sparkles size={24} />
        </div>
        <div className="text-center">
          <h1 className="text-lg font-bold font-mono tracking-tight">DAYFLOW</h1>
          <p className="text-xs text-slate-400 mt-1">Initializing local database...</p>
        </div>
      </div>
    );
  }

  const renderScreen = () => {
    switch (activeScreen) {
      case 'today':
        return <TodayScreen />;
      case 'reminders':
        return <RemindersScreen />;
      case 'tasks':
        return <TasksScreen />;
      case 'habits':
        return <HabitsScreen />;
      case 'insights':
        return <InsightsScreen />;
      case 'calendar':
        return <CalendarScreen />;
      case 'settings':
        return <SettingsScreen />;
      case 'review':
        return <ReviewScreen />;
      case 'briefing':
        return <BriefingScreen />;
      case 'plan':
        return <PlanScreen />;
      default:
        return <TodayScreen />;
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 flex justify-center text-slate-100 selection:bg-accent-mint selection:text-dark-base">
      {/* Mobile-first frame container */}
      <div className="w-full max-w-md min-h-screen bg-dark-base flex flex-col border-x border-white/5 relative shadow-2xl">
        <Navbar />
        <main className="flex-1 px-4 pt-4 overflow-y-auto">
          {renderScreen()}
        </main>
        <BottomNav />
      </div>
    </div>
  );
};

export default App;
