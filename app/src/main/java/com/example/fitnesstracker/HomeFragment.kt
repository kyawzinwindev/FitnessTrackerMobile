package com.example.fitnesstracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.fitnesstracker.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.btnLogin.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        binding.btnRegister.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

        return binding.root
    }

}