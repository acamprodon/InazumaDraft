package com.inazumadraft

import android.app.Application
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.TecnicaRepository

class InazumaDraftApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayerRepository.initialize(this)
        TecnicaRepository.initialize(this)

    }
}