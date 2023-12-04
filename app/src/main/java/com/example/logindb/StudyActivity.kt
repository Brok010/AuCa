package com.example.logindb

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.logindb.databinding.ActivityStudyBinding
import java.io.File

class StudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudyBinding
    private lateinit var db:DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        val userId: Int = intent.getStringExtra("USER_ID")?.toIntOrNull() ?: -1
        val deckId: Int = intent.getStringExtra("DECK_ID")?.toIntOrNull() ?: -1

        if (userId == -1 || deckId == -1) {
            throw IllegalArgumentException("Invalid userId or deckId")
        }

        // Load and display the initial card
        var cardId = displayTopCard(userId, deckId)

        // Set up button click listeners
        findViewById<TextView>(R.id.top_card_view).setOnClickListener {
            displayBottomCard(userId, deckId, cardId)
        }

        findViewById<TextView>(R.id.bottom_card_view).setOnClickListener {
            displayBottomCard(userId, deckId, cardId)
        }

        findViewById<Button>(R.id.btnGood).setOnClickListener {
            cardId =  displayAnotherCardSequence(userId, deckId, cardId, "Good")
        }

        findViewById<Button>(R.id.btnAverage).setOnClickListener {
            cardId = displayAnotherCardSequence(userId, deckId, cardId, "Avg")
        }

        findViewById<Button>(R.id.btnBad).setOnClickListener {
            cardId = displayAnotherCardSequence(userId, deckId, cardId, "Bad")
        }

        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            finish()
        }
    }
    private fun displayAnotherCardSequence(userId: Int, deckId: Int, cardId: Int, feedback: String): Int{
        db.updateTimeout(userId, deckId, cardId, feedback)
        db.updateCoefficient(userId, deckId, cardId, feedback)
        if (db.getReadyCardCount(deckId) > 0) {
            return displayTopCard(userId, deckId)
        }else{
            finish()
            return 0
        }
    }
    private fun displayTopCard(userId: Int, deckId: Int) : Int{
        val db = DatabaseHelper(this) // Initialize the database helper
        var cardId = -1
        try {
            val card = db.getCardContents(userId, deckId)
             if (card != null) {
                if (card.cardTop is String) {
                    binding.topCardView.text = card.cardTop as String

                } else if (card.cardTop is File) {
                    val audioFile = card.cardTop as File

                    // TODO: Implement logic to play audio file using MediaPlayer or other audio player
                    // Example: playAudio(audioFile)
                }
                cardId = card.id
            }
        } finally {
            db.close() // Close the database in a finally block to ensure it's always closed
            return cardId
        }
    }

    private fun displayBottomCard(userId: Int, deckId: Int, cardId: Int) {
        val db = DatabaseHelper(this) // Initialize the database helper

        try {
            val card = db.getCardContents(userId, deckId, cardId)

            if (card.cardBottom is String) {
                binding.bottomCardView.text = card.cardBottom as String

            } else if (card.cardBottom is File) {
                val audioFile = card.cardBottom as File

                // TODO: Implement logic to play audio file using MediaPlayer or other audio player
                // Example: playAudio(audioFile)
            }
        } finally {
            db.close() // Close the database in a finally block to ensure it's always closed
        }
    }
}