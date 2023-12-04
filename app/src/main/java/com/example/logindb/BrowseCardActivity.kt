package com.example.logindb

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.logindb.databinding.ActivityCardsBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BrowseCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardsBinding
    private lateinit var db:DatabaseHelper
    private lateinit var cardAdapter: CardAdapter
    private var userId: Int = -1
    private var deckId: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        userId = intent.getStringExtra("USER_ID")?.toIntOrNull() ?: -1
        deckId = intent.getStringExtra("DECK_ID")?.toIntOrNull() ?: -1

        if (userId == -1 || deckId == -1) {
            throw IllegalArgumentException("Invalid userId or deckId")
        }


        //body - deck table
        var deckName = db.getDeckName(userId, deckId)
        binding.tvPrimaryAccount.text = "$deckName"

        cardAdapter = CardAdapter(mutableListOf(), userId, deckId, db, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BrowseCardActivity)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = cardAdapter
        }
        // Load the initial card list
        db.updateCardTimeouts(userId)
        val initialCardList = db.getAllCards(userId, deckId)
        cardAdapter.updateData(initialCardList)

        //add button
        val addCardButton: FloatingActionButton = findViewById(R.id.Add_card_bt)
        addCardButton.setOnClickListener {
            val intent = Intent(this, CardAddActivity::class.java)
            intent.putExtra("DECK_ID", deckId.toString())
            intent.putExtra("USER_ID", userId.toString())
            startActivity(intent)
        }

        //logout button
        val backButton: Button = findViewById(R.id.backbt)
        backButton.setOnClickListener {
            finish()
        }
    }
    override fun onResume() {
        super.onResume()
        db.updateCardTimeouts(userId)
        val updatedCardList = db.getAllCards(userId, deckId)
        cardAdapter.updateData(updatedCardList)
    }
}