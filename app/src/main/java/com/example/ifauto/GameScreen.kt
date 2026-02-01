package com.example.ifauto

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val TAG = "IFAutoGameScreen"

    init {
        lifecycle.addObserver(this)
        Log.d(TAG, "Initializing GameScreen with Zork Engine")
        
        tts = TextToSpeech(carContext.applicationContext, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { Log.d(TAG, "TTS Started") }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS Done")
                if (utteranceId == "GameText") {
                    startListening()
                }
            }
            override fun onError(utteranceId: String?) { Log.e(TAG, "TTS Error") }
        })

        try {
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
            Log.e(TAG, "Engine error", e)
        }
        
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        carContext.mainExecutor.execute {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(carContext)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d(TAG, "Mic Ready") }
                override fun onBeginningOfSpeech() { Log.d(TAG, "User started speaking") }
                override fun onRmsChanged(rmsdB: Float) {
                    // This logs the volume level. If it stays around -2.0, it's not hearing you.
                    if (rmsdB > 0) Log.d(TAG, "Mic volume: $rmsdB")
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d(TAG, "User stopped speaking")
                    isListening = false
                    invalidate()
                }
                override fun onError(error: Int) {
                    Log.e(TAG, "Mic Error: $error")
                    isListening = false
                    invalidate()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        Log.d(TAG, "Voice Command: ${matches[0]}")
                        processCommand(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        carContext.mainExecutor.execute {
            Log.d(TAG, "Mic activated")
            speechRecognizer?.startListening(intent)
            isListening = true
            invalidate()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speak(currentText)
        }
    }

    private fun speak(text: String) {
        val audioManager = carContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .build()
            
        audioManager.requestAudioFocus(focusRequest)
        tts?.setAudioAttributes(audioAttributes)
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "GameText")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "GameText")
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
        val listBuilder = ItemList.Builder()
        listBuilder.addItem(Row.Builder().setTitle("Story").addText(currentText).build())
        
        listOf("North", "South", "East", "West", "Look", "Inventory").forEach { cmd ->
            listBuilder.addItem(Row.Builder().setTitle(cmd).setOnClickListener { processCommand(cmd) }.build())
        }
        
        val actionStrip = ActionStrip.Builder()
            .addAction(Action.Builder().setTitle("Listen").setOnClickListener { startListening() }.build())
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle(if (isListening) "Listening..." else "Zork on Auto")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
