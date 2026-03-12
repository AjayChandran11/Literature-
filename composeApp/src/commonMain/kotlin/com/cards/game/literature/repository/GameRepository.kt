package com.cards.game.literature.repository

import com.cards.game.literature.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    val gameState: StateFlow<GameState?>
    val gameEvents: Flow<GameEvent>

    suspend fun createGame(playerName: String, playerCount: Int): GameState
    suspend fun submitAsk(askerId: String, targetId: String, card: Card)
    suspend fun submitClaim(declaration: ClaimDeclaration)
}
