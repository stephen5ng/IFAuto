package com.example.ifauto

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var statusMessage = "Ready"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val TAG = "IFAutoGameScreen"
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        lifecycle.addObserver(this)
        Log.d(TAG, "Initializing GameScreen")
        
        tts = TextToSpeech(carContext.applicationContext, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { 
                Log.d(TAG, "TTS Started")
                statusMessage = "Speaking..."
                invalidate()
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS Done")
                statusMessage = "Ready"
                if (utteranceId == "GameText") {
                    // Give the user a moment to digest before listening starts
                    mainHandler.postDelayed({ startListening() }, 800)
                }
                invalidate()
            }
            override fun onError(utteranceId: String?) { 
                Log.e(TAG, "TTS Error")
                statusMessage = "TTS Error"
                invalidate()
            }
        })

        try {
            val stream = carContext.assets.open("zork1.z3")
            zEngine = ZEngineWrapper(stream)
            if (zEngine?.isRunning() == true) {
                currentText = zEngine?.run() ?: "Error running Z-Engine."
            }
        } catch (e: Exception) {
            currentText = "Error: ${e.message}"
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (carContext.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
            Log.d(TAG, "Requesting Mic Permission")
            carContext.requestPermissions(
                listOf(android.Manifest.permission.RECORD_AUDIO)
            ) { granted, rejected ->
                if (granted.contains(android.Manifest.permission.RECORD_AUDIO)) {
                    Log.d(TAG, "Mic Permission Granted")
                    setupSpeechRecognizer()
                } else {
                    statusMessage = "Mic Perm Denied"
                    currentText += "\n(Microphone permission is required to play)"
                    invalidate()
                }
            }
        } else {
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        Log.d(TAG, "setupSpeechRecognizer called")
        carContext.mainExecutor.execute {
            if (speechRecognizer != null) {
                Log.d(TAG, "SpeechRecognizer already initialized")
                return@execute
            }
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(carContext)
                if (speechRecognizer == null) {
                    Log.e(TAG, "SpeechRecognizer creation returned NULL")
                    statusMessage = "No Speech Recog"
                    invalidate()
                    return@execute
                }
                
                Log.d(TAG, "SpeechRecognizer created successfully")
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { 
                        Log.d(TAG, "onReadyForSpeech: Mic is OPEN")
                        statusMessage = "Listening..."
                        isListening = true // Ensure state sync
                        invalidate()
                    }
                    override fun onBeginningOfSpeech() { Log.d(TAG, "onBeginningOfSpeech") }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        isListening = false
                        statusMessage = "Thinking..."
                        invalidate()
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = "Mic Error: $error"
                        Log.e(TAG, errorMsg)
                        statusMessage = errorMsg
                        // speak(errorMsg, isAutoListen = false) // Optional: feedback on error
                        invalidate()
                    }
                    override fun onResults(results: Bundle?) {
                        Log.d(TAG, "onResults")
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            processCommand(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error creating SpeechRecognizer", e)
            }
        }
    }

    private fun startListening() {
        Log.d(TAG, "startListening() called. isListening=$isListening, SR=$speechRecognizer")
        if (isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        
        carContext.mainExecutor.execute {
            try {
                if (speechRecognizer == null) {
                    Log.e(TAG, "Attempted to start listening but speechRecognizer is null")
                    setupSpeechRecognizer() // Try to recover
                    return@execute
                }
                Log.d(TAG, "Calling speechRecognizer.startListening")
                speechRecognizer?.startListening(intent)
                // We set isListening=true in onReadyForSpeech to confirm it actually started
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                statusMessage = "Mic Failed"
                invalidate()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speak(currentText)
        }
    }

    private fun speak(text: String, isAutoListen: Boolean = true) {
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
        
        val utteranceId = if (isAutoListen) "GameText" else "Feedback"
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun processCommand(command: String) {
        if (zEngine != null) {
            Log.d(TAG, "Voice Command: $command")
            zEngine?.input(command)
            val output = zEngine?.run() ?: ""
            currentText = if (output.isNotBlank()) output else "OK."
            speak(currentText)
        }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Story")
                .addText(currentText)
                .build()
        )
        
        listOf("North", "South", "East", "West", "Look", "Inventory").forEach { cmd ->
            listBuilder.addItem(
                Row.Builder().setTitle(cmd).setOnClickListener { processCommand(cmd) }.build()
            )
        }
        
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Listen")
                    .setOnClickListener { startListening() }
                    .build()
            )
            .build()

        val title = if (isListening) "Listening... 🎤" else "Zork ($statusMessage)"

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle(title)
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
