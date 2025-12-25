package com.example.fitnesstracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.navigation.fragment.findNavController
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentRegisterBinding
import org.json.JSONObject

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.txtGotoLogin.setText(
            HtmlCompat.fromHtml("<u>Click Here to Login...</u>",
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        )

        binding.txtGotoLogin.setOnClickListener {
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLoginFragment())
        }

        binding.btnSingUpClear.setOnClickListener {
            binding.apply {
                editTextFirstName.setText("")
                editTextLastName.setText("")
                editTextEmail.setText("")
                editTextUsername.setText("")
                editTextPassword.setText("")
                editTextWeight.setText("")
                editTextHeight.setText("")
                editTextAge.setText("")
                editTextGender.setText("")
            }
        }

        binding.btnSignUp.setOnClickListener {
            signUpAction()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun signUpAction() {
        val firstname = binding.editTextFirstName.text.toString()
        val lastname = binding.editTextLastName.text.toString()
        val email = binding.editTextEmail.text.toString()
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()
        val weight = binding.editTextWeight.text.toString()
        val height = binding.editTextHeight.text.toString()
        val age = binding.editTextAge.text.toString()
        val gender = binding.editTextGender.text.toString()

        if(firstname.isEmpty()){
            binding.editTextFirstName.error = "Enter your firstname here..."
        }else if(lastname.isEmpty()){
            binding.editTextLastName.error = "Enter your lastname here..."
        }else if(email.isEmpty()){
            binding.editTextEmail.error = "Enter your email here..."
        }else if(username.isEmpty()){
            binding.editTextUsername.error = "Enter your username here..."
        }else if(password.isEmpty()){
            binding.editTextPassword.error = "Enter your password here..."
        }else if(weight.isEmpty()){
            binding.editTextWeight.error = "Enter your weight here..."
        }else if(height.isEmpty()){
            binding.editTextHeight.error = "Enter your height here..."
        }else if(age.isEmpty()){
            binding.editTextAge.error = "Enter your age here..."
        }else if(gender.isEmpty()){
            binding.editTextGender.error = "Enter your gender here..."
        }else{
            registerUser(firstname, lastname, email, username, password, weight, height, age, gender)
        }
    }

    private fun registerUser(
        firstname: String,
        lastname: String,
        email: String,
        username: String,
        password: String,
        weight: String,
        height: String,
        age: String,
        gender: String
    ) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/register_controller.php"
        val request = object:StringRequest(Method.POST, url,
            Response.Listener{
                    response->
                if (_binding == null) return@Listener
                Log.d("Sign Up", "***Response:$response")

                val obj = JSONObject(response)
                if(obj.get("status") == "success"){
                    Toast.makeText(context,obj.get("message").toString(),Toast.LENGTH_LONG).show()
                    findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLoginFragment())

                }else if (obj.get("status") == "existed"){
                    showAlert(obj.get("message").toString())
                }
            },
            Response.ErrorListener{
                    error->
                if (_binding == null) return@ErrorListener
                Log.d("Sign Up", "***Error:$error")
            }){
            override fun getParams(): Map<String, String> {
                return mapOf("fname" to firstname,
                    "lname" to lastname,
                    "email" to email,
                    "username" to username,
                    "password" to password,
                    "weight" to weight,
                    "height" to height,
                    "age" to age,
                    "gender" to gender)
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showAlert(msg: String) {
        val context = context ?: return
        val alert = AlertDialog.Builder(context)
        alert.setTitle("Warning")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK"){
                    dialog, _ -> dialog.dismiss()
            }

        val alertDialog = alert.create()
        alertDialog.show()
    }
}