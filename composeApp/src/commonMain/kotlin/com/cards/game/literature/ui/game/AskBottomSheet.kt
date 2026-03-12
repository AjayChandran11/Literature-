package com.cards.game.literature.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.PlayerInfo

enum class AskStep { SELECT_HALF_SUIT, SELECT_CARD, SELECT_OPPONENT, CONFIRM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskBottomSheet(
    myHalfSuits: Set<HalfSuit>,
    opponents: List<PlayerInfo>,
    onConfirm: (targetId: String, card: Card) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(AskStep.SELECT_HALF_SUIT) }
    var selectedHalfSuit by remember { mutableStateOf<HalfSuit?>(null) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var selectedOpponent by remember { mutableStateOf<PlayerInfo?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = when (step) {
                    AskStep.SELECT_HALF_SUIT -> "Step 1: Pick a Half-Suit"
                    AskStep.SELECT_CARD -> "Step 2: Pick a Card"
                    AskStep.SELECT_OPPONENT -> "Step 3: Pick an Opponent"
                    AskStep.CONFIRM -> "Confirm Your Ask"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                AskStep.SELECT_HALF_SUIT -> {
                    myHalfSuits.sortedBy { it.ordinal }.forEach { hs ->
                        ListItem(
                            headlineContent = { Text(hs.displayName) },
                            modifier = Modifier.clickable {
                                selectedHalfSuit = hs
                                step = AskStep.SELECT_CARD
                            }
                        )
                    }
                }
                AskStep.SELECT_CARD -> {
                    val hs = selectedHalfSuit ?: return@Column
                    val allCards = DeckUtils.getAllCardsForHalfSuit(hs)
                    Text(
                        "Cards you DON'T have in ${hs.displayName}:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Show only cards the player doesn't have (they can't ask for cards they hold)
                    allCards.forEach { card ->
                        ListItem(
                            headlineContent = { Text(card.displayName) },
                            modifier = Modifier.clickable {
                                selectedCard = card
                                step = AskStep.SELECT_OPPONENT
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { step = AskStep.SELECT_HALF_SUIT }) {
                        Text("Back")
                    }
                }
                AskStep.SELECT_OPPONENT -> {
                    val activeOpponents = opponents.filter { it.isActive }
                    activeOpponents.forEach { opp ->
                        ListItem(
                            headlineContent = { Text(opp.name) },
                            supportingContent = { Text("${opp.cardCount} cards") },
                            modifier = Modifier.clickable {
                                selectedOpponent = opp
                                step = AskStep.CONFIRM
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { step = AskStep.SELECT_CARD }) {
                        Text("Back")
                    }
                }
                AskStep.CONFIRM -> {
                    val card = selectedCard ?: return@Column
                    val opp = selectedOpponent ?: return@Column

                    Text("Ask ${opp.name} for ${card.displayName}?", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = AskStep.SELECT_OPPONENT },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { onConfirm(opp.id, card) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ask!")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
