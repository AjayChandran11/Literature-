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
import com.cards.game.literature.model.*
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.PlayerInfo

enum class ClaimStep { SELECT_HALF_SUIT, ASSIGN_CARDS, CONFIRM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimBottomSheet(
    myPlayerId: String,
    myHand: List<Card>,
    myHalfSuits: Set<HalfSuit>,
    teammates: List<PlayerInfo>,
    halfSuitStatuses: List<HalfSuitStatus>,
    onConfirm: (ClaimDeclaration) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(ClaimStep.SELECT_HALF_SUIT) }
    var selectedHalfSuit by remember { mutableStateOf<HalfSuit?>(null) }
    var assignments by remember { mutableStateOf<MutableMap<Card, String>>(mutableMapOf()) }

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
                    ClaimStep.SELECT_HALF_SUIT -> "Step 1: Pick Half-Suit to Claim"
                    ClaimStep.ASSIGN_CARDS -> "Step 2: Assign Cards to Players"
                    ClaimStep.CONFIRM -> "Confirm Your Claim"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                ClaimStep.SELECT_HALF_SUIT -> {
                    // Show only unclaimed half-suits that the player has cards in
                    val availableHalfSuits = myHalfSuits.filter { hs ->
                        halfSuitStatuses.find { it.halfSuit == hs }?.claimedByTeamId == null
                    }
                    availableHalfSuits.sortedBy { it.ordinal }.forEach { hs ->
                        ListItem(
                            headlineContent = { Text(hs.displayName) },
                            modifier = Modifier.clickable {
                                selectedHalfSuit = hs
                                // Auto-assign my own cards
                                val cards = DeckUtils.getAllCardsForHalfSuit(hs)
                                val newAssignments = mutableMapOf<Card, String>()
                                cards.forEach { card ->
                                    if (card in myHand) {
                                        newAssignments[card] = myPlayerId
                                    }
                                }
                                assignments = newAssignments
                                step = ClaimStep.ASSIGN_CARDS
                            }
                        )
                    }
                }
                ClaimStep.ASSIGN_CARDS -> {
                    val hs = selectedHalfSuit ?: return@Column
                    val allCards = DeckUtils.getAllCardsForHalfSuit(hs)
                    val allTeamPlayers = listOf(
                        PlayerInfo(myPlayerId, "You", myHand.size, true, false)
                    ) + teammates

                    Text(
                        "Assign each card to the teammate who holds it:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    allCards.forEach { card ->
                        val isMyCard = card in myHand
                        val currentAssignment = assignments[card]
                        val assignedName = when (currentAssignment) {
                            myPlayerId -> "You"
                            null -> "Unassigned"
                            else -> allTeamPlayers.find { it.id == currentAssignment }?.name ?: "?"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                card.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(60.dp)
                            )
                            if (isMyCard) {
                                Text(
                                    "You (locked)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                // Dropdown to pick a teammate
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(assignedName, fontSize = 12.sp)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        allTeamPlayers.forEach { player ->
                                            DropdownMenuItem(
                                                text = { Text(player.name) },
                                                onClick = {
                                                    val newMap = assignments.toMutableMap()
                                                    newMap[card] = player.id
                                                    assignments = newMap
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val allAssigned = allCards.all { assignments.containsKey(it) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = ClaimStep.SELECT_HALF_SUIT },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { step = ClaimStep.CONFIRM },
                            enabled = allAssigned,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Review")
                        }
                    }
                }
                ClaimStep.CONFIRM -> {
                    val hs = selectedHalfSuit ?: return@Column

                    Text(
                        "Claiming ${hs.displayName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "WARNING: If any assignment is wrong, the opponent gets the point!",
                        fontSize = 12.sp,
                        color = CardRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    assignments.entries.groupBy { it.value }.forEach { (playerId, cards) ->
                        val name = if (playerId == myPlayerId) "You"
                        else teammates.find { it.id == playerId }?.name ?: "?"
                        Text(
                            "$name: ${cards.joinToString(", ") { it.key.displayName }}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = ClaimStep.ASSIGN_CARDS },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                // Convert assignments to declaration format
                                val cardAssignments = mutableMapOf<String, MutableList<Card>>()
                                assignments.forEach { (card, playerId) ->
                                    cardAssignments.getOrPut(playerId) { mutableListOf() }.add(card)
                                }
                                onConfirm(
                                    ClaimDeclaration(
                                        claimerId = myPlayerId,
                                        halfSuit = hs,
                                        cardAssignments = cardAssignments.mapValues { it.value.toList() }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text("CLAIM!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
