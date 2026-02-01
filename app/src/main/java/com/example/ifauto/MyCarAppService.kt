package com.example.ifauto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class MyCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // In production, you should validate against known hosts (e.g. Google's signatures)
        // For development/playground, allowing all is easier.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return GameSession()
    }
}

class GameSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GameScreen(carContext)
    }
}
