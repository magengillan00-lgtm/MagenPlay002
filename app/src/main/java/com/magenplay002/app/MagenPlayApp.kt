package com.magenplay002.app

import android.app.Application
import android.os.Environment

class MagenPlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MagenPlayApp
            private set

        fun getOutputDirectory(): String {
            return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
            ).absolutePath + "/MagenPlay"
        }

        fun getAudioOutputDirectory(): String {
            return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
            ).absolutePath + "/MagenPlay"
        }
    }
}
