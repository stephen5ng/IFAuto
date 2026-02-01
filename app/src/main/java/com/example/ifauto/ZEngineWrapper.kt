package com.example.ifauto

import android.util.Log
import com.example.ifauto.zengine.*
import java.io.InputStream
import java.util.Arrays

class ZEngineWrapper(private val gameStream: InputStream) {
    private var machine: ZMachine? = null
    private var screen: ZScreen? = null
    private var status: ZStatus? = null
    private var memory: ByteArray? = null

    init {
        loadResult()
    }

    private fun loadResult() {
        try {
            val bytes = gameStream.readBytes()
            Log.d("ZEngineWrapper", "Read ${bytes.size} bytes from game stream")
            
            if (bytes.size < 64) {
                throw Exception("Game file too small: ${bytes.size} bytes. Minimum Z-Machine header is 64 bytes.")
            }

            memory = bytes
            screen = ZScreen()
            status = ZStatus()
            
            // Determine version
            val version = bytes[0].toInt()
            Log.d("ZEngineWrapper", "Game version: $version")
            
            machine = when (version) {
                 3 -> ZMachine3(screen, status, bytes)
                 5 -> ZMachine5(screen, bytes)
                 8 -> ZMachine8(screen, bytes)
                 else -> ZMachine3(screen, status, bytes) // Fallback
            }
            
            // Initialize
            machine?.restart()
        } catch (e: Exception) {
            Log.e("ZEngineWrapper", "Failed to initialize Z-Machine", e)
            machine = null
        }
    }

    fun run(): String {
        if (machine == null) return "Error: Z-Machine not initialized."
        
        try {
            // Run until input needed
            machine?.run()
            
            // Collect output from windows
            val sb = StringBuilder()
            
            // Lower window (index 0) is usually the main text
            val lower = machine?.window?.get(0)
            if (lower != null) {
                val text = lower.stringyfy(0, lower.cursor)
                sb.append(text)
                lower.clear() 
            }
            
            return sb.toString()
        } catch (e: Exception) {
            Log.e("ZEngineWrapper", "Runtime error", e)
            return "Runtime Error: ${e.message}"
        }
    }

    fun input(command: String) {
        if (machine == null) return
        
        val inputChars = command.toCharArray() + '\n' // Enter key
        machine?.fillInputBuffer(inputChars)
    }
    
    fun isRunning(): Boolean {
        return machine != null
    }
}
