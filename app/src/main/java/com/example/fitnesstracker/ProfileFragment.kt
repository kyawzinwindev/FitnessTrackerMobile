package com.example.fitnesstracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentProfileBinding
import org.json.JSONObject

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())

        binding.updateProfileButton.setOnClickListener {
            updateProfile()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val user = sessionManager.getUserDetails()

        binding.firstNameEditText.setText(user[SessionManager.FNAME]?.toString() ?: "")
        binding.lastNameEditText.setText(user[SessionManager.LNAME]?.toString() ?: "")
        binding.emailEditText.setText(user[SessionManager.EMAIL]?.toString() ?: "")
        binding.usernameEditText.setText(user[SessionManager.USERNAME]?.toString() ?: "")
        binding.weightEditText.setText(user[SessionManager.WEIGHT]?.toString() ?: "")
        binding.heightEditText.setText(user[SessionManager.HEIGHT]?.toString() ?: "")
        binding.ageEditText.setText(user[SessionManager.AGE]?.toString() ?: "")

        when (user[SessionManager.GENDER]) {
            "Male" -> binding.radioMale.isChecked = true
            "Female" -> binding.radioFemale.isChecked = true
            "Other" -> binding.radioOther.isChecked = true
        }
    }

    private fun getSelectedGender(): String {
        return when (binding.genderRadioGroup.checkedRadioButtonId) {
            R.id.radioMale -> "Male"
            R.id.radioFemale -> "Female"
            R.id.radioOther -> "Other"
            else -> ""
        }
    }

    private fun updateProfile() {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/UserProfileUpdateController.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                if (_binding == null) return@Listener
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optString("status") == "success") {

                        val updatedUser = JSONObject()
                        updatedUser.put("firstname", binding.firstNameEditText.text.toString())
                        updatedUser.put("lastname", binding.lastNameEditText.text.toString())
                        updatedUser.put("email", binding.emailEditText.text.toString())
                        updatedUser.put("username", binding.usernameEditText.text.toString())
                        updatedUser.put("weight", binding.weightEditText.text.toString())
                        updatedUser.put("height", binding.heightEditText.text.toString())
                        updatedUser.put("age", binding.ageEditText.text.toString())
                        updatedUser.put("gender", getSelectedGender())

                        sessionManager.saveUser(updatedUser)

                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("UpdateProfile", e.toString())
                    Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener {
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = sessionManager.getUserId().toString()
                params["firstname"] = binding.firstNameEditText.text.toString()
                params["lastname"] = binding.lastNameEditText.text.toString()
                params["email"] = binding.emailEditText.text.toString()
                params["username"] = binding.usernameEditText.text.toString()
                params["weight"] = binding.weightEditText.text.toString()
                params["height"] = binding.heightEditText.text.toString()
                params["age"] = binding.ageEditText.text.toString()
                params["gender"] = getSelectedGender()

                if (binding.passwordEditText.text.isNotEmpty()) {
                    params["password"] = binding.passwordEditText.text.toString()
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
