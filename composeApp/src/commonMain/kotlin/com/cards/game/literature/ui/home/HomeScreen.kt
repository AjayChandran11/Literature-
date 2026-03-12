package com.cards.game.literature.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartGame: (playerName: String, playerCount: Int) -> Unit) {
    var playerName by remember { mutableStateOf("") }
    var showSetupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\u2660 \u2665 \u2666 \u2663",
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
            color = GoldAccent
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Literature",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The Classic Card Game",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Your Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                focusedLabelColor = GoldAccent,
                cursorColor = GoldAccent
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { showSetupDialog = true },
            enabled = playerName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.outline
            )
        ) {
            Text("New Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showSetupDialog) {
        GameSetupDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { playerCount ->
                showSetupDialog = false
                onStartGame(playerName.trim(), playerCount)
            }
        )
    }
}

@Composable
fun GameSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(6) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Setup", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select number of players:", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                listOf(4, 6, 8).forEach { count ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$count Players (${count / 2}v${count / 2})",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedCount) }) {
                Text("Start Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
