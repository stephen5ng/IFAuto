package com.example.ifauto

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class MyCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        Log.d("IFAutoService", "onCreateSession: STARTING SESSION")
        return GameSession()
    }
}

class GameSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        Log.d("IFAutoService", "onCreateScreen: CREATING GAMESCREEN")
        return GameScreen(carContext)
    }
}
