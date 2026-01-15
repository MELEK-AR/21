package com.example.twentyone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalInspectionMode




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
                    RoomRow(
                        room = room,
                        onJoin = { onJoinRoom(room.roomId) }
                    )
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
    opponentValueOrHidden: String,
    opponentCardCount: Int,
    yourValue: Int,
    yourHand: List<CardModel>,
    isYourTurn: Boolean,
    gameOver: Boolean,
    winnerText: String?,
    canRematch: Boolean,
    disconnectCountdown: Int?,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onRematch: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Background image
        Image(
            painter = painterResource(R.drawable.table1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Foreground UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(statusText)
            Spacer(Modifier.height(16.dp))

            Text("Health: You $yourHealth | Opponent $opponentHealth | Round $currentRound | Damage $currentDamage")
            Spacer(Modifier.height(12.dp))

            Text("Opponent: $opponentValueOrHidden")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(opponentCardCount) { CardBack() }
            }

            // Replace this fixed spacer later:
            Spacer(Modifier.height(320.dp))

            Text("You ($yourValue)")
            CardRow(yourHand)

            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onHit, enabled = isYourTurn) { Text("Hit") }
                Button(onClick = onStand, enabled = isYourTurn) { Text("Stand") }
                if (canRematch) Button(onClick = onRematch) { Text("Rematch") }
            }

            winnerText?.let {
                Spacer(Modifier.height(20.dp))
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
   CARD UI
   ========================= */

@Composable
private fun CardRow(cards: List<CardModel>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cards) { card -> PlayingCard(card) }
    }
}

@Composable
private fun PlayingCard(card: CardModel) {
    val imageRes = cardImageForRank(card.rank)

    Box(
        modifier = Modifier
            .size(72.dp, 104.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Card ${card.rank}",
            modifier = Modifier.fillMaxSize().padding(6.dp)
        )
    }
}

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .size(72.dp, 104.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.cardbacksidewhite),
            contentDescription = "Card back",
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        )
    }
}

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

@Preview(showBackground = true)
@Composable
fun LobbyScreenPreview() {
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

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    GameScreen(
        title = "♠ Twenty-One",
        statusText = "Your turn",
        yourHealth = 7,
        opponentHealth = 6,
        currentRound = 2,
        currentDamage = 1,
        opponentValueOrHidden = "??",
        opponentCardCount = 3,
        yourValue = 18,
        yourHand = listOf(
            CardModel("1"),
            CardModel("7"),
            CardModel("4")
        ),
        isYourTurn = true,
        gameOver = false,
        winnerText = null,
        canRematch = false,
        disconnectCountdown = null,
        onHit = {},
        onStand = {},
        onRematch = {}
    )
}

