package com.example.fitnesstracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentGoalBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalFragment : Fragment() {

    private var _binding: FragmentGoalBinding? = null
    private val binding get() = _binding!!

    private var existingGoal: JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePickers()
        fetchUserGoal()

        binding.buttonSaveGoal.setOnClickListener { saveGoal() }
    }

    private fun setupDatePickers() {
        binding.editTextStartDate.setOnClickListener { showDatePickerDialog(isStartDate = true) }
        binding.editTextEndDate.setOnClickListener { showDatePickerDialog(isStartDate = false) }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                if (isStartDate) {
                    binding.editTextStartDate.setText(formattedDate)
                } else {
                    binding.editTextEndDate.setText(formattedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun fetchUserGoal() {
        val sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId()
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php?user_id=$userId"

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                if (_binding == null) return@StringRequest
                val trimmedResponse = response.trim()

                if (trimmedResponse.isNotBlank() && trimmedResponse.lowercase() != "null") {
                    try {
                        var goals: JSONArray? = null
                        // DEFINITIVE FIX: Robustly handle if the API returns a single object or an array.
                        if (trimmedResponse.startsWith("{")) {
                            val jsonObject = JSONObject(trimmedResponse)
                            if (jsonObject.optString("status") == "success") {
                                val data = jsonObject.opt("data")
                                if (data is JSONArray) {
                                    goals = data
                                } else if (data is JSONObject) {
                                    // If the server sends a single object, wrap it in an array.
                                    goals = JSONArray().put(data)
                                }
                            }
                        } else if (trimmedResponse.startsWith("[")) {
                            goals = JSONArray(trimmedResponse)
                        }

                        if (goals != null && goals.length() > 0 && !goals.isNull(0)) {
                            existingGoal = goals.getJSONObject(0)
                            populateUiWithGoalData(existingGoal!!)
                        } else {
                            setupCreateGoalUI()
                        }
                    } catch (e: Exception) {
                        Log.e("GoalFragment", "Error parsing goal response: $trimmedResponse", e)
                        setupCreateGoalUI()
                    }
                } else {
                    setupCreateGoalUI()
                }
            },
            { error ->
                if (_binding == null) return@StringRequest
                Log.e("GoalFragment", "Volley error fetching goal: ${error.message}")
                setupCreateGoalUI()
            })

        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun populateUiWithGoalData(goal: JSONObject) {
        binding.goalInfoCard.visibility = View.VISIBLE
        binding.textGoalCalories.text = "Target Calories: ${goal.optString("goal_calories_burned")}"
        binding.textGoalStartDate.text = "Start Date: ${goal.optString("start_date")}"
        binding.textGoalEndDate.text = "End Date: ${goal.optString("end_date")}"

        binding.formTitle.text = "Update Your Goal"
        binding.editTextCalories.setText(goal.optString("goal_calories_burned"))
        binding.editTextStartDate.setText(goal.optString("start_date"))
        binding.editTextEndDate.setText(goal.optString("end_date"))
        binding.buttonSaveGoal.text = "Update Goal"
    }

    private fun setupCreateGoalUI() {
        binding.goalInfoCard.visibility = View.GONE
        binding.formTitle.text = "Create Your Goal"
        binding.buttonSaveGoal.text = "Create Goal"
        binding.editTextCalories.setText("")
        binding.editTextStartDate.setText("")
        binding.editTextEndDate.setText("")
        existingGoal = null
    }

    private fun saveGoal() {
        val sessionManager = SessionManager(requireContext())
        val calories = binding.editTextCalories.text.toString()
        val startDate = binding.editTextStartDate.text.toString()
        val endDate = binding.editTextEndDate.text.toString()

        if (calories.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(context, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                Toast.makeText(context, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                fetchUserGoal() // Refresh the goal display
            },
            { error ->
                Toast.makeText(context, "Error saving goal: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = sessionManager.getUserId().toString()
                params["goal_calories_burned"] = calories
                params["start_date"] = startDate
                params["end_date"] = endDate
                if (existingGoal != null) {
                    params["action"] = "update"
                    params["id"] = existingGoal!!.optString("id")
                }
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
