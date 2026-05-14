package com.drawchat.app

import android.app.Application
import com.google.firebase.FirebaseApp

class DrawChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация Firebase
        FirebaseApp.initializeApp(this)
    }
}