package com.example.myapplication

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var scheduleLabelEditText: EditText
    private lateinit var scheduleDescriptionEditText: EditText
    private lateinit var scheduleTimeEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        scheduleLabelEditText = findViewById(R.id.scheduleLabel)
        scheduleDescriptionEditText = findViewById(R.id.scheduleDescription)
        scheduleTimeEditText = findViewById(R.id.scheduleTime)
        saveButton = findViewById(R.id.saveButton)

        // Time picker for input time
        scheduleTimeEditText.setOnClickListener {
            showTimePickerDialog()
        }

        saveButton.setOnClickListener {
            val label = scheduleLabelEditText.text.toString().trim()
            val description = scheduleDescriptionEditText.text.toString().trim()
            val inputTime = scheduleTimeEditText.text.toString().trim()

            if (label.isNotEmpty() && inputTime.isNotEmpty()) {
                val creationTime = getCurrentTime()

                val resultIntent = Intent()
                resultIntent.putExtra("label", label)
                resultIntent.putExtra("description", description)
                resultIntent.putExtra("inputTime", inputTime)
                resultIntent.putExtra("creationTime", creationTime)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Label and Time are required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            scheduleTimeEditText.setText(formattedTime)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
