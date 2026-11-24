# DAYFLOW

> **Make today count.**  
> Web-first, offline-first personal productivity and daily execution application.

---

## Overview

DAYFLOW is a web-first application previewable locally in any desktop browser at `http://localhost:5173` and packaged for Android via Capacitor. It consolidates daily task management, recurring interval reminders, habit tracking, deterministic daily planning, morning briefings, and evening reviews into a unified, privacy-first local dashboard.

### Core Pillars
- **100% Offline-First:** Zero network dependencies, zero user accounts, zero cloud storage. No backend, no trackers.
- **Deterministic Daily Planning:** Pure algorithmic schedule optimization with semi-automatic user approval — no AI gimmicks.
- **Reliable Reminders:** Pure recurrence calculation for one-time, daily, weekly, custom weekday, and interval alarms.
- **Privacy & Integrity:** Versioned JSON local backup and safe transactional restoration with schema integrity checks.
- **Design System:** Calm, wellness-inspired premium dark dashboard with mint-green (`#A5E6D6`), yellow (`#F0E99B`), and lavender (`#DCC3F5`) accents.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend UI | React 18+, TypeScript, Tailwind CSS, Lucide React, Recharts |
| State Management | Zustand |
| Runtime Validation | Zod |
| Local Database | IndexedDB via Dexie (Web) • SQLite via Capacitor (Android) |
| Native Packaging | Capacitor |
| Bundler & Dev Server | Vite 6 |
| Testing | Vitest, TypeScript compiler (`tsc`) |

---

## Quick Start & Local Preview

```bash
# 1. Install dependencies
npm install

# 2. Run local development preview (starts at http://localhost:5173)
npm run dev

# 3. Type check
npx tsc --noEmit

# 4. Run pure domain unit tests
npm test

# 5. Build production bundle
npm run build

# 6. Sync web assets into Android project
npx cap sync android

# 7. Build Android APK
cd android && ./gradlew assembleDebug

# Output APK:
# - Root: DayFlow.apk
# - Gradle output: android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```
src/
├── core/             # Design system theme tokens, time and date matrix utilities
├── domain/           # Pure TypeScript domain models, Zod schemas, recurrence engine,
│                     # streak calculator, deterministic planner, and briefing generator
├── data/             # IndexedDB Dexie database, default seed data, and repository layer
└── presentation/     # React UI components, 5 primary tabs (Today, Reminders, Tasks,
                      # Habits, Insights), Calendar, Planner, Review, and Zustand store
```
│   ├── mapper/           # Entity to Domain mappers
│   └── repository/       # Repository implementations (SettingsRepositoryImpl)
├── domain/
│   ├── model/            # Pure domain models (UserProfile, ThemePreference, Priority)
│   ├── planner/          # Deterministic planner engine contract
│   ├── recurrence/       # Recurrence calculator contract
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Domain use cases
├── feature/
│   ├── onboarding/       # Onboarding flow (FR-01)
│   ├── today/            # Today execution dashboard shell
│   ├── reminders/        # Reminder management
│   ├── tasks/            # Task management
│   ├── habits/           # Habit tracking
│   ├── insights/         # Analytics & execution score
│   ├── calendar/         # Day, week, month calendar
│   ├── review/           # Evening review workflow
│   └── settings/         # App settings & theme configuration
├── scheduling/           # AlarmManager integration & receivers
├── notifications/        # NotificationManager channels & dispatching
└── backup/               # WorkManager local backup engine
```

---

## Building and Running

### Prerequisites
- JDK 17 (Microsoft OpenJDK 17 or Android Studio JBR)
- Android SDK (API 35/36 installed, `minSdk = 26`)

### Commands
```powershell
# Assemble Debug APK
.\gradlew.bat assembleDebug

# Assemble Production Release APK (Minified & Resource Shrunk with R8)
.\gradlew.bat assembleRelease

# Run Complete Unit Test Suite
.\gradlew.bat testDebugUnitTest

# Run Lint Analysis
.\gradlew.bat lint

# Run Destructive Migration Safety Check
python scripts/check_no_destructive_migrations.py
```

---

## Documentation

All 11 project governance and architectural documents are maintained in the repository:
- [PRD.md](file:///C:/Projects/DayFlow/PRD.md): Product Requirements Document
- [DESIGN_SYSTEM.md](file:///C:/Projects/DayFlow/DESIGN_SYSTEM.md): Visual and component specifications
- [ARCHITECTURE.md](file:///C:/Projects/DayFlow/ARCHITECTURE.md): Architectural patterns, algorithms, and layer boundaries
- [DATABASE.md](file:///C:/Projects/DayFlow/DATABASE.md): SQLite schema, Room DAOs, entity relationships, and indexing
- [SECURITY.md](file:///C:/Projects/DayFlow/SECURITY.md): Threat model, offline data sovereignty, encryption, and SAF boundaries
- [CODE_STYLE.md](file:///C:/Projects/DayFlow/CODE_STYLE.md): Kotlin, Compose, and architecture standards
- [TESTING.md](file:///C:/Projects/DayFlow/TESTING.md): Testing matrix, Robolectric unit tests, and device verification checklist
- [AGENTS.md](file:///C:/Projects/DayFlow/AGENTS.md): Project rules and non-negotiable architectural constraints
- [IMPLEMENTATION_ROADMAP.md](file:///C:/Projects/DayFlow/IMPLEMENTATION_ROADMAP.md): Detailed 12-phase implementation tracking
- [RELEASE_CHECKLIST.md](file:///C:/Projects/DayFlow/RELEASE_CHECKLIST.md): Release verification gates, system recovery checklist, and OEM battery optimization guide
- [README.md](file:///C:/Projects/DayFlow/README.md): Project overview and quick start guide
- [TESTING.md](file:///c:/Projects/DayFlow/TESTING.md): Testing methodology and guidelines
- [IMPLEMENTATION_ROADMAP.md](file:///c:/Projects/DayFlow/IMPLEMENTATION_ROADMAP.md): Project phases and progress
