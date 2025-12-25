package com.example.fitnesstracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.fitnesstracker.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val sessionManager = SessionManager(requireContext())
        val userDetails = sessionManager.getUserDetails()

        binding.fullNameTextView.text = "${userDetails[SessionManager.FNAME]} ${userDetails[SessionManager.LNAME]}"
        binding.emailTextView.text = userDetails[SessionManager.EMAIL].toString()
        binding.usernameTextView.text = userDetails[SessionManager.USERNAME].toString()
        binding.weightTextView.text = "Weight: ${userDetails[SessionManager.WEIGHT]} kg"
        binding.heightTextView.text = "Height: ${userDetails[SessionManager.HEIGHT]} cm"
        binding.ageTextView.text = "Age: ${userDetails[SessionManager.AGE]}"
        binding.genderTextView.text = "Gender: ${userDetails[SessionManager.GENDER]}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
