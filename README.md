# 🎴 TongitsAI

**TongitsAI** is a modern, high-performance Android implementation of **Tongits**—the premier 3-player rummy-style card game popular in the Philippines. Built natively with **Kotlin** and **Jetpack Compose**, it features a **Finite State Machine (FSM)** architecture and a **Dual-Engine Hybrid AI** (Deep PyTorch / ONNX Policy Neural Network + Minimax Tactical Engine).

---

## 🌟 Features

### 🎴 Native Jetpack Compose & Modern UI
- **Immersive 10-Foot Table Layout**: Designed for tablets and mobile devices in landscape orientation with full-screen immersive mode.
- **Dynamic 3D Chips System**: Visual casino chip stacks (`DynamicChipStack`) that automatically break down balances and pot fees into **30** (Purple), **20** (Blue), and **10** (Red) denomination chips.
- **60fps Confetti Celebration**: Canvas-based particle animation loop (`ConfettiView`) triggered automatically upon Game Over and overall champion victory.
- **Customization**: Switchable card back designs, table background felts, and opponent trios.
- **Clean Navigation & Dialog System**: Dedicated dialog managers for Calls, Showdowns, Round Summaries, and an Android-standard `ShowExitDialog`.

### 🧠 Dual-Engine Hybrid AI Strategy
- **ONNX Deep Policy Network**: A 512-hidden multi-layer neural network trained via 50,000 self-play Reinforcement Learning cycles (3.2 million decision states).
- **5 Unified Strategic Reward Habits**:
  1. **Clean Win & Pot Jackpot**: High rewards (+5.0 / +4.0) for clean Tongits wins and 2-hit consecutive victories.
  2. **Anti-Feeding Defense**: -2.0 penalty for discarding cards that opponents can Chow or Sapaw.
  3. **Strategic Deadwood Shedding**: +1.5 reward for discarding high-value cards (J, Q, K) early in the round.
  4. **Smart Discard Pile Pickup**: +1.5 reward for capitalizing on the discard pile to complete 3+ card melds.
  5. **Secret Meld Preservation**: +1.0 reward for concealing hand strength when deck > 15.
  6. **Smart Sapaw Strategy**: +2.0 emergency Hitter suppression, -1.5 redundant early Sapaw penalty, and +1.0 late-game deadwood dumping.
- **Kotlin Tactical Minimax Engine**: Evaluates 100% legal constraints, maximal subset Chow pruning, own-meld Sapaw protection, and 1-turn win lookaheads.

### 🎮 Complete Tongits Game Rules Engine
- **Game Phases**: `SETUP` $\rightarrow$ `DEALING` $\rightarrow$ `PLAYING` $\rightarrow$ `CALL_CHALLENGE` $\rightarrow$ `REVEAL` $\rightarrow$ `ROUND_END` $\rightarrow$ `GAME_OVER`.
- **Turn Actions**: Draw from deck, Chow from discard pile, Meld 3+ card sets/runs, Sapaw onto table melds, and Discard.
- **Call / Challenge System**: Clockwise rotation, house & secret meld checks, and **Last Revealer Tie-Breaker Rule** (in 2-way/3-way score ties, the last player in reveal order wins).
- **Conserved Payment System**: Accumulated Center Pot, strict chip conservation laws, bankruptcy detection, and persistent round history tracking.

---

## 🛠️ Architecture & Tech Stack

```
TongitsAI/
├── app/src/main/java/com/nowlsantos/tongitsai/
│   ├── data/           # Repositories & Data Models (GameData, PlayerSummary)
│   ├── di/             # Koin Dependency Injection Modules
│   ├── domain/
│   │   ├── ai/         # AI Engines (OnnxAiEngine, AiMinimax, MoveGenerator, GameRules)
│   │   ├── fsm/        # Finite State Machines (GameStateMachine, PlayerStateMachine)
│   │   └── model/      # Pure Domain Models (Player, PlayingCard, Meld, GameState)
│   ├── presentation/   # ViewModels & UI Mappers (GameViewModel, PaymentProcessor)
│   └── ui/             # Jetpack Compose UI
│       ├── animations/ # GameAnimator & Card Layouts
│       ├── components/ # ConfettiView, DynamicChipStack, CenterPot
│       ├── dialogs/    # CallChallengeDialog, RoundWinnerDialog, ShowExitDialog
│       ├── players/    # PlayerSeat, PlayerBalance, PlayerMeld
│       └── screen/     # GameScreen & LobbyScreen
└── trainer/            # PyTorch Reinforcement Learning Training Pipeline
    ├── reward_shaped_self_play.py  # Master Unified RL Training & ONNX Export Script
    └── requirements.txt            # Python Dependencies
```

- **Language**: Kotlin 2.0+ & Python 3.10+
- **UI Framework**: Jetpack Compose 1.7+, Material 3
- **Dependency Injection**: Koin 3.5+
- **AI / ML Runtime**: PyTorch 2.0+, ONNX Runtime Android (`com.microsoft.onnxruntime:onnxruntime-android`), ONNX Opset 18
- **Concurrency & State**: Kotlin Coroutines, StateFlow, SharedFlow

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug / 2024.2.1 or newer
- **JDK**: Java 17 or Java 21
- **Android SDK**: API 24 (Android 7.0) minimum, API 34+ target
- **Python** *(Optional, only for re-training AI)*: Python 3.10+ with `torch` and `onnx`

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/TongitsAI.git
   cd TongitsAI
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and run on an Android device or tablet emulator in landscape mode:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🤖 Training the AI Model (Optional)

The pre-trained ONNX neural network model is already included in `app/src/main/assets/tongits_policy_model.onnx`.

If you wish to re-train the self-play Reinforcement Learning model with updated reward functions:

1. Navigate to the `trainer/` directory:
   ```bash
   cd trainer
   pip install -r requirements.txt
   ```
2. Execute the master self-play training script (50,000 cycles / 3.2M decisions):
   ```bash
   python reward_shaped_self_play.py
   ```
3. The script will automatically train the deep policy network, export `tongits_policy_model.onnx`, and deploy it directly into `app/src/main/assets/`.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
