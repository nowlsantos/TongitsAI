# AGENTS.MD - AI Development & Engineering Standards (AITongits)

## 1. Overview & Agent Mission
This document defines the strict engineering guidelines, architectural invariants, coding conventions, and tool execution standards for AI coding agents working within the **AITongits** card game project.

**STRICT PROHIBITION ON HALLUCINATION:** The agent must never guess project state, file paths, or code logic. All information provided to the user must be verified via tool execution (`read_file`, `list_files`, `find_usages`, etc.). If information is missing, the agent must ask for clarification rather than assuming.

**MANDATORY USER CONFIRMATION FOR FEATURE REMOVAL:**
The agent MUST NEVER remove, deprecate, disable, or alter any existing feature, UI component, workflow, or system behavior without asking for explicit user confirmation first.

**MANDATORY DEEP SCRUTINY:**
Every proposed solution, refactoring, or code change MUST be the result of analytical scrutiny across state flows, AI inference lifecycles, and architectural boundaries.

---

## 2. Tech Stack & Ecosystem Standards
- **Platform / IDE:** Android Studio (Latest Stable), targeting Android API level 36 (`minSdk 26`, `compileSdk 36`, `targetSdk 36`).
- **Language:** Kotlin (Latest stable version, strict null safety).
- **Frontend UI:** Jetpack Compose with Material 3, adhering strictly to Unidirectional Data Flow (UDF) and edge-to-edge display rules.
- **Dependency Injection:** Hilt (`hilt-android`, `hilt-navigation-compose`) with KSP.
- **AI Inference & Logic:** Minimax AI (`AiMinimax`), Move Generators (`MoveGenerator`), and State Machine (`GameStateMachine`).
- **Architecture Pattern:** Clean Architecture principles structured within the Android module (`domain`, `data`, `presentation`, `di`).
- **Asynchronous Concurrency:** Kotlin Coroutines and `StateFlow` / `SharedFlow`. **Strict prohibition:** Never use RxJava or LiveData.

---

## 3. Project Architecture & Directory Layout (`:app`)
- **`com.nowlsantos.tongitsai.domain.fsm`**: State machines handling game phases (`GameStateMachine`), human interactions (`HumanStateMachine`), and player turn workflows (`PlayerStateMachine`).
- **`com.nowlsantos.tongitsai.domain.ai`**: Advanced AI challenge logic, Minimax tree search (`AiMinimax`), and move generation (`MoveGenerator`).
- **`com.nowlsantos.tongitsai.domain.model`**: Core game models (`GameState`, `PlayingCard`, `Deck`, `Meld`, `Player`, `DiscardPile`).
- **`com.nowlsantos.tongitsai.data`**: Repositories and local persistence for game sessions (`GameRepository`, `GameData`).
- **`com.nowlsantos.tongitsai.presentation`**: ViewModels (`GameViewModel`) and Jetpack Compose UI screens.
- **`com.nowlsantos.tongitsai.di`**: Hilt dependency injection modules (`GameModule`).

---

## 4. Coding Style & Architectural Rules

### 4.1. Jetpack Compose & UI Layer
1. **Stateless Composables:** Screens must have a stateful root collecting from ViewModel and stateless content.
2. **Material 3 & Theming:** Consume styling from `MaterialTheme`. Leverage Jetpack Compose Styles API (`Modifier.styleable`) for custom components and interactive states.
3. **Edge-to-Edge & Insets:** Ensure all composables properly handle window insets (system bars and gesture navigation).
4. **Localization:** All user-facing text MUST use `stringResource(R.string...)` (Zero raw strings).
5. **Lifecycle Awareness:** Use `collectAsStateWithLifecycle()`.

### 4.2. ViewModel & State Management
- Follow strict **Unidirectional Data Flow (UDF)**.
- Expose only read-only `StateFlow` (`StateFlow<UiState>`) to the UI, keeping mutation methods private within ViewModels and State Machines.

### 4.3. Error Handling & Robustness
- Use Kotlin's `Result<T>` for domain/data boundaries.
- UI state should include structured error handling and loading states.

---

## 5. Agent Quality Checklist
Before finalizing code, verify:
- [x] **Zero Hallucination:** All logic is grounded in actual project files.
- [x] **No Raw Strings:** All UI text is in `strings.xml`.
- [x] **UDF Enforcement:** ViewModel state mutations are private and flows are read-only.
- [x] **Resource Cleanup:** Native resources and coroutine scopes are properly managed and closed.
- [x] **Edge-to-Edge & M3 Compliance:** UI properly respects system insets and Material 3 design tokens.
