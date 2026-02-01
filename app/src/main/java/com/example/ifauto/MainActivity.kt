package com.example.ifauto

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var outputTextView: TextView
    private lateinit var commandEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var micButton: Button
    private lateinit var scrollView: ScrollView
    
    private var zEngine: ZEngineWrapper? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListenting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputTextView = findViewById(R.id.outputTextView)
        commandEditText = findViewById(R.id.commandEditText)
        sendButton = findViewById(R.id.sendButton)
        micButton = findViewById(R.id.micButton)
        scrollView = findViewById(R.id.scrollView)

        tts = TextToSpeech(this, this)
        initZEngine()
        checkPermissionsAndSetupMic()

        sendButton.setOnClickListener { handleCommand() }
        micButton.setOnClickListener { startListening() }

        commandEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleCommand()
                true
            } else {
                false
            }
        }
    }

    private fun checkPermissionsAndSetupMic() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
        } else {
            setupSpeechRecognizer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        runOnUiThread {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { 
                    micButton.text = "🔴" 
                    isListenting = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { 
                    micButton.text = "🎤" 
                    isListenting = false
                }
                override fun onError(error: Int) {
                    micButton.text = "🎤"
                    isListenting = false
                    Log.e("MainActivity", "Speech error: $error")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        commandEditText.setText(matches[0])
                        // Don't auto-send, let user confirm or just edit? 
                        // Actually for a game "North" should just send.
                        handleCommand()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        if (isListenting) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Start listening failed", e)
        }
    }

    private fun initZEngine() {
        try {
            val stream = assets.open("zork1.z3")
            zEngine = ZEngineWrapper(stream)
            if (zEngine?.isRunning() == true) {
                val intro = zEngine?.run() ?: ""
                appendOutput(intro)
                speak(intro)
            }
        } catch (e: Exception) {
            appendOutput("Error: ${e.message}")
        }
    }

    private fun handleCommand() {
        val command = commandEditText.text.toString().trim()
        if (command.isNotEmpty()) {
            appendOutput("\n> $command\n")
            commandEditText.text.clear()
            
            zEngine?.input(command)
            val output = zEngine?.run() ?: ""
            appendOutput(output)
            speak(output)
        }
    }

    private fun appendOutput(text: String) {
        outputTextView.append(text)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    // Auto-listen after speaking
                    if (utteranceId == "GameResponse") {
                        runOnUiThread { startListening() }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun speak(text: String) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "GameResponse")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "GameResponse")
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
