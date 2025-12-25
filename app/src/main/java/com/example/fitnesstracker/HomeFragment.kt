package com.example.fitnesstracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize the chart utils for unit conversion
        Utils.init(requireContext())

        binding.createActivityButtonHome.setOnClickListener {
            findNavController().navigate(R.id.createActivityFragment)
        }

        binding.letsDoItButton.setOnClickListener {
            findNavController().navigate(R.id.createActivityFragment)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(requireContext())
        val firstName = sessionManager.getFirstName()
        val lastName = sessionManager.getLastName()

        binding.welcomeText.text = "Hello, ${firstName ?: ""} ${lastName ?: ""}"

        fetchActivityHistory(sessionManager.getUserId())
    }

    private fun fetchActivityHistory(userId: Int) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php?user_id=$userId"
        val request = StringRequest(Request.Method.GET, url,
            { response ->
                if (_binding == null) return@StringRequest
                Log.d("Home", "Raw Server Response: $response")
                if (response.isNullOrBlank()) {
                    showEmptyState()
                    return@StringRequest
                }

                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "success") {
                        val activities = jsonObject.optJSONArray("data")
                        if (activities != null && activities.length() > 0) {
                            binding.groupChartState.visibility = View.VISIBLE
                            binding.groupEmptyState.visibility = View.GONE
                            setupChart(activities)
                        } else {
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                } catch (e: Exception) {
                    Log.e("Home", "Failed to parse activity history: $response", e)
                    showEmptyState()
                }
            },
            { error ->
                if (_binding == null) return@StringRequest
                Log.e("Home", "Volley error: ${error.message}")
                showEmptyState()
            })

        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showEmptyState() {
        binding.groupChartState.visibility = View.GONE
        binding.groupEmptyState.visibility = View.VISIBLE
    }

    private fun setupChart(activities: JSONArray) {
        val activityTypes = arrayOf("Running", "Cycling", "Swimming", "Hiking", "Yoga", "WeightLifting")
        val caloriesPerActivity = mutableMapOf<String, Float>()
        for (type in activityTypes) {
            caloriesPerActivity[type] = 0f
        }

        for (i in 0 until activities.length()) {
            val activity = activities.getJSONObject(i)
            val type = activity.optString("activity_type")
            val calories = activity.optInt("calories_burned", 0).toFloat()
            if (caloriesPerActivity.containsKey(type)) {
                caloriesPerActivity[type] = caloriesPerActivity.getValue(type) + calories
            }
        }

        val entries = ArrayList<BarEntry>()
        for ((index, type) in activityTypes.withIndex()) {
            entries.add(BarEntry(index.toFloat(), caloriesPerActivity[type] ?: 0f))
        }

        val dataSet = BarDataSet(entries, "Total Calories Burned")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.chart.data = barData

        // Configure the X-axis
        val xAxis = binding.chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(activityTypes)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setLabelCount(activityTypes.size)
        xAxis.labelRotationAngle = -45f
        xAxis.textColor = Color.BLACK

        // Definitive Fix: Use setViewPortOffsets with DP-to-Pixel conversion
        val leftOffset = Utils.convertDpToPixel(40f)
        val topOffset = Utils.convertDpToPixel(20f)
        val rightOffset = Utils.convertDpToPixel(40f)
        val bottomOffset = Utils.convertDpToPixel(80f) // Generous bottom offset
        binding.chart.setViewPortOffsets(leftOffset, topOffset, rightOffset, bottomOffset)

        // General chart styling
        binding.chart.description.isEnabled = false
        binding.chart.legend.isEnabled = false
        binding.chart.animateY(1000)
        binding.chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
