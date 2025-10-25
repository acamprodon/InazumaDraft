package com.inazumadraft

import android.app.Application
import com.inazumadraft.data.PlayerRepository

class InazumaDraftApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayerRepository.initialize(this)
    }
}