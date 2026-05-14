package com.drawchat.app.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drawchat.app.data.model.CanvasPoint
import com.drawchat.app.data.model.Chat
import com.drawchat.app.data.model.Message
import com.drawchat.app.data.remote.SocketManager
import com.drawchat.app.data.repository.AuthRepository
import com.drawchat.app.data.repository.ChatRepository
import com.drawchat.app.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()
    private val storageRepository = StorageRepository()
    private val socketManager = SocketManager()

    // Список чатов
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    // Сообщения текущего чата
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Текущий чат
    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId

    // Загрузка
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Ошибка
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Подключаемся к серверу рисования
    init {
        socketManager.connect()
    }

    // Загрузить список чатов
    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getPublicChats().collect { chatList ->
                _chats.value = chatList
                _isLoading.value = false
            }
        }
    }

    // Загрузить сообщения чата
    fun loadMessages(chatId: String) {
        _currentChatId.value = chatId
        socketManager.joinRoom(chatId)
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messageList ->
                _messages.value = messageList.reversed()
            }
        }
    }

    // Отправить текстовое сообщение
    fun sendMessage(text: String) {
        val chatId = _currentChatId.value ?: return
        val userId = authRepository.getCurrentUserId() ?: return

        val message = Message(
            senderId = userId,
            senderName = authRepository.getCurrentUserEmail() ?: "User",
            text = text
        )

        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, message)
            result.onFailure { error ->
                _error.value = error.message
            }
        }
    }

    // Отправить изображение
    fun sendImage(uri: Uri) {
        val chatId = _currentChatId.value ?: return
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val uploadResult = storageRepository.uploadImage(uri)
            uploadResult.fold(
                onSuccess = { imageUrl ->
                    val message = Message(
                        senderId = userId,
                        senderName = authRepository.getCurrentUserEmail() ?: "User",
                        imageUrl = imageUrl
                    )
                    chatRepository.sendMessage(chatId, message)
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    // Создать новый чат
    fun createChat(name: String, isPublic: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.createChat(name, isPublic)
            result.fold(
                onSuccess = { chatId ->
                    loadChats()
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    // Отправить событие рисования
    fun sendDrawEvent(point: CanvasPoint) {
        socketManager.sendDrawEvent(point)
    }

    // Очистить холст
    fun clearCanvas() {
        socketManager.sendClearCanvas()
    }

    // Очистить ошибку
    fun clearError() {
        _error.value = null
    }

    // Отключиться при уничтожении
    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}