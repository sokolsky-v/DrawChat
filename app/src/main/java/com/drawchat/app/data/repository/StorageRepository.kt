package com.drawchat.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val imagesRef = storage.reference.child("chat_images")

    // Загрузить изображение в Storage
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val imageName = "${UUID.randomUUID()}.jpg"
            val imageRef = imagesRef.child(imageName)

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка загрузки фото: ${e.message}"))
        }
    }

    // Удалить изображение
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка удаления фото: ${e.message}"))
        }
    }
}