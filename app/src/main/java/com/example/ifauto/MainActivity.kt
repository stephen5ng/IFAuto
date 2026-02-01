package com.example.ifauto

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var outputTextView: TextView
    private lateinit var commandEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var scrollView: ScrollView
    private var zEngine: ZEngineWrapper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputTextView = findViewById(R.id.outputTextView)
        commandEditText = findViewById(R.id.commandEditText)
        sendButton = findViewById(R.id.sendButton)
        scrollView = findViewById(R.id.scrollView)

        inspectAssets()
        initZEngine()

        sendButton.setOnClickListener {
            handleCommand()
        }

        commandEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleCommand()
                true
            } else {
                false
            }
        }
    }

    private fun inspectAssets() {
        try {
            val assetsList = assets.list("")
            assetsList?.forEach { name ->
                try {
                    val stream = assets.open(name)
                    val bytes = stream.readBytes()
                    Log.d("MainActivity", "Asset: $name, Size: ${bytes.size} bytes")
                    stream.close()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error reading asset $name", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error listing assets", e)
        }
    }

    private fun initZEngine() {
        try {
            // Using zork1.z3 as it was found to be valid (86838 bytes)
            val stream = assets.open("zork1.z3")
            zEngine = ZEngineWrapper(stream)
            if (zEngine?.isRunning() == true) {
                appendOutput(zEngine?.run() ?: "")
            } else {
                appendOutput("Error initializing Z-Engine. Check Logcat.")
            }
        } catch (e: Exception) {
            appendOutput("Error loading game: ${e.message}")
            Log.e("MainActivity", "Error loading game", e)
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
        }
    }

    private fun appendOutput(text: String) {
        if (text.isEmpty()) return
        outputTextView.append(text)
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
