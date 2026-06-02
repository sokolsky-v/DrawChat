package com.drawchat.app.presentation.chat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drawchat.app.data.model.Chat
import com.drawchat.app.data.model.Message
import com.drawchat.app.data.repository.AuthRepository
import com.drawchat.app.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class CanvasState(val paths: List<PathData> = emptyList(), val currentPath: PathData? = null)
data class PathData(
    val color: Color,
    val strokeWidth: Float,
    val points: List<Offset> = emptyList(),
    val isEraser: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    val canvasPaths = mutableListOf<PathData>()
    var currentPathData: PathData? = null
    val loadedPaths = mutableListOf<PathData>()

    private var autoSaveJob: Job? = null

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                db.collection("chats").whereArrayContains("participants", userId).get().await()
                    .documents.map { doc ->
                        Chat(id = doc.id, name = doc.getString("name") ?: "", isPublic = true,
                            participantsCount = (doc.get("participants") as? List<*>)?.size ?: 0)
                    }.let { _chats.value = it }
            } catch (e: Exception) { _error.value = e.message }
            _isLoading.value = false
        }
    }

    fun loadMessages(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch { chatRepository.getMessages(chatId).collect { _messages.value = it.reversed() } }
        loadCanvasFromFirestore(chatId)
    }

    fun loadCanvasFromFirestore(chatId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                val jsonStr = doc.getString("canvasPaths") ?: ""
                loadedPaths.clear()
                if (jsonStr.isNotEmpty()) {
                    val arr = JSONArray(jsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val colorInt = obj.getLong("color")
                        val color = Color(colorInt.toULong())
                        val sw = obj.getDouble("strokeWidth").toFloat()
                        val isEraser = obj.optBoolean("isEraser", false)
                        val ptsArr = obj.getJSONArray("points")
                        val pts = mutableListOf<Offset>()
                        for (j in 0 until ptsArr.length()) {
                            val p = ptsArr.getJSONObject(j)
                            pts.add(Offset(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()))
                        }
                        loadedPaths.add(PathData(color = color, strokeWidth = sw, points = pts, isEraser = isEraser))
                    }
                }
            } catch (e: Exception) { loadedPaths.clear() }
        }
    }

    fun autoSaveCanvas() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            saveToFirestore()
        }
    }

    fun saveImmediately() {
        autoSaveJob?.cancel()
        viewModelScope.launch { saveToFirestore() }
    }

    private suspend fun saveToFirestore() {
        val chatId = _currentChatId.value ?: return
        try {
            val allPaths = getAllPaths()
            if (allPaths.isEmpty()) return
            val arr = JSONArray()
            allPaths.forEach { pd ->
                val obj = JSONObject()
                obj.put("color", pd.color.value.toLong())
                obj.put("strokeWidth", pd.strokeWidth.toDouble())
                obj.put("isEraser", pd.isEraser)
                val ptsArr = JSONArray()
                pd.points.forEach { p ->
                    val pObj = JSONObject()
                    pObj.put("x", p.x.toDouble())
                    pObj.put("y", p.y.toDouble())
                    ptsArr.put(pObj)
                }
                obj.put("points", ptsArr)
                arr.put(obj)
            }
            db.collection("chats").document(chatId).update("canvasPaths", arr.toString()).await()
        } catch (e: Exception) { }
    }

    private fun getAllPaths(): List<PathData> {
        val all = mutableListOf<PathData>()
        all.addAll(loadedPaths)
        all.addAll(canvasPaths)
        currentPathData?.let { all.add(it) }
        return all
    }

    fun clearCanvas() {
        canvasPaths.clear()
        loadedPaths.clear()
        currentPathData = null
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            try { db.collection("chats").document(chatId).update("canvasPaths", "").await() }
            catch (e: Exception) { }
        }
    }

    fun sendMessage(text: String) {
        val chatId = _currentChatId.value ?: return
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: return
        val userEmail = currentUser.email ?: "User"
        viewModelScope.launch {
            chatRepository.sendMessage(chatId, Message(senderId = userId,
                senderName = userEmail, text = text))
        }
    }

    fun createChat(name: String, isPublic: Boolean) {
        viewModelScope.launch { _isLoading.value = true; chatRepository.createChat(name, isPublic); loadChats() }
    }

    fun saveDrawing(context: Context) {
        val allPaths = getAllPaths()
        if (allPaths.isEmpty()) {
            _error.value = "Нарисуйте что-нибудь сначала!"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser
                val userId = currentUser?.uid ?: return@launch
                val userEmail = currentUser.email ?: "User"

                val bitmap = createBitmapFromPaths(allPaths, 800, 600)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                val artworkData = hashMapOf(
                    "userId" to userId,
                    "userEmail" to userEmail,
                    "imageBase64" to base64,
                    "title" to "Рисунок",
                    "authorName" to userEmail,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("artworks").add(artworkData).await()
                saveBitmapToGallery(context, bitmap)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = "Ошибка сохранения: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    private fun createBitmapFromPaths(paths: List<PathData>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint().apply {
            isAntiAlias = true; style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        paths.forEach { pd ->
            paint.color = if (pd.isEraser) android.graphics.Color.WHITE else pd.color.hashCode()
            paint.strokeWidth = if (pd.isEraser) pd.strokeWidth * 3 else pd.strokeWidth
            for (i in 1 until pd.points.size) {
                canvas.drawLine(pd.points[i-1].x, pd.points[i-1].y, pd.points[i].x, pd.points[i].y, paint)
            }
        }
        return bitmap
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val v = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "DrawChat_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DrawChat")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun clearError() { _error.value = null }
    fun resetSaveSuccess() { _saveSuccess.value = false }
}