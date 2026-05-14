package com.drawchat.app.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drawchat.app.data.repository.AuthRepository
import com.drawchat.app.data.repository.StorageRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val storageRepository = StorageRepository()
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

                // Пытаемся загрузить из Firestore
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    _userName.value = doc.getString("name") ?: email.substringBefore("@")
                    _avatarUrl.value = doc.getString("avatarUrl") ?: ""
                } else {
                    // Создаем запись пользователя
                    val userData = hashMapOf(
                        "name" to email.substringBefore("@"),
                        "email" to email,
                        "avatarUrl" to "",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(userId).set(userData).await()
                    _userName.value = email.substringBefore("@")
                }
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки профиля"
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
                    .update("name", name)
                    .await()
                _userName.value = name
                _message.value = "Имя обновлено!"
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = storageRepository.uploadImage(uri)
            result.fold(
                onSuccess = { url ->
                    val userId = authRepository.getCurrentUserId() ?: return@launch
                    db.collection("users").document(userId)
                        .update("avatarUrl", url)
                        .await()
                    _avatarUrl.value = url
                    _message.value = "Фото обновлено!"
                },
                onFailure = { error ->
                    _message.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun clearMessage() {
        _message.value = null
    }
}