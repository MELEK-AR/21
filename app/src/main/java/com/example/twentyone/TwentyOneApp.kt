package com.example.twentyone

import GameSocket
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/* =========================
   DATA MODELS
   ========================= */

data class CardModel(val rank: String)

data class RoomInfo(
    val roomId: Int,
    val players: List<String>,
    val state: String,
    val mode: String
)

/* =========================
   MAIN APP
   ========================= */

@Composable
fun TwentyOneApp() {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var playerId by remember { mutableStateOf<Int?>(null) }
    var currentTurnPlayerId by remember { mutableStateOf<Int?>(null) }

    var yourHand by remember { mutableStateOf<List<CardModel>>(emptyList()) }
    var opponentHand by remember { mutableStateOf<List<CardModel>>(emptyList()) }

    var yourValue by remember { mutableStateOf(0) }
    var opponentValue by remember { mutableStateOf(0) }

    var yourHealth by remember { mutableStateOf(7) }
    var opponentHealth by remember { mutableStateOf(7) }

    var currentRound by remember { mutableStateOf(1) }
    var currentDamage by remember { mutableStateOf(1) }

    var inGame by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var matchOver by remember { mutableStateOf(false) }

    var disconnectCountdown by remember { mutableStateOf<Int?>(null) }

    var winnerText by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Connecting…") }

    var rooms by remember { mutableStateOf<List<RoomInfo>>(emptyList()) }

    val socket = remember {
        GameSocket(
            onLog = { println(it) },
            onEvent = { type, data ->
                mainHandler.post {
                    handleSocketEvent(
                        type = type,
                        data = data,
                        playerId = playerId,
                        yourHand = yourHand,
                        opponentHand = opponentHand,
                        onSetPlayerId = { playerId = it },
                        onSetCurrentTurn = { currentTurnPlayerId = it },
                        onUpdateHands = { y, o ->
                            yourHand = y
                            opponentHand = o
                        },
                        onUpdateValues = { y, o ->
                            if (y != 0) yourValue = y
                            if (o != 0) opponentValue = o
                        },
                        onUpdateHealth = { y, o ->
                            yourHealth = y
                            opponentHealth = o
                        },
                        onUpdateRound = { currentRound = it },
                        onUpdateDamage = { currentDamage = it },
                        onSetInGame = { inGame = it },
                        onGameOver = {
                            winnerText = it.ifEmpty { null }
                            gameOver = it.isNotEmpty()
                        },
                        onStatus = { statusText = it },
                        onRoomsUpdate = { rooms = it },
                        onMatchOver = { matchOver = true }
                    )
                }
            }
        )
    }

    LaunchedEffect(Unit) { socket.connect() }

    val isYourTurn = playerId != null && playerId == currentTurnPlayerId

    LaunchedEffect(gameOver) {
        if (gameOver) {
            disconnectCountdown = 10

            while (disconnectCountdown!! > 0) {
                delay(1000)
                disconnectCountdown = disconnectCountdown!! - 1
            }

            socket.leaveRoom()

            inGame = false
            yourHand = emptyList()
            opponentHand = emptyList()
            yourValue = 0
            opponentValue = 0
            gameOver = false
            matchOver = false
            statusText = "Disconnected"
            disconnectCountdown = null
        } else {
            disconnectCountdown = null
        }
    }


    if (!inGame) {
        LobbyScreen(
            title = "♠ Twenty-One",
            statusText = statusText,
            rooms = rooms,
            onRefresh = { socket.requestRoomList() },
            onJoinRoom = { socket.joinRoom(it, "Player $playerId") },
            onCreateRoom = { socket.createRoom("Player $playerId") }
        )
    } else {
        GameScreen(
            title = "♠ Twenty-One",
            statusText = statusText,
            yourHealth = yourHealth,
            opponentHealth = opponentHealth,
            currentRound = currentRound,
            currentDamage = currentDamage,
            opponentValueOrHidden = if (gameOver) opponentValue.toString() else "??",
            opponentCardCount = opponentHand.size,
            yourValue = yourValue,
            yourHand = yourHand,
            isYourTurn = isYourTurn,
            gameOver = gameOver,
            winnerText = winnerText,
            canRematch = gameOver && (yourHealth <= 0 || opponentHealth <= 0),
            disconnectCountdown = disconnectCountdown,
            onHit = { socket.hit() },
            onStand = { socket.stand() },
            onRematch = {
                socket.rematch()
                yourHand = emptyList()
                opponentHand = emptyList()
                yourValue = 0
                opponentValue = 0
                gameOver = false
                matchOver = false
                disconnectCountdown = null
                inGame = true
            }
        )
    }
}

/* =========================
   HELPERS
   ========================= */

fun parseHand(arr: JSONArray): List<CardModel> =
    List(arr.length()) { CardModel(arr.getString(it)) }

fun readOpponentHand(data: JSONObject): List<CardModel> =
    when {
        data.has("opponentHand") -> parseHand(data.getJSONArray("opponentHand"))
        data.has("opponentCardHand") -> parseHand(data.getJSONArray("opponentCardHand"))
        else -> emptyList()
    }

/* =========================
   SOCKET HANDLER
   ========================= */

fun handleSocketEvent(
    type: String,
    data: JSONObject,
    playerId: Int?,
    yourHand: List<CardModel>,
    opponentHand: List<CardModel>,
    onSetPlayerId: (Int) -> Unit,
    onSetCurrentTurn: (Int) -> Unit,
    onUpdateHands: (List<CardModel>, List<CardModel>) -> Unit,
    onUpdateValues: (Int, Int) -> Unit,
    onUpdateHealth: (Int, Int) -> Unit,
    onUpdateRound: (Int) -> Unit,
    onUpdateDamage: (Int) -> Unit,
    onSetInGame: (Boolean) -> Unit,
    onGameOver: (String) -> Unit,
    onStatus: (String) -> Unit,
    onRoomsUpdate: (List<RoomInfo>) -> Unit,
    onMatchOver: () -> Unit
) {
    when (type) {
        "welcome" -> onSetPlayerId(data.getInt("playerId"))

        "room_list" -> {
            val arr = data.getJSONArray("rooms")
            onRoomsUpdate(
                List(arr.length()) {
                    val o = arr.getJSONObject(it)
                    RoomInfo(
                        o.getInt("roomId"),
                        List(o.getJSONArray("players").length()) { i ->
                            o.getJSONArray("players").getString(i)
                        },
                        o.getString("state"),
                        o.getString("mode")
                    )
                }
            )
        }

        "game_start", "round_start" -> {
            onSetInGame(true)

            val yh = parseHand(data.getJSONArray("yourHand"))
            val oh = readOpponentHand(data)

            onUpdateHands(yh, oh)
            onUpdateValues(data.getInt("yourValue"), data.optInt("opponentValue", 0))

            data.optJSONObject("health")?.let {
                onUpdateHealth(it.getInt("you"), it.getInt("opponent"))
            }

            onUpdateRound(data.optInt("round", 1))
            onUpdateDamage(data.optInt("damage", 1))

            val turn = data.getInt("currentTurnPlayerId")
            onSetCurrentTurn(turn)
            onStatus(if (turn == playerId) "Your turn" else "Opponent's turn")
            onGameOver("")
        }

        "hit_result" -> {
            val pid = data.getInt("playerId")
            val card = CardModel(data.get("card").toString())
            val value = data.getInt("newValue")

            if (pid == playerId) {
                onUpdateHands(yourHand + card, opponentHand)
                onUpdateValues(value, 0)
            } else {
                onUpdateHands(yourHand, opponentHand + card)
                onUpdateValues(0, value)
            }
        }

        "turn_change" -> {
            val turn = data.getInt("currentTurnPlayerId")
            onSetCurrentTurn(turn)
            onStatus(if (turn == playerId) "Your turn" else "Opponent's turn")
        }

        "round_end" -> {
            val h = data.getJSONObject("health")
            onUpdateHealth(h.getInt("you"), h.getInt("opponent"))

            val winnerId = data.optInt("winnerId", -1)
            onGameOver(
                when {
                    winnerId == -1 -> "Draw"
                    winnerId == playerId -> "You Win"
                    else -> "You Lose"
                }
            )
        }

        "game_over" -> {
            onMatchOver()
            onStatus("Game over – rematch?")
        }
    }
}