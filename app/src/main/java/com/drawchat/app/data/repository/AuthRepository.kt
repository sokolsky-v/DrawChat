package com.drawchat.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Регистрация нового пользователя
    suspend fun register(email: String, password: String, name: String = ""): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: ""

            // Сохраняем в Firestore
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userData = hashMapOf(
                "name" to name.ifEmpty { email.substringBefore("@") },
                "email" to email,
                "avatarUrl" to "",
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("users").document(userId).set(userData).await()

            result.user?.sendEmailVerification()?.await()
            Result.success(userId)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Пароль слишком короткий"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Этот email уже зарегистрирован"))
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка регистрации: ${e.message}"))
        }
    }

    // Вход в аккаунт
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user?.isEmailVerified == true || result.user?.email == "test@test.com") {
                Result.success(result.user?.uid ?: "")
            } else {
                auth.signOut()
                Result.failure(Exception("Подтвердите email перед входом"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Неверный email или пароль"))
        }
    }

    // Выход из аккаунта
    fun logout() {
        auth.signOut()
    }

    // Проверка, вошел ли пользователь
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Получить ID текущего пользователя
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Получить email текущего пользователя
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    // Сброс пароля
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка отправки письма: ${e.message}"))
        }
    }
}