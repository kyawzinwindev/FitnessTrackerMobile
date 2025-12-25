package com.example.fitnesstracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentCreateActivityBinding
import org.json.JSONObject
import java.util.Calendar

class CreateActivityFragment : Fragment() {

    private var _binding: FragmentCreateActivityBinding? = null
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
        _binding = FragmentCreateActivityBinding.inflate(inflater, container, false)

        val activities = metricMappings.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, activities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.activityTypeSpinner.adapter = adapter

        binding.dateEditText.setOnClickListener { showDatePickerDialog() }
        binding.timeEditText.setOnClickListener { showTimePickerDialog() }

        binding.activityTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMetricHints(activities[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.createActivityButton.setOnClickListener { createActivity() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateMetricHints(activityType: String) {
        val hints = metricMappings[activityType]
        binding.metric1EditText.hint = hints?.get(0) ?: "Metric 1"
        binding.metric2EditText.hint = hints?.get(1) ?: "Metric 2"
        binding.metric3EditText.hint = hints?.get(2) ?: "Metric 3"
    }

    private fun createActivity() {
        val sessionManager = SessionManager(requireContext())
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php"

        val date = binding.dateEditText.text.toString()
        val time = binding.timeEditText.text.toString()

        if (date.isBlank() || time.isBlank()) {
            Toast.makeText(requireContext(), "Please enter date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                if (_binding == null) return@Listener
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optString("status") == "success") {
                        Toast.makeText(requireContext(), "Activity created successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.activityHistoryFragment)
                    } else {
                        val message = jsonResponse.optString("message", "Unknown error.")
                        Toast.makeText(requireContext(), "Failed to create activity: $message", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("CreateActivity", "Error parsing response: $response", e)
                    Toast.makeText(requireContext(), "An error occurred while creating the activity.", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                if (_binding == null) return@ErrorListener
                val networkResponse = error.networkResponse
                var errorMessage = "Unknown error"
                if (networkResponse?.data != null) {
                    errorMessage = String(networkResponse.data, Charsets.UTF_8)
                }
                Toast.makeText(requireContext(), "Error connecting to the server.", Toast.LENGTH_SHORT).show()
                Log.e("CreateActivity", "Error: ${error.message}, Body: $errorMessage")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                // Definitive Fix: Add calories_burned parameter as it's required by the server.
                params["user_id"] = sessionManager.getUserId().toString()
                params["activity_type"] = binding.activityTypeSpinner.selectedItem.toString()
                params["date"] = date
                params["time"] = time
                params["m1"] = binding.metric1EditText.text.toString().ifEmpty { "0" }
                params["m2"] = binding.metric2EditText.text.toString().ifEmpty { "0" }
                params["m3"] = binding.metric3EditText.text.toString().ifEmpty { "0" }
                params["calories_burned"] = "0" // Send 0, server will recalculate.
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val formattedDate = String.format("%d-%02d-%02d", y, m + 1, d)
            _binding?.dateEditText?.setText(formattedDate)
        }, year, month, day).show()
    }

    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        TimePickerDialog(requireContext(), { _, h, m ->
            val formattedTime = String.format("%02d:%02d:00", h, m)
            _binding?.timeEditText?.setText(formattedTime)
        }, hour, minute, true).show()
    }
}
