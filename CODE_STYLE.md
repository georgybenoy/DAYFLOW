# DAYFLOW — CODE_STYLE.md

## 1. Principles
DAYFLOW code prioritizes clarity, correctness, and maintainability over clever abstractions or excessive brevity.

## 2. Naming Conventions
- **Classes & Interfaces:** PascalCase (e.g. `SettingsRepositoryImpl`, `GreetingHeader`, `RecurrenceCalculator`).
- **Functions & Methods:** camelCase (e.g. `selectDate`, `calculateNextOccurrence`).
- **Composables:** PascalCase (e.g. `MetricCard`, `DayflowScaffold`).
- **Variables & Properties:** camelCase (e.g. `selectedDate`, `userProfile`).
- **Constants:** UPPER_SNAKE_CASE (e.g. `DEFAULT_QUIET_HOURS_START`).
- **Packages:** lowercase dot-separated (e.g. `com.dayflow.app.core.designsystem`).

## 3. Jetpack Compose Guidelines
- **Theme Centralization:** Never hardcode raw hex colors or arbitrary dp values in composables. Always reference `DayflowTheme.colors`, `DayflowTheme.spacing`, and `DayflowTheme.shapes`.
- **Previews:** Every reusable component and full-screen composable must include at least one `@Preview` demonstrating its default dark theme appearance.
- **State Hoisting:** Composables accept state as parameters and emit events via lambda callbacks.
- **Accessibility:** Touch targets must meet or exceed 48x48dp. Interactive elements must provide meaningful content descriptions for accessibility services.
- **Performance:** Avoid heavy allocations in composable functions. Defer reading layout state until draw or layout phase where appropriate.

## 4. Coroutines & Concurrency
- **Dispatchers:** Always inject CoroutineDispatchers (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`) rather than referencing `Dispatchers.IO` directly in business logic.
- **Exception Handling:** Never swallow exceptions silently. Use structured concurrency with SupervisorJob when spawning parallel tasks.
- **Flows:** Use `stateIn` with `SharingStarted.WhileSubscribed(5000)` in ViewModels for observable UI state.

## 5. Clean Architecture Guardrails
- UI composables must never access Room DAOs or DataStore directly.
- ViewModels communicate with Domain Use Cases or Repositories.
- Domain models are immutable `data class` types.
