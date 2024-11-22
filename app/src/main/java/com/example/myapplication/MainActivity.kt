package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.database.DatabaseInstance
import com.example.myapplication.database.Schedule
import com.example.myapplication.database.ScheduleDao
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ScheduleAdapter.OnDeleteClickListener {

    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var scheduleDao: ScheduleDao
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = DatabaseInstance.getDatabase(this)
        scheduleDao = db.scheduleDao()

        scheduleAdapter = ScheduleAdapter(mutableListOf(), this)
        binding.reminderRecyclerView.adapter = scheduleAdapter
        binding.reminderRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.addScheduleButton.setOnClickListener {
            val intent = Intent(this, AddScheduleActivity::class.java)
            startActivityForResult(intent, 1)
        }

        binding.speakButton.setOnClickListener {
            val intent = Intent(this, SpeakActivity::class.java)
            startActivity(intent)
        }

        loadSchedules()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val label = data?.getStringExtra("label") ?: ""
            val description = data?.getStringExtra("description") ?: ""
            val inputTime = data?.getStringExtra("inputTime") ?: ""
            val creationTime = data?.getStringExtra("creationTime") ?: ""

            val newSchedule = Schedule(
                label = label,
                description = description,
                inputTime = inputTime,
                creationTime = creationTime
            )

            CoroutineScope(Dispatchers.IO).launch {
                scheduleDao.insert(newSchedule)
                loadSchedules()
            }
        }
    }

    private fun loadSchedules() {
        CoroutineScope(Dispatchers.IO).launch {
            val schedules = scheduleDao.getAllSchedules()
            Log.d("MainActivity", "Loaded schedules: $schedules")
            runOnUiThread {
                scheduleAdapter.setSchedules(schedules)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSchedules()
    }

    override fun onDeleteClick(position: Int) {
        val schedule = scheduleAdapter.getScheduleAt(position)
        CoroutineScope(Dispatchers.IO).launch {
            scheduleDao.delete(schedule)
            loadSchedules()
        }
    }
}
