package com.example.logindb

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.logindb.databinding.ActivityDeckBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DeckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeckBinding
    private lateinit var db:DatabaseHelper
    private lateinit var deckAdapter: DeckAdapter
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //header
        val extraUsername = intent.getStringExtra("USERNAME_DATA")
        userId = intent.getLongExtra("USER_ID", -1)
        //body - deck table
        binding.tvPrimaryAccount.text = "$extraUsername"
        db = DatabaseHelper(this)
        deckAdapter = DeckAdapter(mutableListOf(), userId, db, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DeckActivity)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = deckAdapter
        }
        db.updateCardTimeouts(userId.toInt())

        // Load the initial deck list
        val initialDeckList = db.getAllDecks(userId)
        deckAdapter.updateData(initialDeckList)

        //add button
        val plusButton: FloatingActionButton = findViewById(R.id.Addbt)
        plusButton.setOnClickListener {
            showDeckNameDialog(userId)
        }
        //logout button
        val logoutButton: Button = findViewById(R.id.logoutbt)
        logoutButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        db.updateCardTimeouts(userId.toInt())
        val userId = intent.getLongExtra("USER_ID", -1)
        val updatedDeckList = db.getAllDecks(userId)
        deckAdapter.updateData(updatedDeckList)
    }

    private fun showDeckNameDialog(userId: Long) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.deck_name_dialog, null)

        val deckNameEditText: EditText = dialogView.findViewById(R.id.deckNameEditText)

        builder.setView(dialogView)
            .setTitle("Enter Name of Deck")
            .setPositiveButton("OK") { _, _ ->
                // Handle OK button click
                val deckName = deckNameEditText.text.toString()
                db.addDeck(deckName, userId)
                Toast.makeText(this, "Deck added", Toast.LENGTH_SHORT).show()

                val newDeckList = db.getAllDecks(userId)
                deckAdapter.updateData(newDeckList)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Handle Cancel button click
                dialog.cancel()
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }
    fun startAddCardActivity(deckId: Int, userId: Long) {
        val intent = Intent(this, CardAddActivity::class.java)
        intent.putExtra("DECK_ID", deckId.toString())
        intent.putExtra("USER_ID", userId.toString())
        startActivity(intent)
    }
    fun startStudyActivity(deckId: Int, userId: Long){
        val intent = Intent(this, StudyActivity::class.java)
        intent.putExtra("DECK_ID", deckId.toString())
        intent.putExtra("USER_ID", userId.toString())
        startActivity(intent)
    }
    fun startBrowseCardActivity(deckId: Int, userId: Long){
        val intent = Intent(this, BrowseCardActivity::class.java)
        intent.putExtra("DECK_ID", deckId.toString())
        intent.putExtra("USER_ID", userId.toString())
        startActivity(intent)
    }
}