package com.example.fitnesstracker

import android.content.Intent
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
import org.json.JSONObject
import kotlin.toString

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

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

    private fun loginAction() {
        val username = binding.editLoginUsername.text.toString()
        val password = binding.editLoginPassword.text.toString()

        if(username.isEmpty()){
            binding.editLoginUsername.error = "Enter your username here..."
        }else if(password.isEmpty()){
            binding.editLoginPassword.error = "Enter your password here..."
        }else{
//            if(username == "su su" && password == "12345"){
//                Toast.makeText(context,"Login Successful", Toast.LENGTH_LONG).show()
//            }else{
//                Toast.makeText(context, "Login Failed", Toast.LENGTH_LONG).show()
//            }

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
                Log.d("Login", "***Response:$response")

                val obj = JSONObject(response)

                Toast.makeText(context, obj.getString("message"), Toast.LENGTH_LONG).show()

                if(obj.get("status") == "success"){
                    val user = obj.getJSONObject("user")
                    val st = "Hello, "+ user.getString("firstname")+
                            " "+user.getString("lastname")+
                            " "+user.getString("email")

                    Toast.makeText(context,st,Toast.LENGTH_LONG).show()

//                    val intent = Intent(context, UserActivity::class.java)
//                    intent.putExtra("id",user.getInt("id"))
//                    intent.putExtra("firstname", user.getString("firstname"))
//                    intent.putExtra("lastname", user.getString("lastname"))
//                    intent.putExtra("username", user.getString("username"))
//                    startActivity(intent)
//                    activity?.finish()
                }

            },
            Response.ErrorListener{
                    error->
                Log.d("LOgin", "***Error:$error")
            }
        ){
            override fun getParams(): Map<String, String>? {
                return mapOf("username" to username, "password" to password)
            }
        }

        Volley.newRequestQueue(context).add(result)
    }


}