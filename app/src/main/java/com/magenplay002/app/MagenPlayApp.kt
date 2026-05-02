package com.magenplay002.app

import android.app.Application
import android.util.Log

class MagenPlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            _instance = this
        } catch (e: Exception) {
            Log.e("MagenPlayApp", "Error initializing app", e)
        }
    }

    companion object {
        @Volatile
        private var _instance: MagenPlayApp? = null

        val instance: MagenPlayApp
            get() = _instance ?: throw IllegalStateException("MagenPlayApp not initialized")
    }
}
