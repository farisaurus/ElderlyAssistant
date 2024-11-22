package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.database.DatabaseInstance
import com.example.myapplication.database.Schedule
import kotlinx.coroutines.*
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.util.*

class SpeakActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var voiceButton: Button
    private lateinit var recognizedText: TextView
    private lateinit var tflite: Interpreter
    private lateinit var tokenizer: Map<String, Int>
    private lateinit var labelMap: Map<Int, String>
    private var isListening = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speak)

        voiceButton = findViewById(R.id.voiceButton)
        recognizedText = findViewById(R.id.recognizedText)

        checkPermissions()

        // Launch the coroutine to load model and tokenizer
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadModelAndTokenizerAsync()
                Toast.makeText(this@SpeakActivity, "Initialization complete", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Initialization", "Error during initialization", e)
                Toast.makeText(this@SpeakActivity, "Failed to initialize model or tokenizer.", Toast.LENGTH_LONG).show()
                return@launch
            }
        }

        setupSpeechRecognizer()

        voiceButton.setOnClickListener {
            if (isListening) {
                stopListening()
                voiceButton.text = "Start Listening" // Change button label back
            } else {
                startListening()
                voiceButton.text = "Listening..." // Indicate listening state
            }
        }
        val tvClose = findViewById<TextView>(R.id.tvClose)
        tvClose.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Menutup SpeakActivity
        }


    }


    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private suspend fun loadModelAndTokenizerAsync() = withContext(Dispatchers.IO) {
        try {
            // Load TensorFlow Lite model
            val modelFile = assets.open("intent_classification_model.tflite").use { inputStream ->
                ByteBuffer.allocateDirect(inputStream.available()).apply {
                    put(inputStream.readBytes())
                    rewind()
                }
            }
            tflite = Interpreter(modelFile)

            // Load Tokenizer
            val tokenizerFile = assets.open("tokenizer.json").bufferedReader().use { it.readText() }
            val tokenizerJson = JSONObject(tokenizerFile).getJSONObject("config").getJSONObject("word_index")
            tokenizer = tokenizerJson.keys().asSequence().associateWith { tokenizerJson.getInt(it) }

            // Initialize labelMap
            labelMap = mapOf(
                1 to "reminder",    // Map 0 index to reminder intent
                0 to "medical_qna"  // Map 1 index to medical_qna intent
            )

            Log.d("Initialization", "Model and tokenizer loaded successfully.")
        } catch (e: Exception) {
            Log.e("Initialization", "Failed to load model or tokenizer", e)
            throw e
        }
    }
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListenerAdapter() {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d("VoiceCommand", "Recognized Text: $spokenText")
                    handleVoiceCommand(spokenText)
                } else {
                    showResponse("No text recognized")
                }
            }
        })
    }

    private fun preprocessInput(input: String, maxLength: Int): FloatArray {
        val tokens = input.lowercase(Locale.getDefault())
            .split(" ")
            .map { tokenizer[it] ?: 0 }

        // Create an array for batch size 32
        val paddedTokens = FloatArray(32 * maxLength) // 32 sequences, each of length maxLength

        for (i in tokens.indices) {
            paddedTokens[i] = if (i < maxLength) tokens[i].toFloat() else 0f
        }

        return paddedTokens
    }


    private fun predictIntent(input: String): String {
        val inputTensor = preprocessInput(input, 50)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(32, 50), org.tensorflow.lite.DataType.FLOAT32)
        inputBuffer.loadArray(inputTensor)

        // Output tensor shape [32, 2] (32 sequences, each with 2 possible labels)
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(32, 2), org.tensorflow.lite.DataType.FLOAT32)
        tflite.run(inputBuffer.buffer, outputBuffer.buffer)

        // Extract the prediction for the first sequence
        val outputArray = outputBuffer.floatArray
        val predictedIndex = outputArray.sliceArray(0 until 2).indices.maxByOrNull { outputArray[it] } ?: -1
        return if (predictedIndex != -1) {
            labelMap[predictedIndex] ?: "Unknown"
        } else {
            "Unknown"
        }
    }



    private fun handleVoiceCommand(command: String) {
        try {
            val predictedIntent = predictIntent(command)
            Log.d("VoiceCommand", "Predicted Intent: $predictedIntent")

            when (predictedIntent) {
                "reminder" -> {
                    val reminderData = extractReminderDetails(command) // Extract details from voice command
                    if (reminderData != null) {
                        saveScheduleToDatabase(reminderData) // Save to Room database
                        showResponse("Pengingat berhasil disimpan untuk jam ${reminderData.inputTime}")
                    } else {
                        showResponse("Maaf, saya tidak mengerti detail pengingat.")
                    }
                }
                "medical_qna" -> showResponse("Intent: Medical Q&A detected")
                else -> showResponse("Maaf, saya tidak mengerti.")
            }
        } catch (e: Exception) {
            Log.e("VoiceCommand", "Error handling command", e)
            showResponse("Error processing the command.")
        }
    }


    private fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            isListening = true
            voiceButton.text = "Listening..." // Update UI

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            }

            Log.d("SpeechRecognizer", "Starting listening...")
            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(this, "Speech recognition not available.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        voiceButton.text = "Start Listening" // Update UI
        Log.d("SpeechRecognizer", "Stopped listening.")
    }


    private fun showResponse(response: String) {
        runOnUiThread {
            recognizedText.text = response
            Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tflite.close()
    }
    private fun extractReminderDetails(command: String): Schedule? {
        // List of possible command variations
        val commandPrefixes = listOf(
            "ingatkan saya", "ingatkan untuk", "buatkan jadwal", "jadwalkan",
            "tolong buat jadwal", "ingat saya", "tolong ingatkan saya", "buat pengingat",
            "ingatkan jadwal", "jadwalkan", "tolong jadwalkan", "setel pengingat",
            "atur jadwal", "atur pengingat", "tolong atur pengingat", "buat agenda",
            "jadwalkan untuk", "ingat agenda", "tolong atur agenda", "buat alarm",
            "ingat jadwal", "ingat pengingat", "atur agenda", "ingatkan aktivitas",
            "buatkan alarm", "buatkan pengingat", "buat catatan", "ingat aktivitas",
            "ingat jadwal saya", "ingat saya untuk", "atur aktivitas", "buatkan aktivitas",
            "buat pengingat aktivitas", "buatkan jadwal untuk", "jadwalkan aktivitas",
            "ingatkan untuk jadwal", "tolong ingat jadwal", "setel agenda", "atur pengingat saya",
            "atur jadwal saya", "ingat jadwal harian", "buatkan pengingat harian",
            "atur alarm", "atur aktivitas harian", "ingatkan kegiatan", "atur kegiatan",
            "ingatkan saya tentang", "buatkan agenda", "atur agenda harian", "ingatkan tentang"
        )

        // Regex to match time (e.g., "jam 10 pagi")
        val timeRegex = Regex("""\b(jam\s\d{1,2}(?:\.\d{2})?\s(?:pagi|siang|sore|malam))\b""")
        val timeMatch = timeRegex.find(command)
        val time = timeMatch?.value?.replace("jam ", "")?.trim() ?: "00:00"

        // Remove the prefix from the command
        var commandWithoutPrefix = command.lowercase(Locale.getDefault())
        for (prefix in commandPrefixes) {
            if (commandWithoutPrefix.startsWith(prefix)) {
                commandWithoutPrefix = commandWithoutPrefix.replaceFirst(prefix, "").trim()
                break
            }
        }

        // Remove the time portion
        val commandWithoutTime = commandWithoutPrefix.replace(timeMatch?.value ?: "", "").trim()

        // Split into label and description
        val words = commandWithoutTime.split(" ")
        val labelBuilder = StringBuilder()
        val descriptionBuilder = StringBuilder()

        for ((index, word) in words.withIndex()) {
            if (index == 0 || (index == 1 && words.size > 2)) {
                labelBuilder.append("$word ")
            } else {
                descriptionBuilder.append("$word ")
            }
        }

        val label = labelBuilder.toString().trim()
        val description = descriptionBuilder.toString().trim()

        // Create a Schedule object
        return if (label.isNotEmpty() && time.isNotEmpty()) {
            Schedule(
                label = label,
                description = description,
                inputTime = time,
                creationTime = System.currentTimeMillis().toString()
            )
        } else {
            null
        }
    }


    private fun saveScheduleToDatabase(schedule: Schedule) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseInstance.getDatabase(applicationContext)
                db.scheduleDao().insert(schedule)
                Log.d("Database", "Reminder saved: $schedule")
            } catch (e: Exception) {
                Log.e("Database", "Error saving reminder", e)
            }
        }
    }


}
