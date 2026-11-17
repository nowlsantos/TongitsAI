package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.domain.ai.AIChallengeLogic
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameInput.*
import com.nowlsantos.tongitsai.presentation.UiEffect.*
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CallChallengeState : GamePhaseState {
    private val tag = "CallChallengeState"
    private val foldeesPlayerIds = mutableSetOf<String>()
    private val callees = mutableSetOf<Player>()
    private var challengers: List<Player> = emptyList()
    private var challengerIndex: Int = 0 // Track which challenger is next
    private val suspenseDelay = 1500.milliseconds // Generous pause to read call announcement

    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d(tag, "Entering Call-Challenge phase")
        val gameState = stateMachine.gameState.value

        stateMachine.broadcastDecision(gameState.currentPlayer, MoveType.IDLE)

        val rawCaller = gameState.players.firstOrNull { it.id == gameState.callerId } ?: gameState.currentPlayer
        val caller = rawCaller.copy(challengerType = ChallengerType.CALLER)
        challengers = getChallengerOrder(stateMachine, caller)
        challengerIndex = 0

        stateMachine.updateState { currentState ->
            val updatedPlayers = currentState.players.map { p ->
                if (p.id == caller.id) caller else p
            }
            currentState.copy(callerId = caller.id, players = updatedPlayers)
        }

        stateMachine.emitUiEffect(
            InitialMessage(message = "${caller.name} has called!\nWaiting for opponents...")
        )

        stateMachine.externalScope.launch {
            delay(suspenseDelay)
            handleNextChallenger(stateMachine)
        }
    }

    override suspend fun handleInput(
        stateMachine: GameStateMachine,
        input: GameInput
    ) {
        Logger.d(tag, "Handling Input: $input")
        val gameState = stateMachine.gameState.value

        when (input) {
            is ChallengeDecision -> {
                val challenger = gameState.players.first { it.id == input.challengerId }

                if (input.accepts) {
                    Logger.d(tag, "${challenger.name} has accepted the challenge!")

                    stateMachine.emitUiEffect(
                        InitialMessage(message = "${challenger.name} has accepted the challenge!")
                    )

                    callees.add(challenger)

                    stateMachine.updateState { currentState ->
                        val updatedPlayers = currentState.players.map { player ->
                            if (challenger.id == player.id) {
                                when (challenger.id) {
                                    callees.elementAt(0).id -> {
                                        challenger.copy(challengerType = ChallengerType.CALLEE)
                                    }
                                    callees.elementAt(1).id -> {
                                        challenger.copy(challengerType = ChallengerType.LAST_CALLEE)
                                    }
                                    else -> player
                                }
                            } else {
                                player
                            }
                        }
                        currentState.copy(players = updatedPlayers)
                    }
                } else {
                    val message = if (challenger.melds.isEmpty()) "${challenger.name} has no house meld and folds." else "${challenger.name} folds."

                    stateMachine.emitUiEffect(InitialMessage(message = message))

                    Logger.d(tag, "${challenger.name} folded")
                    foldeesPlayerIds.add(challenger.id)

                    stateMachine.updateState { currentState ->
                        val updatedPlayers = currentState.players.map { player ->
                            if (challenger.id == player.id) {
                                when(challenger.id) {
                                    foldeesPlayerIds.elementAt(0) -> {
                                        challenger.copy(challengerType = ChallengerType.FOLDEE)
                                    }
                                    foldeesPlayerIds.elementAt(1) -> {
                                        challenger.copy(challengerType = ChallengerType.LAST_FOLDEE)
                                    }
                                    else -> player
                                }
                            } else {
                                player
                            }
                        }
                        currentState.copy(players = updatedPlayers)
                    }
                }

                // Generous pause to read challenge acceptance/fold result before next step
                stateMachine.externalScope.launch {
                    delay(suspenseDelay)
                    handleNextChallenger(stateMachine)
                }
            }
            else -> {
                Logger.w(tag, "Input of type ${input::class.simpleName} is not handled in CallChallengeState.")
            }
        }
    }

    private fun handleNextChallenger(stateMachine: GameStateMachine) {
        val gameState = stateMachine.gameState.value
        val caller = gameState.players.firstOrNull { it.id == gameState.callerId } ?: gameState.currentPlayer

        if (challengerIndex < challengers.size) {
            val rawChallenger = challengers[challengerIndex]
            val nextChallenger = gameState.players.firstOrNull { it.id == rawChallenger.id } ?: rawChallenger
            Logger.d(tag, "NextChallenger :: ${nextChallenger.name} | MeldsCount: ${nextChallenger.melds.size}")

            when (nextChallenger.type) {
                PlayerType.AI -> {
                    stateMachine.externalScope.launch {
                        stateMachine.emitUiEffect(
                            InitialMessage(message = "${nextChallenger.name} is considering the call...")
                        )
                        delay(suspenseDelay) // Generous thinking pause

                        val aiDecision = AIChallengeLogic.decideCallChallenge(nextChallenger, gameState)
                        val decisionMsg = if (aiDecision) "${nextChallenger.name} accepts the call!" else "${nextChallenger.name} folds."
                        stateMachine.emitUiEffect(InitialMessage(message = decisionMsg))
                        //delay(suspenseDelay) // Generous pause to read AI decision

                        stateMachine.handleInput(
                            ChallengeDecision(
                                callerId = caller.id,
                                challengerId = nextChallenger.id,
                                accepts = aiDecision
                            )
                        )
                    }
                }
                PlayerType.HUMAN -> {
                    stateMachine.emitUiEffect(ResetDialog)
                    if (nextChallenger.melds.isEmpty()) {
                        stateMachine.emitUiEffect(InitialMessage(message = "${nextChallenger.name} has no house meld and folds automatically."))
                        stateMachine.externalScope.launch {
                            delay(suspenseDelay)
                            stateMachine.handleInput(
                                ChallengeDecision(
                                    callerId = caller.id,
                                    challengerId = nextChallenger.id,
                                    accepts = false
                                )
                            )
                        }
                    } else {
                        stateMachine.emitUiEffect(
                            AskCallChallenge(
                                callerId = caller.id,
                                callerName = caller.name,
                                challengerId = nextChallenger.id,
                                message = "${caller.name} is challenging you!"
                            )
                        )
                    }
                }
            }
            challengerIndex++
        } else {
            stateMachine.emitUiEffect(InitialMessage(message = "All responses collected. Resolving challenge..."))

            stateMachine.externalScope.launch {
                delay(suspenseDelay) // Generous pause before showdown resolution
                stateMachine.emitUiEffect(ResetDialog)

                //val caller = gameState.players.firstOrNull { it.id == gameState.callerId } ?: gameState.currentPlayer
                //val showdownPlayers = challengers.filter { it.id !in foldeesPlayerIds } + caller
                
                //if (showdownPlayers.isNotEmpty()) {
                if (callees.isNotEmpty()) {
                    Logger.d(tag, "Showdown starting with: ${callees.joinToString { it.name }}")

                    stateMachine.updateState { currentState ->
                        currentState.copy(
                            revealType = RevealType.CHALLENGE,
                            isReveal = true
                        )
                    }
                    stateMachine.transitionTo(GamePhase.REVEAL)
                } else {
                    Logger.d(tag, "All Opponents folds. Transitioning to RoundEnd")
                    stateMachine.updateState { currentState ->
                        currentState.copy(
                            roundWinnerId = gameState.callerId,
                            revealType = RevealType.ALL_FOLDS,
                            isReveal = false
                        )
                    }
                    stateMachine.transitionTo(GamePhase.ROUND_END)
                }
            }
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d(tag, "Exiting Call-Challenge phase")
        foldeesPlayerIds.clear()
        callees.clear()
        challengers = emptyList()
    }

    private fun getChallengerOrder(stateMachine: GameStateMachine, caller: Player): List<Player> {
        val gameState = stateMachine.gameState.value
        val turnOrder = listOf(PlayerPosition.BOTTOM, PlayerPosition.RIGHT, PlayerPosition.LEFT)
        val sortedPlayers = gameState.players.sortedBy { turnOrder.indexOf(it.position) }
        val callerIndex = sortedPlayers.indexOfFirst { it.id == caller.id }
        if (callerIndex == -1) return sortedPlayers.filter { it.id != caller.id }
        val dynamicOrder = sortedPlayers.drop(callerIndex + 1) + sortedPlayers.take(callerIndex)
        return dynamicOrder.filter { it.id != caller.id }
    }
}
