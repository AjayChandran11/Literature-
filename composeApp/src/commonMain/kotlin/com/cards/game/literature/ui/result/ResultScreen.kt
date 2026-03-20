package com.cards.game.literature.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.game.GameLogEntry
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.ResultViewModel
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResultScreen(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: ResultViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLog by remember { mutableStateOf(false) }

    val myTeamDisplayName = uiState.myTeamName.ifEmpty { stringResource(Res.string.label_your_team) }
    val opponentTeamDisplayName = uiState.opponentTeamName.ifEmpty { stringResource(Res.string.label_opponents) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Winner banner
        Text(
            text = when {
                uiState.isDraw -> stringResource(Res.string.result_draw)
                uiState.isWinner -> stringResource(Res.string.result_win)
                else -> stringResource(Res.string.result_lose)
            },
            style = MaterialTheme.typography.displaySmall,
            color = when {
                uiState.isDraw -> MaterialTheme.colorScheme.secondary
                uiState.isWinner -> LightGreen
                else -> CardRed
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(myTeamDisplayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${uiState.myTeamScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = LightGreen
                )
            }
            Text(
                "-",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(opponentTeamDisplayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${uiState.opponentTeamScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Breakdown
        Text(
            stringResource(Res.string.result_breakdown_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            uiState.halfSuitBreakdown.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        status.halfSuit.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val claimedBy = when (status.claimedByTeamId) {
                        "team_1" -> myTeamDisplayName
                        "team_2" -> opponentTeamDisplayName
                        else -> stringResource(Res.string.result_unclaimed)
                    }
                    val claimColor = when (status.claimedByTeamId) {
                        "team_1" -> LightGreen
                        "team_2" -> CardRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        claimedBy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = claimColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game Log
        OutlinedButton(
            onClick = { showLog = !showLog },
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(if (showLog) stringResource(Res.string.result_hide_log) else stringResource(Res.string.result_show_log))
        }

        if (showLog) {
            Spacer(modifier = Modifier.height(8.dp))
            val displayEvents = uiState.gameLog.filterNot {
                it is GameEvent.TurnChanged || it is GameEvent.GameStarted
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                items(displayEvents) { event ->
                    GameLogEntry(event = event, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(Res.string.button_play_again), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(Res.string.button_home))
        }
    }
}
