package com.example

import android.app.Application
import com.example.ads.StartIoAdManager
import com.example.audio.SoundEngine
import com.example.data.GameRepository
import com.example.data.local.BubbleBlitzDatabase

class BubbleBlitzApp : Application() {
    val database by lazy { BubbleBlitzDatabase.getInstance(this) }
    val repository by lazy { GameRepository(database.dao()) }
    val soundEngine by lazy { SoundEngine() }
    val adManager by lazy { StartIoAdManager() }

    override fun onCreate() {
        super.onCreate()
        adManager.initialize(this)
    }
}
