package com.example.fitnesstracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentEditProfileBinding
import org.json.JSONObject

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        binding.updateProfileButton.setOnClickListener { updateProfile() }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(requireContext())
        val userDetails = sessionManager.getUserDetails()

        binding.firstnameEditText.setText(userDetails[SessionManager.FNAME].toString())
        binding.lastnameEditText.setText(userDetails[SessionManager.LNAME].toString())
        binding.emailEditText.setText(userDetails[SessionManager.EMAIL].toString())
        binding.usernameEditText.setText(userDetails[SessionManager.USERNAME].toString())
        binding.weightEditText.setText(userDetails[SessionManager.WEIGHT].toString())
        binding.heightEditText.setText(userDetails[SessionManager.HEIGHT].toString())
        binding.ageEditText.setText(userDetails[SessionManager.AGE].toString())
        binding.genderEditText.setText(userDetails[SessionManager.GENDER].toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateProfile() {
        val sessionManager = SessionManager(requireContext())
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/UserProfileUpdateController.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                if (_binding == null) return@Listener
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optString("status") == "success") {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        // Update the session with the latest user data
                        val updatedUser = JSONObject()
                        updatedUser.put("firstname", binding.firstnameEditText.text.toString())
                        updatedUser.put("lastname", binding.lastnameEditText.text.toString())
                        updatedUser.put("email", binding.emailEditText.text.toString())
                        updatedUser.put("username", binding.usernameEditText.text.toString())
                        updatedUser.put("weight", binding.weightEditText.text.toString().toDouble())
                        updatedUser.put("height", binding.heightEditText.text.toString().toDouble())
                        updatedUser.put("age", binding.ageEditText.text.toString().toInt())
                        updatedUser.put("gender", binding.genderEditText.text.toString())
                        sessionManager.saveUser(updatedUser)
                    } else {
                        Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("UpdateProfile", "Error parsing response", e)
                    Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                if (_binding == null) return@ErrorListener
                Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show()
                Log.e("UpdateProfile", error.toString())
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = sessionManager.getUserId().toString()
                params["firstname"] = binding.firstnameEditText.text.toString()
                params["lastname"] = binding.lastnameEditText.text.toString()
                params["email"] = binding.emailEditText.text.toString()
                params["username"] = binding.usernameEditText.text.toString()
                params["weight"] = binding.weightEditText.text.toString()
                params["height"] = binding.heightEditText.text.toString()
                params["age"] = binding.ageEditText.text.toString()
                params["gender"] = binding.genderEditText.text.toString()
                if (binding.passwordEditText.text.isNotEmpty()) {
                    params["password"] = binding.passwordEditText.text.toString()
                }
                return params
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }
}
