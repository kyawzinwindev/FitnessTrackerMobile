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
import kotlin.toString

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.txtGotoLogin.setText(
            HtmlCompat.fromHtml("<u>Click Here to Login...</u>",
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        )

        binding.txtGotoLogin.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        binding.btnSingUpClear.setOnClickListener {
            binding.apply {
                editTextFirstName.setText("")
                editTextLastName.setText("")
                editTextEmail.setText("")
                editTextUsername.setText("")
                editTextPassword.setText("")
            }
        }

        binding.btnSignUp.setOnClickListener {
            signUpAction()
        }

        return binding.root
    }

    private fun signUpAction() {
        val firstname = binding.editTextFirstName.text.toString()
        val lastname = binding.editTextLastName.text.toString()
        val email = binding.editTextEmail.text.toString()
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()

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
        }else{
//            Toast.makeText(context,"Register Successful", Toast.LENGTH_LONG).show()
//            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
//            findNavController().navigate(action)

            registerUser(firstname, lastname, email, username, password)
        }
    }

    private fun registerUser(
        firstname: String,
        lastname: String,
        email: String,
        username: String,
        password: String
    ) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/register_controller.php"
        val request = object:StringRequest(Method.POST, url,
            Response.Listener{
                    response->
                Log.d("Sign Up", "***Response:$response")

                val obj = JSONObject(response)
                if(obj.get("status") == "success"){
                    Toast.makeText(context,obj.get("message").toString(),Toast.LENGTH_LONG).show()

                    val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
                    findNavController().navigate(action)

                }else if (obj.get("status") == "existed"){
                    showAlert(obj.get("message").toString())
                }
            },
            Response.ErrorListener{
                    error->
                Log.d("Sign Up", "***Error:$error")
            }){
            override fun getParams(): Map<String, String>? {
                return mapOf("fname" to firstname,
                    "lname" to lastname,
                    "email" to email,
                    "username" to username,
                    "password" to password)
            }
        }
        Volley.newRequestQueue(context).add(request)
        Log.d("Sign Up Request", "***Register User")
    }

    private fun showAlert(msg: String) {
        val alert = AlertDialog.Builder(requireContext())
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