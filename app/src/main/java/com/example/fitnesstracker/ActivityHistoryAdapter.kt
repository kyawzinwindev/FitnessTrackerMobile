package com.example.fitnesstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class ActivityHistoryAdapter(
    private val activities: JSONArray,
    private val onEditClick: (JSONObject) -> Unit,
    private val onDeleteClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<ActivityHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activityType: TextView = view.findViewById(R.id.activity_type_text_view)
        val caloriesBurned: TextView = view.findViewById(R.id.calories_burned_text_view)
        val date: TextView = view.findViewById(R.id.date_text_view)
        val time: TextView = view.findViewById(R.id.time_text_view)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities.getJSONObject(position)
        holder.activityType.text = activity.optString("activity_type", "N/A")
        val calories = activity.optInt("calories_burned", 0)
        holder.caloriesBurned.text = "Calories Burned: $calories"
        holder.date.text = activity.optString("date")
        holder.time.text = activity.optString("time")

        holder.editButton.setOnClickListener { onEditClick(activity) }
        holder.deleteButton.setOnClickListener { onDeleteClick(activity) }
    }

    override fun getItemCount() = activities.length()
}
