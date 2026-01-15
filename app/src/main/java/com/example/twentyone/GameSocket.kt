import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

class GameSocket(
    private val onLog: (String) -> Unit,
    private val onEvent: (String, JSONObject) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(url: String = "wss://superawesomeblackjackgame-production.up.railway.app") {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onLog("Connected to server")
                requestRoomList() // request rooms right away
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onLog("Received raw message: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    onEvent(type, json)
                } catch (e: Exception) {
                    onLog("Failed parsing message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onLog("WebSocket error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onLog("Disconnected: $reason")
            }
        })
    }

    fun requestRoomList() {
        val msg = JSONObject().apply { put("type", "get_rooms") }
        webSocket?.send(msg.toString())
        onLog("Sent: get_rooms")
    }

    fun createRoom(name: String) {
        val msg = JSONObject().apply {
            put("type", "create_room")
            put("name", name)
        }
        webSocket?.send(msg.toString())
        onLog("Sent: create_room")
    }

    fun joinRoom(roomId: Int, name: String) {
        val msg = JSONObject().apply {
            put("type", "join_room")
            put("roomId", roomId)
            put("name", name)
        }
        webSocket?.send(msg.toString())
        onLog("Sent: join_room ($roomId)")
    }

    fun leaveRoom() {
        val msg = JSONObject().apply {
            put("type", "leave_room")
        }
        webSocket?.send(msg.toString())
    }

    fun hit() { webSocket?.send("""{"type":"hit"}"""); onLog("Sent: hit") }
    fun stand() { webSocket?.send("""{"type":"stand"}"""); onLog("Sent: stand") }
    fun rematch() { webSocket?.send("""{"type":"rematch"}"""); onLog("Sent: rematch") }
    fun disconnect() { webSocket?.close(1000, "Client closing"); onLog("Socket closed") }
}
