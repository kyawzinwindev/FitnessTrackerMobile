package com.example.fitnesstracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.navigation.fragment.findNavController
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fitnesstracker.databinding.FragmentLoginBinding
import org.json.JSONException
import org.json.JSONObject

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.txtGotoSignUp.setText(
            HtmlCompat.fromHtml("<u>Click Here Go to Sign Up...</u>",
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        )

        binding.txtGotoSignUp.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

        binding.btnLogIn.setOnClickListener {
            loginAction()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loginAction() {
        val username = binding.editLoginUsername.text.toString()
        val password = binding.editLoginPassword.text.toString()

        if(username.isEmpty()){
            binding.editLoginUsername.error = "Enter your username here..."
        }else if(password.isEmpty()){
            binding.editLoginPassword.error = "Enter your password here..."
        }else{
            loginUser(username,password)
        }
    }

    private fun loginUser(username: String, password: String) {
        val url = "http://10.0.2.2:81/FitnessTrackerAPI/controllers/login_controller.php"
        val result = object:StringRequest(
            Method.POST,
            url,
            Response.Listener{
                    response->
                if (_binding == null) return@Listener
                Log.d("Login", "***Response:$response")
                try {
                    val obj = JSONObject(response)

                    Toast.makeText(context, obj.getString("message"), Toast.LENGTH_LONG).show()

                    if(obj.getString("status") == "success"){
                        val user = obj.getJSONObject("user")
                        val sessionManager = SessionManager(requireContext())
                        sessionManager.saveUser(user)

                        val action = LoginFragmentDirections.actionLoginFragmentToHomeFragment()
                        findNavController().navigate(action)
                    }
                } catch (e: JSONException) {
                    Log.e("Login", "Error parsing JSON", e)
                    Toast.makeText(context, "An error occurred.", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener{
                    error->
                if (_binding == null) return@ErrorListener
                Log.d("Login", "***Error:$error")
                Toast.makeText(context, "Login failed. Check your connection.", Toast.LENGTH_LONG).show()
            }
        ){
            override fun getParams(): Map<String, String>? {
                return mapOf("username" to username, "password" to password)
            }
        }

        Volley.newRequestQueue(requireContext()).add(result)
    }


}