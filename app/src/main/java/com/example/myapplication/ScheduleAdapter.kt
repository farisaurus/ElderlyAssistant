package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.database.Schedule

class ScheduleAdapter(private val schedules: MutableList<Schedule>, private val listener: OnDeleteClickListener) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {
    fun setSchedules(newSchedules: List<Schedule>) {
        schedules.clear()
        schedules.addAll(newSchedules)
        notifyDataSetChanged() // Refresh RecyclerView
    }

    fun getScheduleAt(position: Int): Schedule = schedules[position]
    interface OnDeleteClickListener {
        fun onDeleteClick(position: Int)
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val medicationTextView: TextView = itemView.findViewById(R.id.medicationTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val creationTimeTextView: TextView = itemView.findViewById(R.id.creationTimeTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(schedule: Schedule, position: Int) {
            medicationTextView.text = schedule.label
            descriptionTextView.text = schedule.description
            timeTextView.text = "Time: ${schedule.inputTime}"
            creationTimeTextView.text = "Created at: ${schedule.creationTime}"
            deleteButton.setOnClickListener {
                listener.onDeleteClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position], position)
    }

    override fun getItemCount(): Int {
        return schedules.size
    }

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
        notifyItemInserted(schedules.size - 1)
    }

    fun removeSchedule(position: Int) {
        schedules.removeAt(position)
        notifyItemRemoved(position)
    }
}
