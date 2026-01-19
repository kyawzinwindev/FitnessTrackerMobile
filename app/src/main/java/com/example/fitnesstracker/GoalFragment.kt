package com.example.fitnesstracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
    private var totalCaloriesBurned = 0.0
    private var goalCalories = 0.0

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

        binding.buttonSaveGoal.setOnClickListener {
            saveGoal()
        }
    }



    private fun setupDatePickers() {
        binding.editTextStartDate.setOnClickListener { showDatePicker(true) }
        binding.editTextEndDate.setOnClickListener { showDatePicker(false) }
    }

    private fun showDatePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day)
                val formatted =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.time)

                if (isStart) binding.editTextStartDate.setText(formatted)
                else binding.editTextEndDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }



    private fun fetchUserGoal() {
        val userId = SessionManager(requireContext()).getUserId()
        val url =
            "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php?user_id=$userId"

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                if (_binding == null) return@StringRequest

                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        val data = json.opt("data")
                        val goals = when (data) {
                            is JSONArray -> data
                            is JSONObject -> JSONArray().put(data)
                            else -> null
                        }

                        if (goals != null && goals.length() > 0) {
                            existingGoal = goals.getJSONObject(0)
                            populateGoalUI(existingGoal!!)
                            fetchActivitiesForProgress(existingGoal!!)
                        } else {
                            setupCreateGoalUI()
                        }
                    } else {
                        setupCreateGoalUI()
                    }
                } catch (e: Exception) {
                    setupCreateGoalUI()
                }
            },
            {
                setupCreateGoalUI()
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }



    private fun fetchActivitiesForProgress(goal: JSONObject) {
        val userId = SessionManager(requireContext()).getUserId()
        val url =
            "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php"

        val request = object : StringRequest(Method.POST, url,
            StringRequest@{ response ->
                if (_binding == null) return@StringRequest
                Log.d("GoalFragment", "Activities response: $response")

                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        calculateProgress(goal, json.optJSONArray("data"))
                    } else {
                        updateProgressUI(0.0, goalCalories)
                    }
                } catch (e: Exception) {
                    updateProgressUI(0.0, goalCalories)
                }
            },
            {
                updateProgressUI(0.0, goalCalories)
            }) {

            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "action" to "get_by_date_range", // ✅ EXACT PHP MATCH
                    "user_id" to userId.toString(),
                    "start_date" to goal.optString("start_date"),
                    "end_date" to goal.optString("end_date")
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun calculateProgress(goal: JSONObject, activities: JSONArray?) {
        goalCalories = goal.optDouble("goal_calories_burned", 0.0)

        if (activities == null || activities.length() == 0) {
            updateProgressUI(0.0, goalCalories)
            return
        }

        var total = 0.0

        for (i in 0 until activities.length()) {
            val activity = activities.optJSONObject(i) ?: continue
            total += activity.optDouble("calories_burned", 0.0)
        }

        totalCaloriesBurned = total
        updateProgressUI(totalCaloriesBurned, goalCalories)
    }



    private fun updateProgressUI(burned: Double, target: Double) {
        if (_binding == null) return

        binding.textProgress.text =
            "Burned: ${"%.1f".format(burned)} / ${"%.1f".format(target)} kcal"

        val percent =
            if (target > 0) ((burned / target) * 100).coerceIn(0.0, 100.0).toInt()
            else 0

        binding.progressBarCalories.progress = percent
        binding.textProgressPercentage.text = "$percent%"
    }


    private fun populateGoalUI(goal: JSONObject) {
        binding.goalInfoCard.visibility = View.VISIBLE
        binding.formTitle.text = "Update Your Goal"
        binding.buttonSaveGoal.text = "Update Goal"

        goalCalories = goal.optDouble("goal_calories_burned", 0.0)

        binding.textGoalCalories.text =
            "Target Calories: ${goal.optString("goal_calories_burned")}"
        binding.textGoalStartDate.text =
            "Start Date: ${goal.optString("start_date")}"
        binding.textGoalEndDate.text =
            "End Date: ${goal.optString("end_date")}"

        binding.editTextCalories.setText(goal.optString("goal_calories_burned"))
        binding.editTextStartDate.setText(goal.optString("start_date"))
        binding.editTextEndDate.setText(goal.optString("end_date"))
    }

    private fun setupCreateGoalUI() {
        binding.goalInfoCard.visibility = View.GONE
        binding.formTitle.text = "Create Your Goal"
        binding.buttonSaveGoal.text = "Create Goal"

        binding.editTextCalories.text = null
        binding.editTextStartDate.text = null
        binding.editTextEndDate.text = null

        existingGoal = null
        totalCaloriesBurned = 0.0
        goalCalories = 0.0
    }


    private fun saveGoal() {
        val session = SessionManager(requireContext())

        val calories = binding.editTextCalories.text.toString()
        val start = binding.editTextStartDate.text.toString()
        val end = binding.editTextEndDate.text.toString()

        if (calories.isBlank() || start.isBlank() || end.isBlank()) {
            Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val url =
            "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php"

        val request = object : StringRequest(Method.POST, url,
            {
                Toast.makeText(context, "Goal saved", Toast.LENGTH_SHORT).show()
                fetchUserGoal()
            },
            {
                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = hashMapOf(
                    "user_id" to session.getUserId().toString(),
                    "goal_calories_burned" to calories,
                    "start_date" to start,
                    "end_date" to end
                )

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
