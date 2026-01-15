package com.example.twentyone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* =========================
   LOBBY (MAIN MENU) UI
   ========================= */

@Composable
fun LobbyScreen(
    title: String,
    statusText: String,
    rooms: List<RoomInfo>,
    onRefresh: () -> Unit,
    onJoinRoom: (roomId: Int) -> Unit,
    onCreateRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(statusText)
        Spacer(Modifier.height(16.dp))

        Text("Available Rooms", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onRefresh) { Text("Refresh") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onCreateRoom) { Text("Create Room") }
        }

        Spacer(Modifier.height(16.dp))

        val waitingRooms = rooms.filter { it.state == "waiting" }
        if (waitingRooms.isEmpty()) {
            Text("No rooms yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                waitingRooms.forEach { room ->
                    RoomRow(room = room, onJoin = { onJoinRoom(room.roomId) })
                }
            }
        }
    }
}

@Composable
private fun RoomRow(room: RoomInfo, onJoin: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Room #${room.roomId}", fontWeight = FontWeight.Bold)
            Text("Players: ${room.players.joinToString()}")
            Text("Mode: ${room.mode}")
        }
        Button(onClick = onJoin) { Text("Join") }
    }
}

/* =========================
   IN-GAME UI
   ========================= */

@Composable
fun GameScreen(
    title: String,
    statusText: String,
    yourHealth: Int,
    opponentHealth: Int,
    currentRound: Int,
    currentDamage: Int,
    opponentValueOrHidden: String, // bleibt drin, aber wir zeigen unsere eigene Anzeige
    yourValue: Int,                // bleibt drin, aber wir zeigen unsere eigene Anzeige
    yourHand: List<CardModel>,
    opponentHand: List<CardModel>,
    isYourTurn: Boolean,
    gameOver: Boolean,
    winnerText: String?,
    canRematch: Boolean,
    disconnectCountdown: Int?,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onRematch: () -> Unit
) {
    // Summen aus den Händen berechnen (nur Zahlenkarten)
    val oppRevealed = revealedSum(opponentHand)   // ohne erste Karte
    val oppHidden = hiddenValue(opponentHand)     // nur erste Karte

    val youRevealed = revealedSum(yourHand)
    val youHidden = hiddenValue(yourHand)

    Box(modifier = Modifier.fillMaxSize()) {

        // Background image (muss existieren: res/drawable/table1.png oder .webp)
        Image(
            painter = painterResource(R.drawable.table1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(statusText, color = Color.White)
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Health: You $yourHealth | Opponent $opponentHealth | Round $currentRound | Damage $currentDamage",
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            // Gegner-Anzeige:
            // - während Runde: "?? + revealedSum"
            // - am Ende: komplette Summe
            val opponentLine = if (!gameOver) {
                "Opponent: ?? + $oppRevealed"
            } else {
                "Opponent: ${oppRevealed + oppHidden}"
            }

            Text(text = opponentLine, color = Color.White)

            if (gameOver) {
                Text(
                    text = "(revealed $oppRevealed + hidden $oppHidden)",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Gegner-Hand: erste Karte verdeckt, am Ende aufdecken
            HandWithHiddenFirstCard(
                cards = opponentHand,
                revealAll = gameOver,
                showHiddenRankUnderFirst = false
            )

            Spacer(modifier = Modifier.weight(1f))

            // Deine Anzeige:
            // du kennst deine verdeckte Karte, also kannst du immer die volle Summe anzeigen
            val youLine = if (!gameOver) {
                "You: ${youRevealed + youHidden}"
            } else {
                "You: ${youRevealed + youHidden}"
            }

            Text(text = youLine, color = Color.White)

            if (gameOver) {
                Text(
                    text = "(revealed $youRevealed + hidden $youHidden)",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Deine Hand:
            // - erste Karte verdeckt, aber Zahl darunter als Text (nur während Runde)
            // - am Ende: aufdecken (dann kein "Text darunter" nötig)
            HandWithHiddenFirstCard(
                cards = yourHand,
                revealAll = gameOver,
                showHiddenRankUnderFirst = !gameOver
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onHit, enabled = isYourTurn) { Text("Hit") }
                Button(onClick = onStand, enabled = isYourTurn) { Text("Stand") }
                if (canRematch) Button(onClick = onRematch) { Text("Rematch") }
            }

            winnerText?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (it.contains("Win")) Color(0xFF2E7D32) else Color.Red
                )
            }

            disconnectCountdown?.let {
                Spacer(Modifier.height(12.dp))
                Text("Auto-disconnect in $it seconds", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

/* =========================
   HAND + CARDS UI
   ========================= */

@Composable
private fun HandWithHiddenFirstCard(
    cards: List<CardModel>,
    revealAll: Boolean,
    showHiddenRankUnderFirst: Boolean
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(cards) { index, card ->
            if (index == 0 && !revealAll) {
                // erste Karte verdeckt
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardBack()

                    // nur bei dir: Zahl unter der verdeckten Karte anzeigen
                    if (showHiddenRankUnderFirst) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = card.rank,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                PlayingCard(card)
            }
        }
    }
}

@Composable
private fun PlayingCard(card: CardModel) {
    val imageRes = cardImageForRank(card.rank)

    Box(
        modifier = Modifier
            .size(65.dp, 85.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = "Card ${card.rank}",
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        )
    }
}

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .size(65.dp, 85.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.cardbacksidewhite),
            contentDescription = "Card back",
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        )
    }
}

/* =========================
   RANK/VALUE HELPERS (nur Zahlen)
   ========================= */

private fun rankToValue(rank: String): Int = rank.toIntOrNull() ?: 0

// Summe der aufgedeckten Karten = alle außer die erste
private fun revealedSum(hand: List<CardModel>): Int =
    hand.drop(1).sumOf { rankToValue(it.rank) }

// Wert der verdeckten Karte = erste Karte
private fun hiddenValue(hand: List<CardModel>): Int =
    hand.firstOrNull()?.let { rankToValue(it.rank) } ?: 0

/* =========================
   IMAGE MAPPING (nur Zahlen)
   ========================= */

private fun cardImageForRank(rank: String): Int {
    return when (rank) {
        "1" -> R.drawable.card1
        "2" -> R.drawable.card2
        "3" -> R.drawable.card3
        "4" -> R.drawable.card4
        "5" -> R.drawable.card5
        "6" -> R.drawable.card6
        "7" -> R.drawable.card7
        "8" -> R.drawable.card8
        "9" -> R.drawable.card9
        "10" -> R.drawable.card10
        "11" -> R.drawable.card11
        else -> R.drawable.cardbacksidewhite
    }
}

/* =========================
   PREVIEWS
   ========================= */

@Preview(showBackground = true)
@Composable
fun LobbyScreenPreview() {
    MaterialTheme {
        LobbyScreen(
            title = "♠ Twenty-One",
            statusText = "Lobby ready",
            rooms = listOf(
                RoomInfo(1, listOf("Player 1"), "waiting", "classic"),
                RoomInfo(2, listOf("Player 2"), "waiting", "classic")
            ),
            onRefresh = {},
            onJoinRoom = {},
            onCreateRoom = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MaterialTheme {
        GameScreen(
            title = "♠ Twenty-One",
            statusText = "Your turn",
            yourHealth = 7,
            opponentHealth = 6,
            currentRound = 2,
            currentDamage = 1,
            opponentValueOrHidden = "??",
            yourValue = 18,
            yourHand = listOf(CardModel("1"), CardModel("7"), CardModel("4")),
            opponentHand = listOf(CardModel("2"), CardModel("5"), CardModel("9")),
            isYourTurn = true,
            gameOver = false, // setze true um Reveal zu sehen
            winnerText = null,
            canRematch = false,
            disconnectCountdown = null,
            onHit = {},
            onStand = {},
            onRematch = {}
        )
    }
}
