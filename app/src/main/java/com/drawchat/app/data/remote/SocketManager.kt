package com.drawchat.app.data.remote

import android.util.Log
import com.drawchat.app.data.model.CanvasPoint
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

class SocketManager {

    private var socket: Socket? = null

    // Потоки для получения событий рисования
    private val _drawEvents = MutableSharedFlow<CanvasPoint>()
    val drawEvents: SharedFlow<CanvasPoint> = _drawEvents

    private val _clearEvents = MutableSharedFlow<Unit>()
    val clearEvents: SharedFlow<Unit> = _clearEvents

    // Подключение к серверу
    fun connect(serverUrl: String = "http://10.0.2.2:3000") {
        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                timeout = 10000
            }

            socket = IO.socket(serverUrl, options)

            // Слушаем событие рисования
            socket?.on("draw_event") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val point = CanvasPoint(
                            x = data.optDouble("x", 0.0).toFloat(),
                            y = data.optDouble("y", 0.0).toFloat(),
                            color = data.optString("color", "#000000"),
                            strokeWidth = data.optDouble("strokeWidth", 5.0).toFloat()
                        )
                        _drawEvents.tryEmit(point)
                    }
                } catch (e: Exception) {
                    Log.e("SocketManager", "Error parsing draw_event: ${e.message}")
                }
            }

            // Слушаем очистку холста
            socket?.on("clear_canvas") {
                _clearEvents.tryEmit(Unit)
            }

            // Подключение установлено
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Connected to server")
            }

            // Отключение
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketManager", "Disconnected from server")
            }

            // Ошибка подключения
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketManager", "Connection error: ${args.firstOrNull()}")
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketManager", "Failed to create socket: ${e.message}")
        }
    }

    // Отправить событие рисования
    fun sendDrawEvent(point: CanvasPoint) {
        try {
            val json = JSONObject().apply {
                put("x", point.x.toDouble())
                put("y", point.y.toDouble())
                put("color", point.color)
                put("strokeWidth", point.strokeWidth.toDouble())
            }
            socket?.emit("draw_event", json)
        } catch (e: Exception) {
            Log.e("SocketManager", "Error sending draw event: ${e.message}")
        }
    }

    // Отправить очистку холста
    fun sendClearCanvas() {
        socket?.emit("clear_canvas")
    }

    // Присоединиться к комнате чата
    fun joinRoom(roomId: String) {
        socket?.emit("join_room", roomId)
    }

    // Отключиться
    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    // Проверить подключение
    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }
}