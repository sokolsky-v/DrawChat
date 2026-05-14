package com.drawchat.app.data.repository

import com.drawchat.app.data.model.Chat
import com.drawchat.app.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    // Получить список публичных чатов
    fun getPublicChats(): Flow<List<Chat>> = callbackFlow {
        val listener = db.collection("chats")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.map { doc ->
                    Chat(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        isPublic = doc.getBoolean("isPublic") ?: true,
                        participantsCount = doc.getLong("participantsCount")?.toInt() ?: 0,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageTime = doc.getLong("lastMessageTime") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    // Получить сообщения чата
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { doc ->
                    Message(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // Отправить сообщение
    suspend fun sendMessage(chatId: String, message: Message): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "imageUrl" to message.imageUrl,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(messageData)
                .await()

            // Обновляем последнее сообщение в чате
            db.collection("chats")
                .document(chatId)
                .update(
                    "lastMessage", message.text.ifEmpty { "Фото" },
                    "lastMessageTime", System.currentTimeMillis()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка отправки: ${e.message}"))
        }
    }

    // Создать новый чат
    suspend fun createChat(name: String, isPublic: Boolean): Result<String> {
        return try {
            val chatData = hashMapOf(
                "name" to name,
                "isPublic" to isPublic,
                "participantsCount" to 1,
                "lastMessage" to "Чат создан",
                "lastMessageTime" to System.currentTimeMillis(),
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = db.collection("chats")
                .add(chatData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка создания чата: ${e.message}"))
        }
    }
}