package com.drawchat.app.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val isOnline: Boolean = false
)