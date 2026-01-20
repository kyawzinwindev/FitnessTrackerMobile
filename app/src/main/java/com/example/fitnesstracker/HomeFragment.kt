package com.example.fitnesstracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var userActivities: JSONArray? = null
    private var isActivityRequestFinished = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Utils.init(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.letsDoItButton.setOnClickListener {
            findNavController().navigate(R.id.createActivityFragment)
        }

        binding.createActivityButtonHome.setOnClickListener {
            findNavController().navigate(R.id.createActivityFragment)
        }

        binding.fabLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(requireContext())
        binding.welcomeText.text = "Hello, ${sessionManager.getFirstName() ?: ""} ${sessionManager.getLastName() ?: ""}"
        isActivityRequestFinished = false
        userActivities = null
        fetchActivityHistory(sessionManager.getUserId())
    }

    private fun fetchActivityHistory(userId: Int) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/ActivitiesController.php?user_id=$userId"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "success") {
                        userActivities = jsonObject.optJSONArray("data")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error parsing activity data", e)
                }
                isActivityRequestFinished = true
                updateUi()
            },
            { error ->
                Log.e("HomeFragment", "Volley error fetching activities: ${error.message}")
                isActivityRequestFinished = true
                updateUi()
            })
        request.setShouldCache(false)
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun updateUi() {
        val hasActivities = userActivities != null && userActivities!!.length() > 0
        if (!hasActivities) {
            binding.groupContentState.visibility = View.GONE
            binding.groupEmptyState.visibility = View.VISIBLE
            binding.chart.visibility = View.GONE
            return
        }
        binding.groupEmptyState.visibility = View.GONE
        binding.groupContentState.visibility = View.VISIBLE
        setupChart(userActivities!!)
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

        binding.chart.apply {
            visibility = View.VISIBLE
            data = BarData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(activityTypes)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = -45f
                textColor = Color.BLACK
            }
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.any { ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, 100)
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    fusedLocationClient.removeLocationUpdates(this)
                    binding.latitudeText.text = "Latitude: ${location.latitude}"
                    binding.longitudeText.text = "Longitude: ${location.longitude}"
                    getAddressFromLocation(location.latitude, location.longitude)
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())

            Thread {
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                    requireActivity().runOnUiThread {
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            var city = address.locality
                            if (city == null) {
                                city = address.subAdminArea
                            }
                            if (city == null) {
                                city = address.adminArea
                            }
                            val country = address.countryName

                            binding.cityText.text = "City: ${city ?: "N/A"}"
                            binding.countryText.text = "Country: ${country ?: "N/A"}"
                        } else {
                            binding.cityText.text = "City: N/A"
                            binding.countryText.text = "Country: N/A"
                        }
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        Log.e("HomeFragment", "Geocoder error", e)
                        binding.cityText.text = "City: Error"
                        binding.countryText.text = "Country: Error"
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e("HomeFragment", "Geocoder init error", e)
            binding.cityText.text = "City: Error"
            binding.countryText.text = "Country: Error"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        _binding = null
    }
}