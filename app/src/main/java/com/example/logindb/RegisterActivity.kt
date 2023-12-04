package com.example.logindb

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.example.logindb.Classes.User
import com.example.logindb.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)


        binding.registerbt.setOnClickListener {
            registerUser()
        }

        //  on sign in/up button switch to login activity
        val toLoginButton: Button = findViewById(R.id.register_cancel_bt)
        toLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
    private fun registerUser(){
        val username = binding.registerUsername.text.toString()
        val email = binding.registerEmail.text.toString()
        val password = binding.registerPassword.text.toString()
        val repPassword = binding.registerPasswordRepeat.text.toString()

        if(ValidationUtils.isTextNotEmpty(username) && ValidationUtils.isTextNotEmpty(email)
          && ValidationUtils.isTextNotEmpty(password) && ValidationUtils.isTextNotEmpty(repPassword)){
            if (ValidationUtils.isValidEmail(email) && password == repPassword && ValidationUtils.isValidUsername(username)){
                val user = User(username = username, email = email.trim(), password = password)
                db.registerUser(user)
                Toast.makeText(this,"User registered", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            }else if (password != repPassword){
                Toast.makeText(this, "Passwords not the same", Toast.LENGTH_SHORT).show()
            }else if (!ValidationUtils.isValidUsername(username)){
                Toast.makeText(this, "Wrong username format - [3,18], use char and digits only", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this, "Please input all fields", Toast.LENGTH_SHORT).show()
        }
    }
}