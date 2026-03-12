package com.cards.game.literature.ui.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.GameViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameBoardScreen(
    playerName: String,
    playerCount: Int,
    onGameEnd: () -> Unit,
    viewModel: GameViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gameLog by viewModel.gameLog.collectAsState()

    var showAskSheet by remember { mutableStateOf(false) }
    var showClaimSheet by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }

    // Start game on first composition
    LaunchedEffect(Unit) {
        viewModel.startGame(playerName, playerCount)
    }

    // Navigate to result when game ends
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.FINISHED) {
            onGameEnd()
        }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Score bar
            ScoreBar(
                myTeamScore = uiState.myTeamScore,
                opponentTeamScore = uiState.opponentTeamScore
            )

            // Deck tracker
            DeckTracker(
                statuses = uiState.halfSuitStatuses,
                myTeamId = uiState.myTeamId
            )

            // Opponents
            Text(
                "Opponents",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            OpponentRow(opponents = uiState.opponents)

            // Teammates
            if (uiState.teammates.isNotEmpty()) {
                Text(
                    "Teammates",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                TeammateRow(teammates = uiState.teammates)
            }

            // Turn indicator
            AnimatedVisibility(visible = uiState.phase == GamePhase.IN_PROGRESS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (uiState.isMyTurn) GoldAccent.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isMyTurn) {
                        Text(
                            "Your Turn!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    } else if (uiState.isBotThinking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${uiState.activePlayerName} is thinking...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "${uiState.activePlayerName}'s turn",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Game log
            GameLogPanel(events = gameLog)

            // Player's hand
            CardHand(
                handByHalfSuit = uiState.myHandByHalfSuit,
                selectedCard = selectedCard,
                onCardSelected = { selectedCard = it }
            )

            // Action buttons
            ActionButtons(
                isMyTurn = uiState.isMyTurn,
                onAskCard = { showAskSheet = true },
                onClaimDeck = { showClaimSheet = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Ask bottom sheet
    if (showAskSheet) {
        AskBottomSheet(
            myHalfSuits = uiState.myHandByHalfSuit.keys,
            opponents = uiState.opponents,
            onConfirm = { targetId, card ->
                showAskSheet = false
                viewModel.askCard(targetId, card)
            },
            onDismiss = { showAskSheet = false }
        )
    }

    // Claim bottom sheet
    if (showClaimSheet) {
        ClaimBottomSheet(
            myPlayerId = uiState.myPlayerId,
            myHand = uiState.myHand,
            myHalfSuits = uiState.myHandByHalfSuit.keys,
            teammates = uiState.teammates,
            halfSuitStatuses = uiState.halfSuitStatuses,
            onConfirm = { declaration ->
                showClaimSheet = false
                viewModel.claimDeck(declaration)
            },
            onDismiss = { showClaimSheet = false }
        )
    }
}
