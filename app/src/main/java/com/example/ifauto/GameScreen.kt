package com.example.ifauto

import android.speech.tts.TextToSpeech
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.Locale

class GameScreen(carContext: CarContext) : Screen(carContext), TextToSpeech.OnInitListener, DefaultLifecycleObserver {
    private var tts: TextToSpeech? = null
    private var zEngine: ZEngineWrapper? = null
    private var currentText = "Loading Zork..."

    init {
        lifecycle.addObserver(this)
        tts = TextToSpeech(carContext, this)
        try {
            // Using zork1.z3 as it's been verified to work
            val stream = carContext.assets.open("zork1.z3")
            zEngine = ZEngineWrapper(stream)
            if (zEngine?.isRunning() == true) {
                currentText = zEngine?.run() ?: "Error running Z-Engine."
                if (currentText.isBlank()) currentText = "Zork loaded. What now?"
            } else {
                currentText = "Error: Z-Engine failed to initialize."
            }
        } catch (e: Exception) {
            currentText = "Error loading Z-Machine: ${e.message}"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speak(currentText)
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GameText")
    }

    private fun processCommand(command: String) {
        if (zEngine != null) {
            zEngine?.input(command)
            val output = zEngine?.run() ?: ""
            currentText = if (output.isNotBlank()) output else "OK."
            invalidate()
            speak(currentText)
        }
    }

    override fun onGetTemplate(): Template {
        // Z-Machine UI for Android Auto
        val listBuilder = ItemList.Builder()
        
        // Common commands as shortcuts
        listOf("Look", "Inventory", "North", "South", "East", "West", "Up", "Down", "Take all", "Drop all").forEach { cmd ->
            listBuilder.addItem(
                Row.Builder().setTitle(cmd).setOnClickListener { processCommand(cmd) }.build()
            )
        }
        
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Zork on Auto")
            .build()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        tts?.stop()
        tts?.shutdown()
    }
}
