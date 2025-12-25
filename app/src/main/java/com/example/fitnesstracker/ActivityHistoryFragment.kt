package com.example.fitnesstracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentActivityHistoryBinding
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class ActivityHistoryFragment : Fragment() {

    private var _binding: FragmentActivityHistoryBinding? = null
    private val binding get() = _binding!!

    private val metricMappings = mapOf(
        "Running" to arrayOf("Distance (km)", "Duration (min)", "Speed (km/h)"),
        "Cycling" to arrayOf("Distance (km)", "Duration (min)", "Speed (km/h)"),
        "Swimming" to arrayOf("Distance (m)", "Duration (min)", "Laps"),
        "Hiking" to arrayOf("Distance (km)", "Duration (min)", "Speed (km/h)"),
        "Yoga" to arrayOf("Duration (min)", "", ""),
        "WeightLifting" to arrayOf("Duration (min)", "Sets", "Weight (kg)")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityHistoryBinding.inflate(inflater, container, false)

        binding.createActivityButtonHistory.setOnClickListener {
            findNavController().navigate(R.id.createActivityFragment)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        fetchActivityHistory()
    }

    private fun fetchActivityHistory() {
        val sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId()
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php?user_id=$userId"

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                Log.d("History", "Raw Server Response: $response")
                if (_binding == null) return@StringRequest
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "success") {
                        val activities = jsonObject.optJSONArray("data")
                        if (activities != null && activities.length() > 0) {
                            binding.activityHistoryRecyclerView.visibility = View.VISIBLE
                            binding.emptyHistoryText.visibility = View.GONE
                            binding.createActivityButtonHistory.visibility = View.GONE
                            binding.activityHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                            binding.activityHistoryRecyclerView.adapter = ActivityHistoryAdapter(activities, ::onEditClick, ::onDeleteClick)
                        } else {
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    Log.e("History", "Failed to parse history: $response", e)
                    showEmptyState()
                }
            },
            { error ->
                if (_binding == null) return@StringRequest
                Log.e("History", "Volley error: $error")
                showEmptyState()
            }
        )

        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showEmptyState() {
        binding.activityHistoryRecyclerView.visibility = View.GONE
        binding.emptyHistoryText.visibility = View.VISIBLE
        binding.createActivityButtonHistory.visibility = View.VISIBLE
    }

    private fun onEditClick(activity: JSONObject) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_activity, null)
        val dateInput = dialogView.findViewById<EditText>(R.id.edit_date_edit_text)
        val timeInput = dialogView.findViewById<EditText>(R.id.edit_time_edit_text)
        val metric1Label = dialogView.findViewById<TextView>(R.id.metric1_label)
        val metric1Input = dialogView.findViewById<EditText>(R.id.edit_metric1_edit_text)
        val metric2Label = dialogView.findViewById<TextView>(R.id.metric2_label)
        val metric2Input = dialogView.findViewById<EditText>(R.id.edit_metric2_edit_text)
        val metric3Label = dialogView.findViewById<TextView>(R.id.metric3_label)
        val metric3Input = dialogView.findViewById<EditText>(R.id.edit_metric3_edit_text)

        val activityType = activity.optString("activity_type")
        val hints = metricMappings[activityType]

        metric1Label.text = hints?.get(0) ?: "Metric 1"
        metric2Label.text = hints?.get(1) ?: "Metric 2"
        metric3Label.text = hints?.get(2) ?: "Metric 3"

        dateInput.setText(activity.optString("date"))
        timeInput.setText(activity.optString("time"))
        metric1Input.setText(activity.optString("m1"))
        metric2Input.setText(activity.optString("m2"))
        metric3Input.setText(activity.optString("m3"))

        dateInput.setOnClickListener { showDatePickerDialog(dateInput) }
        timeInput.setOnClickListener { showTimePickerDialog(timeInput) }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Activity")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                updateActivity(
                    activity.getInt("id"),
                    dateInput.text.toString(),
                    timeInput.text.toString(),
                    metric1Input.text.toString(),
                    metric2Input.text.toString(),
                    metric3Input.text.toString()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onDeleteClick(activity: JSONObject) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete this activity?")
            .setPositiveButton("Delete") { _, _ ->
                deleteActivity(activity.getInt("id"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteActivity(activityId: Int) {
        val sessionManager = SessionManager(requireContext())
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                Log.d("Delete", "Response: $response")
                Toast.makeText(requireContext(), "Activity deleted successfully", Toast.LENGTH_SHORT).show()
                fetchActivityHistory()
            },
            Response.ErrorListener { error ->
                Log.e("Delete", "Error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to delete activity", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                 return mapOf(
                    "action" to "delete",
                    "id" to activityId.toString(),
                    "user_id" to sessionManager.getUserId().toString()
                )
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun updateActivity(activityId: Int, date: String, time: String, m1: String, m2: String, m3: String) {
        val sessionManager = SessionManager(requireContext())
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                Log.d("Update", "Response: $response")
                Toast.makeText(requireContext(), "Activity updated successfully", Toast.LENGTH_SHORT).show()
                fetchActivityHistory()
            },
            Response.ErrorListener { error ->
                Log.e("Update", "Error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to update activity", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "action" to "update",
                    "id" to activityId.toString(),
                    "user_id" to sessionManager.getUserId().toString(),
                    "date" to date,
                    "time" to time,
                    "m1" to m1,
                    "m2" to m2,
                    "m3" to m3
                )
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showDatePickerDialog(dateEditText: EditText) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val formattedDate = String.format("%d-%02d-%02d", y, m + 1, d)
            dateEditText.setText(formattedDate)
        }, year, month, day).show()
    }

    private fun showTimePickerDialog(timeEditText: EditText) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        TimePickerDialog(requireContext(), { _, h, m ->
            val formattedTime = String.format("%02d:%02d:00", h, m)
            timeEditText.setText(formattedTime)
        }, hour, minute, true).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
