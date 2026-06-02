package com.drawchat.app.presentation.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drawchat.app.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl: StateFlow<String> = _avatarUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                val email = authRepository.getCurrentUserEmail() ?: ""
                _userEmail.value = email

                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    _userName.value = doc.getString("name") ?: email.substringBefore("@")
                    _avatarUrl.value = doc.getString("avatarBase64") ?: ""
                } else {
                    val defaultName = email.substringBefore("@")
                    db.collection("users").document(userId).set(
                        hashMapOf(
                            "name" to defaultName,
                            "email" to email,
                            "avatarBase64" to "",
                            "createdAt" to System.currentTimeMillis()
                        )
                    ).await()
                    _userName.value = defaultName
                }
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                db.collection("users").document(userId)
                    .update("name", name).await()
                _userName.value = name
                _message.value = "Имя обновлено!"
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val baos = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                db.collection("users").document(userId)
                    .update("avatarBase64", base64).await()
                _avatarUrl.value = base64
                _message.value = "Фото обновлено!"
            } catch (e: Exception) {
                _message.value = "Ошибка фото: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun logout() { authRepository.logout() }
    fun clearMessage() { _message.value = null }
}