package com.drawchat.app.data.model

data class Chat(
    val id: String = "",
    val name: String = "",
    val isPublic: Boolean = true,
    val participantsCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
)