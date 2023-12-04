package com.example.logindb

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.logindb.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        //  on sign in/up button switch to login activity
        val toRegisterButton: TextView = findViewById(R.id.login_txw)
        toRegisterButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        binding.loginBt.setOnClickListener {
            loginUser()
        }
        binding.quitBt.setOnClickListener {
            finishAffinity()
        }
    }

    private fun loginUser(){
        val username = binding.loginUsername.text.toString().trim()
        val password = binding.loginPass.text.toString().trim()

        if(ValidationUtils.isTextNotEmpty(username) && ValidationUtils.isTextNotEmpty(password)){
            if (ValidationUtils.isValidUsername(username)){
                val (loginSuccess, userId) = db.loginUser(username, password)
                if (loginSuccess){
                    val i = (Intent(this, DeckActivity::class.java))
                    i.putExtra("USERNAME_DATA", username)
                    userId?.let { i.putExtra("USER_ID", it) }
                    startActivity(i)

                    // clears the texts for another potential login
                    binding.loginUsername.text.clear()
                    binding.loginPass.text.clear()
                }else{
                    Toast.makeText(this, "Wrong password/email", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Invalid name format - 3-18 alphabet/number chars", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
        }
    }
}