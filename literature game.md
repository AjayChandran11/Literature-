# Literature Card Game — Full Development Reference
> Use this document as the single source of truth for building the Literature card game app.

---

## 1. Game Rules & Logic

### 1.1 The Deck
- Standard 52-card deck, **remove all four 8s** → 48 cards total
- Cards are grouped into **8 half-suits** (called "decks" in-game):

| Half-Suit | Cards (6 per suit × 4 suits) |
|-----------|-------------------------------|
| ♠ Lower | 2♠ 3♠ 4♠ 5♠ 6♠ 7♠ |
| ♠ Upper | 9♠ 10♠ J♠ Q♠ K♠ A♠ |
| ♥ Lower | 2♥ 3♥ 4♥ 5♥ 6♥ 7♥ |
| ♥ Upper | 9♥ 10♥ J♥ Q♥ K♥ A♥ |
| ♦ Lower | 2♦ 3♦ 4♦ 5♦ 6♦ 7♦ |
| ♦ Upper | 9♦ 10♦ J♦ Q♦ K♦ A♦ |
| ♣ Lower | 2♣ 3♣ 4♣ 5♣ 6♣ 7♣ |
| ♣ Upper | 9♣ 10♣ J♣ Q♣ K♣ A♣ |

### 1.2 Players & Teams
| Mode | Players | Teams | Cards per Player |
|------|---------|-------|-----------------|
| 4-player | 4 | 2×2 | 12 cards |
| 6-player | 6 | 2×3 | 8 cards |
| 8-player | 8 | 2×4 | 6 cards |

> Total cards always = 48. Cards per player = 48 ÷ number of players.

### 1.3 Turn Structure
1. The active player selects a **target** (must be from the opponent team).
2. The active player selects a **card** to ask for.
3. **Validation** (both must pass):
   - The active player must hold **at least one card from the same half-suit** as the card being asked.
   - The active player must **not already hold** the card being asked.
4. **If the target has the card**: The card is transferred. The active player **retains their turn**.
5. **If the target does not have the card**: The turn **passes to the target player**.

### 1.4 Claiming a Half-Suit ("Declaring a Deck")
- Only the **active player** (whose turn it is) may initiate a claim.
- The claiming player must declare the **exact location of all 6 cards** in the half-suit:
  - Cards in their own hand are auto-assigned.
  - Each remaining card must be assigned to a specific **teammate**.
- **Claim resolution**:
  - ✅ All 6 cards are correctly assigned → **Claiming team gets +1 point**. The half-suit is removed from play.
  - ❌ Even one card is assigned to the wrong player → **Opponent team gets +1 point**. The half-suit is still removed from play.
  - ❌ The cards are not all within the claiming team → **Opponent team gets +1 point**. Half-suit removed from play.
- After a successful claim, if the claiming player has 0 cards remaining, **any teammate** takes the next turn.

### 1.5 Player Elimination (Running Out of Cards)
- A player with **0 cards** is inactive and skips their turn automatically.
- Their teammates continue playing on behalf of the team.
- An inactive player can never ask or claim.
- Turn order skips inactive players.

### 1.6 Win Condition
- Game ends when all **8 half-suits** have been claimed (correctly or incorrectly).
- The team with **more points (out of 8)** wins.
- A **4-4 tie** is possible — handle with a "Draw" result screen.

### 1.7 Information Visibility
- Every card **ask** and **transfer** is visible to **all players** in the game log.
- Players do **not** see each other's hands directly.
- All deduction must come from tracking the game log.

---

## 2. Data Models (Kotlin — Shared KMP Module)

```kotlin
// Suits
enum class Suit { SPADES, HEARTS, DIAMONDS, CLUBS }

// Card values (no 8s)
enum class CardValue {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN,
    NINE, TEN, JACK, QUEEN, KING, ACE
}

// Half-suit designation
enum class HalfSuit {
    SPADES_LOWER, SPADES_UPPER,
    HEARTS_LOWER, HEARTS_UPPER,
    DIAMONDS_LOWER, DIAMONDS_UPPER,
    CLUBS_LOWER, CLUBS_UPPER
}

data class Card(
    val suit: Suit,
    val value: CardValue,
    val halfSuit: HalfSuit // derived, always computable from suit + value
)

data class Player(
    val id: String,
    val name: String,
    val teamId: String,
    val hand: MutableList<Card>,
    val isActive: Boolean, // false when hand is empty
    val isBot: Boolean = false
)

data class Team(
    val id: String,
    val name: String,
    val playerIds: List<String>,
    val score: Int = 0
)

// Each event appended to the game log
sealed class GameEvent {
    data class CardAsked(
        val askerId: String,
        val targetId: String,
        val card: Card,
        val success: Boolean,
        val timestamp: Long
    ) : GameEvent()

    data class DeckClaimed(
        val claimerId: String,
        val halfSuit: HalfSuit,
        val declaration: Map<String, List<Card>>, // playerId -> cards they declared
        val success: Boolean,
        val pointsTo: String, // teamId
        val timestamp: Long
    ) : GameEvent()

    data class TurnChanged(
        val newActivePlayerId: String,
        val reason: String // "card_not_found" | "claim_complete" | "no_cards"
    ) : GameEvent()
}

data class HalfSuitStatus(
    val halfSuit: HalfSuit,
    val isClaimed: Boolean = false,
    val claimedByTeamId: String? = null
)

data class GameState(
    val gameId: String,
    val roomCode: String,
    val players: List<Player>,
    val teams: List<Team>,
    val activePlayerId: String,
    val halfSuitStatuses: List<HalfSuitStatus>,
    val log: List<GameEvent>,
    val phase: GamePhase,
    val winnerId: String? = null // teamId, null if game ongoing
)

enum class GamePhase {
    WAITING,    // In lobby, waiting for players
    IN_PROGRESS,
    FINISHED
}

// Claim declaration structure submitted by client
data class ClaimDeclaration(
    val claimerId: String,
    val halfSuit: HalfSuit,
    val cardAssignments: Map<String, List<Card>> // teammateId -> cards assigned to them
)
```

---

## 3. Validation Rules (MoveValidator)

```kotlin
object MoveValidator {

    fun validateAsk(
        state: GameState,
        askerId: String,
        targetId: String,
        card: Card
    ): ValidationResult {
        val asker = state.players.find { it.id == askerId }
            ?: return ValidationResult.Error("Asker not found")
        val target = state.players.find { it.id == targetId }
            ?: return ValidationResult.Error("Target not found")

        if (state.activePlayerId != askerId)
            return ValidationResult.Error("Not your turn")
        if (asker.teamId == target.teamId)
            return ValidationResult.Error("Cannot ask a teammate")
        if (asker.hand.none { it.halfSuit == card.halfSuit })
            return ValidationResult.Error("You have no card from this half-suit")
        if (asker.hand.contains(card))
            return ValidationResult.Error("You already own this card")
        if (state.halfSuitStatuses.find { it.halfSuit == card.halfSuit }?.isClaimed == true)
            return ValidationResult.Error("This half-suit is already claimed")

        return ValidationResult.Ok
    }

    fun validateClaim(
        state: GameState,
        declaration: ClaimDeclaration
    ): ValidationResult {
        val claimer = state.players.find { it.id == declaration.claimerId }
            ?: return ValidationResult.Error("Claimer not found")

        if (state.activePlayerId != declaration.claimerId)
            return ValidationResult.Error("Not your turn")

        // All declared players must be teammates (including self)
        val team = state.teams.find { it.id == claimer.teamId }!!
        val declaredPlayerIds = declaration.cardAssignments.keys
        if (!team.playerIds.containsAll(declaredPlayerIds))
            return ValidationResult.Error("Can only assign cards to teammates")

        // All 6 cards of the half-suit must be declared
        val allCardsInHalfSuit = getAllCardsForHalfSuit(declaration.halfSuit)
        val allDeclaredCards = declaration.cardAssignments.values.flatten()
        if (allDeclaredCards.toSet() != allCardsInHalfSuit.toSet())
            return ValidationResult.Error("Must declare all 6 cards of the half-suit")

        return ValidationResult.Ok
    }
}

sealed class ValidationResult {
    object Ok : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

---

## 4. Claim Adjudication (ClaimEvaluator)

```kotlin
object ClaimEvaluator {

    fun evaluate(
        state: GameState,
        declaration: ClaimDeclaration
    ): ClaimResult {
        var correct = true

        for ((playerId, declaredCards) in declaration.cardAssignments) {
            val player = state.players.find { it.id == playerId } ?: continue
            for (card in declaredCards) {
                if (!player.hand.contains(card)) {
                    correct = false
                    break
                }
            }
            if (!correct) break
        }

        val claimer = state.players.find { it.id == declaration.claimerId }!!
        val pointsToTeamId = if (correct) claimer.teamId
            else state.teams.first { it.id != claimer.teamId }.id

        return ClaimResult(
            halfSuit = declaration.halfSuit,
            success = correct,
            pointsToTeamId = pointsToTeamId
        )
    }
}

data class ClaimResult(
    val halfSuit: HalfSuit,
    val success: Boolean,
    val pointsToTeamId: String
)
```

---

## 5. Backend Architecture (Ktor)

### 5.1 Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/room/create` | Create a new room, returns roomCode |
| `POST` | `/room/join` | Join a room by code, returns playerId |
| `GET` | `/room/{code}/state` | Fetch current room/lobby state |
| `POST` | `/room/{code}/start` | Host starts the game |
| `WS` | `/game/{roomCode}/{playerId}` | WebSocket connection for live game |

### 5.2 WebSocket Message Types

All messages are JSON. Every message has a `type` field.

#### Client → Server

```json
// Ask a card
{ "type": "ASK", "targetId": "p2", "card": { "suit": "SPADES", "value": "NINE" } }

// Submit a claim
{
  "type": "CLAIM",
  "halfSuit": "SPADES_UPPER",
  "cardAssignments": {
    "p1": [{"suit":"SPADES","value":"NINE"},{"suit":"SPADES","value":"TEN"}],
    "p3": [{"suit":"SPADES","value":"JACK"},{"suit":"SPADES","value":"QUEEN"},
           {"suit":"SPADES","value":"KING"},{"suit":"SPADES","value":"ACE"}]
  }
}

// Player is ready in lobby
{ "type": "READY" }

// Manual team swap request (lobby only)
{ "type": "SWAP_TEAM", "playerId": "p2" }
```

#### Server → Client (Broadcast)

```json
// Full game state sync (sent on connect/reconnect and after every action)
{ "type": "STATE_SYNC", "state": { ...GameState } }

// Result of an ask
{ "type": "ASK_RESULT", "success": true, "card": {...}, "newActivePlayerId": "p1" }

// Result of a claim
{ "type": "CLAIM_RESULT", "halfSuit": "SPADES_UPPER", "success": true, "pointsToTeam": "teamA" }

// Turn changed
{ "type": "TURN_CHANGED", "activePlayerId": "p3", "reason": "card_not_found" }

// Game over
{ "type": "GAME_OVER", "winnerTeamId": "teamA", "finalScores": {"teamA": 5, "teamB": 3} }

// Error
{ "type": "ERROR", "code": "NOT_YOUR_TURN", "message": "It is not your turn" }
```

### 5.3 Room Management

- Room codes are **6 alphanumeric characters** (e.g., `XK92TL`), unique and randomly generated.
- A room supports **4, 6, or 8 players**. The host selects the mode when creating the room.
- **Public rooms**: Entered into a matchmaking queue. Matched when enough players join for the selected mode.
- **Private rooms**: Join via code only. Host waits in lobby until all slots are filled.
- If a player disconnects mid-game, the server waits **60 seconds** for reconnection.
- After 60 seconds, the slot is filled by a **Level 1 bot** seamlessly.
- The reconnecting player can rejoin at any time during the 60s window using their `playerId` (stored in session).

### 5.4 Team Assignment
- **Auto-assign**: On game start, players are randomly split into two equal teams.
- **Manual assign**: In lobby, host can drag/assign players between Team A and Team B before starting.
- Teams must be equal in size before the game can start.

---

## 6. Android App — Screen Inventory

### 6.1 Home Screen
- App logo + name
- **"Create Game"** button → Room setup dialog (mode: 4/6/8 players, public/private)
- **"Join Game"** button → Enter 6-digit room code
- **"Quick Match"** button → Public matchmaking queue for 6-player mode (default)
- Session nickname entry (persisted locally for session)

### 6.2 Lobby Screen
- Room code displayed prominently (shareable)
- Player slots with names and ready status
- Team A / Team B columns — drag to reassign (host only)
- **"Add Bot"** button to fill empty slots with bots
- **"Start Game"** button (host only, enabled when all slots filled and teams equal)
- **"Ready"** toggle for non-host players

### 6.3 Game Board Screen (Main Screen)
```
┌──────────────────────────────────────┐
│  Team A: 3 pts       Team B: 2 pts   │  ← Score bar
├──────────────────────────────────────┤
│  [♠↑][♠↓][♥↑][♥↓][♦↑][♦↓][♣↑][♣↓] │  ← Deck Tracker (8 tiles)
│   🟢  🟡  🔴  🟡  🟡  🟢  🔴  🟡   │  ← Status indicators
├──────────────────────────────────────┤
│  Opponents:  Ravi(5) Priya(3) Dev(4) │  ← Avatar + card count
│              [ACTIVE TURN INDICATOR] │
├──────────────────────────────────────┤
│  📋 Game Log ─────────────────── [▼] │
│  Priya asked Dev for 9♠ — No         │
│  You asked Ravi for K♥ — Got it!     │
├──────────────────────────────────────┤
│  Teammates: Arjun(6)  Meera(4)       │
├──────────────────────────────────────┤
│  YOUR HAND:                          │
│  [9♠][K♥][3♦][3♣][5♣][7♣][A♥][J♦] │  ← Scrollable card fan
├──────────────────────────────────────┤
│       [ASK CARD]    [CLAIM DECK]     │  ← Action buttons
└──────────────────────────────────────┘
```

**Deck Tracker Tile States:**
- 🟡 Yellow — In play, unclaimed
- 🟢 Green — Claimed by your team
- 🔴 Red — Claimed by opponent
- Tapping a tile shows a tooltip: which cards are known to be where (based on log)

### 6.4 Ask Flow
1. Player taps **"ASK CARD"**
2. Bottom sheet appears: select a half-suit (only half-suits in hand are enabled)
3. On selecting a half-suit: show the 6 cards, grayed out cards are ones already in player's hand or already claimed
4. Player selects the card they want
5. Player selects which opponent to ask (avatar list)
6. Confirmation: "Ask Ravi for K♠?" → **[CANCEL] [ASK]**

### 6.5 Claim Flow
1. Player taps **"CLAIM DECK"**
2. Bottom sheet: select which half-suit to claim (only half-suits where player holds ≥1 card)
3. Claim assignment screen appears:
   - Player's own cards: auto-assigned with a ✅ lock icon
   - Each remaining card: dropdown to assign to a teammate
4. Warning banner: *"⚠️ A wrong claim gives the point to the opponent"*
5. **[CANCEL] [CONFIRM CLAIM]** — confirm triggers a final alert dialog before submission

### 6.6 Game Log Screen (Full View)
- Expandable from the game board via the log panel
- Full scrollable history of all events
- Filter chips: **All | Asks | Claims | My Team | Me**
- Color coded: Green (success), Red (failure), Gray (neutral)

### 6.7 Result Screen
- Winner banner with team name
- Score: Team A X — Y Team B
- Deck-by-deck breakdown table (who claimed what, correctly or not)
- **[PLAY AGAIN]** (returns to lobby with same players) **[HOME]**

---

## 7. Bot AI Design

### Level 1 Bot — Log Tracker
The bot maintains a mental model of card locations using the game log, identical to what a human can observe.

**Decision logic (in priority order):**
1. If the bot **knows** an opponent has a card it needs (from log) → ask for it
2. If the bot's team has **5 of 6 cards** in a half-suit and knows the 6th location → ask for it, then claim
3. If the bot's team has **all 6 cards** confirmed → claim immediately on its turn
4. If none of the above → ask for a card in a half-suit it holds, targeting the most likely opponent (based on log inference)

**Claim logic:**
- Bot will only claim when it has **100% certainty** of all 6 card locations (from log tracking)
- Bot auto-fills the declaration correctly

**Turn timing:**
- Bot waits **1.5–2.5 seconds** (randomized) before acting, to feel human

### Level 2 Bot (Future)
- Probabilistic card location inference (e.g., "Ravi has not denied having 9♠, so he probably has it")
- Strategic turn passing (ask a card it expects to not get, to pass turn to a specific opponent)
- Team coordination awareness

---

## 8. Session Management

- On joining/creating a room, the server generates a **`playerId`** (UUID) and **`sessionToken`**
- Both are stored in **Android DataStore** (local, persists for app session)
- On WebSocket reconnect, the client sends `{ "type": "RECONNECT", "playerId": "...", "sessionToken": "..." }`
- Server restores the player's hand and game state
- No login, no accounts — purely session-based

---

## 9. Project Structure (KMP)

```
literature-app/
├── shared/                          # KMP shared module
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── model/
│       │   │   ├── Card.kt
│       │   │   ├── Player.kt
│       │   │   ├── Team.kt
│       │   │   ├── GameState.kt
│       │   │   ├── GameEvent.kt
│       │   │   └── HalfSuit.kt
│       │   ├── logic/
│       │   │   ├── MoveValidator.kt
│       │   │   ├── ClaimEvaluator.kt
│       │   │   ├── GameEngine.kt
│       │   │   ├── CardDealer.kt
│       │   │   └── BotStrategy.kt
│       │   └── network/
│       │       └── WebSocketMessages.kt  # Serializable message data classes
│       └── androidMain/kotlin/      # Android-specific implementations if any
│
├── androidApp/                      # Android app module
│   └── src/main/kotlin/
│       ├── ui/
│       │   ├── home/
│       │   ├── lobby/
│       │   ├── game/
│       │   │   ├── GameBoardScreen.kt
│       │   │   ├── AskCardSheet.kt
│       │   │   ├── ClaimDeckSheet.kt
│       │   │   ├── DeckTracker.kt
│       │   │   ├── GameLogPanel.kt
│       │   │   └── CardHandView.kt
│       │   └── result/
│       ├── viewmodel/
│       │   ├── GameViewModel.kt
│       │   └── LobbyViewModel.kt
│       └── network/
│           └── WebSocketClient.kt
│
└── server/                          # Ktor backend
    └── src/main/kotlin/
        ├── Application.kt
        ├── room/
        │   ├── RoomManager.kt
        │   └── RoomRoutes.kt
        ├── game/
        │   ├── GameEngine.kt
        │   ├── GameSession.kt
        │   └── ClaimAdjudicator.kt
        ├── websocket/
        │   └── WebSocketHandler.kt
        └── bot/
            └── BotManager.kt
```

---

## 10. Key Edge Cases to Handle

| Scenario | Expected Behavior |
|----------|-------------------|
| Player asks for a card in a claimed half-suit | Blocked by validator — ASK button should not show claimed half-suits |
| All 3 teammates run out of cards before all decks claimed | Game ends early, remaining unclaimed decks go to opponent |
| Claim made when teammate has 0 cards | Valid — just assign 0 cards to that teammate |
| Player disconnects on their turn | Turn is paused for 60s, then bot takes over |
| Two players try to claim the same deck simultaneously | Server processes first-received message only; second gets an error |
| 4-4 tie | Result screen shows "It's a Draw!" |
| Bot assigned to a disconnected player's slot | Bot has full access to that player's hand and log |
| Host disconnects in lobby | Next player in join order becomes host |
| Host disconnects in game | No host needed in-game; server manages everything |

---

## 11. Tech Stack Summary

| Concern | Technology |
|---------|-----------|
| Mobile (Android) | Kotlin + Jetpack Compose |
| Shared Logic | Kotlin Multiplatform (KMP) |
| Future iOS/Web | Compose Multiplatform |
| Backend | Ktor (Kotlin) |
| Real-time | WebSockets (Ktor) |
| Game State Cache | Redis |
| Persistence (future) | PostgreSQL |
| Session Storage (client) | Android DataStore |
| Serialization | kotlinx.serialization |
| DI | Koin |
| Async | Kotlin Coroutines + Flow |

---

## 12. Non-Functional Requirements

- **Latency**: Every game action should reflect on all clients within **500ms** under normal network conditions
- **Reconnection**: Player state must be fully restorable within the 60-second window
- **Concurrency**: Server must support multiple simultaneous game rooms without state bleed
- **Offline resilience**: If WebSocket drops briefly, client should queue the last action and retry on reconnect
- **Cheating prevention**: All validation happens **server-side only**. Client sends intent; server decides outcome.

---

*Document version: 1.0 | Game: Literature | Stack: KMP + Ktor + Jetpack Compose*
