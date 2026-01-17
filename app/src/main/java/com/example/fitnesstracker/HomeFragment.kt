package com.example.fitnesstracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var userGoal: JSONObject? = null
    private var userActivities: JSONArray? = null
    private var isGoalRequestFinished = false
    private var isActivityRequestFinished = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Utils.init(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.letsDoItButton.setOnClickListener { findNavController().navigate(R.id.createActivityFragment) }
        binding.createActivityButtonHome.setOnClickListener { findNavController().navigate(R.id.createActivityFragment) }
        binding.fabLocation.setOnClickListener { requestLocationPermission() }
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(requireContext())
        binding.welcomeText.text = "Hello, ${sessionManager.getFirstName() ?: ""} ${sessionManager.getLastName() ?: ""}"

        isGoalRequestFinished = false
        isActivityRequestFinished = false
        userGoal = null
        userActivities = null
        fetchUserGoal(sessionManager.getUserId())
        fetchActivityHistory(sessionManager.getUserId())
    }

    private fun fetchUserGoal(userId: Int) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php?user_id=$userId"
        val request = StringRequest(Request.Method.GET, url,
            { response ->
                val trimmedResponse = response.trim()
                if (trimmedResponse.isNotBlank() && trimmedResponse.lowercase() != "null") {
                    try {
                        val goals = JSONArray(trimmedResponse)
                        if (goals.length() > 0 && !goals.isNull(0)) {
                            userGoal = goals.getJSONObject(0)
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error parsing goal data: $response", e)
                    }
                }
                isGoalRequestFinished = true
                checkAllDataFetched()
            },
            { error ->
                Log.e("HomeFragment", "Volley error fetching goal: ${error.message}")
                isGoalRequestFinished = true
                checkAllDataFetched()
            })
        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchActivityHistory(userId: Int) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php?user_id=$userId"
        val request = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "success") {
                        userActivities = jsonObject.optJSONArray("data")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error parsing activity data: $response", e)
                }
                isActivityRequestFinished = true
                checkAllDataFetched()
            },
            { error ->
                Log.e("HomeFragment", "Volley error fetching activities: ${error.message}")
                isActivityRequestFinished = true
                checkAllDataFetched()
            })
        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun checkAllDataFetched() {
        if (isGoalRequestFinished && isActivityRequestFinished && _binding != null) {
            updateUi()
        }
    }

    private fun updateUi() {
        // Check if there's an active goal to show progress
        val hasActiveGoal = hasActiveGoal()

        // Check if there are activities to show chart
        val hasActivities = userActivities != null && userActivities!!.length() > 0

        // Handle the empty state first
        if (!hasActivities && !hasActiveGoal) {
            // No activities and no active goal - show empty state
            binding.groupContentState.visibility = View.GONE
            binding.groupEmptyState.visibility = View.VISIBLE
            binding.goalProgressCard.visibility = View.GONE
            return
        }

        // Show content state if there's either activities or an active goal
        binding.groupContentState.visibility = View.VISIBLE
        binding.groupEmptyState.visibility = View.GONE

        // Update goal progress visibility based on active goal
        updateGoalProgress()

        // Setup chart if there are activities
        if (hasActivities) {
            setupChart(userActivities!!)
        } else {
            // Hide chart if no activities
            binding.chart.visibility = View.GONE
        }
    }

    private fun hasActiveGoal(): Boolean {
        val goal = userGoal ?: return false

        val isAchieved = goal.optInt("is_achieved", 0) == 1
        val goalCalories = goal.optDouble("goal_calories_burned", 0.0)

        // Only return true if there's a goal that's not achieved and has positive calories target
        return !isAchieved && goalCalories > 0
    }

    private fun updateGoalProgress() {
        val goal = userGoal ?: run {
            binding.goalProgressCard.visibility = View.GONE
            return
        }

        val isAchieved = goal.optInt("is_achieved", 0) == 1
        val goalCalories = goal.optDouble("goal_calories_burned", 0.0)

        // Hide progress card if goal is achieved or has invalid calories target
        if (isAchieved || goalCalories <= 0) {
            binding.goalProgressCard.visibility = View.GONE
            return
        }

        binding.goalProgressCard.visibility = View.VISIBLE
        var totalCaloriesInRange = 0.0

        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val goalStartDate = LocalDate.parse(goal.optString("start_date"), formatter)
            val goalEndDateStr = goal.optString("end_date")
            val goalEndDate = if (goalEndDateStr.isNotEmpty()) LocalDate.parse(goalEndDateStr, formatter) else null

            val activities = userActivities
            if (activities != null) {
                for (i in 0 until activities.length()) {
                    val activity = activities.getJSONObject(i)
                    try {
                        val activityDateStr = activity.optString("date")
                        if (activityDateStr.length >= 10) {
                            val activityDate = LocalDate.parse(activityDateStr.substring(0, 10), formatter)
                            if (isDateInRange(activityDate, goalStartDate, goalEndDate)) {
                                totalCaloriesInRange += activity.optDouble("calories_burned", 0.0)
                            }
                        }
                    } catch (e: DateTimeParseException) {
                        Log.w("HomeFragment", "Skipping activity with malformed date: ${activity.optString("date")}", e)
                    }
                }
            }
        } catch (e: DateTimeParseException) {
            totalCaloriesInRange = 0.0
            Log.e("HomeFragment", "Could not parse goal dates. Displaying 0% progress.", e)
        }

        val progressPercentage = if (goalCalories > 0) ((totalCaloriesInRange / goalCalories) * 100).toInt() else 0
        binding.goalProgressBar.progress = progressPercentage.coerceIn(0, 100)
        binding.goalProgressText.text = "${totalCaloriesInRange.toInt()} / ${goalCalories.toInt()} kcal"

        if (!isAchieved && totalCaloriesInRange >= goalCalories) {
            markGoalAsAchieved(goal.optInt("id"))
        }
    }

    private fun isDateInRange(date: LocalDate, startDate: LocalDate, endDate: LocalDate?): Boolean {
        val isAfterStartOrOn = !date.isBefore(startDate)
        val isBeforeEndOrOn = endDate == null || !date.isAfter(endDate)
        return isAfterStartOrOn && isBeforeEndOrOn
    }

    private fun markGoalAsAchieved(goalId: Int) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/GoalController.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                Toast.makeText(context, "Congratulations! Goal Achieved!", Toast.LENGTH_LONG).show()
                onResume()
            },
            { error ->
                Log.e("HomeFragment", "Error updating goal to achieved: ${error.message}")
            }) {
            override fun getParams(): Map<String, String> = mapOf(
                "action" to "update",
                "id" to goalId.toString(),
                "is_achieved" to "1"
            )
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun setupChart(activities: JSONArray) {
        val activityTypes = arrayOf("Running", "Cycling", "Swimming", "Hiking", "Yoga", "WeightLifting")
        val caloriesPerActivity = mutableMapOf<String, Float>()
        activityTypes.forEach { caloriesPerActivity[it] = 0f }

        for (i in 0 until activities.length()) {
            val activity = activities.getJSONObject(i)
            val type = activity.optString("activity_type")
            val calories = activity.optDouble("calories_burned", 0.0).toFloat()
            if (caloriesPerActivity.containsKey(type)) {
                caloriesPerActivity[type] = caloriesPerActivity.getValue(type) + calories
            }
        }

        val entries = ArrayList<BarEntry>()
        activityTypes.forEachIndexed { index, type ->
            entries.add(BarEntry(index.toFloat(), caloriesPerActivity[type] ?: 0f))
        }

        val dataSet = BarDataSet(entries, "Total Calories Burned").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.chart.visibility = View.VISIBLE
        binding.chart.data = BarData(dataSet)
        binding.chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(activityTypes)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = activityTypes.size
            labelRotationAngle = -45f
            textColor = Color.BLACK
        }
        binding.chart.setViewPortOffsets(Utils.convertDpToPixel(40f), Utils.convertDpToPixel(20f), Utils.convertDpToPixel(40f), Utils.convertDpToPixel(80f))
        binding.chart.description.isEnabled = false
        binding.chart.legend.isEnabled = false
        binding.chart.animateY(1000)
        binding.chart.invalidate()
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.any { ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, 100)
        } else {
            getLastLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    binding.latitudeText.text = "Latitude: ${location.latitude}"
                    binding.longitudeText.text = "Longitude: ${location.longitude}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}