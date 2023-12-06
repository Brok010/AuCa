package com.example.logindb

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.logindb.databinding.ActivityStudyBinding
import java.io.File
import java.io.IOException

class StudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudyBinding
    private lateinit var db:DatabaseHelper
    private lateinit var topCardContainer: LinearLayout
    private lateinit var bottomCardContainer: LinearLayout
    private lateinit var topCardView: TextView
    private lateinit var bottomCardView: TextView
    private lateinit var playBtTop: ImageButton
    private lateinit var playBtBottom: ImageButton
    private lateinit var mediaPlayerTop: MediaPlayer
    private lateinit var mediaPlayerBottom: MediaPlayer
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

        topCardContainer = findViewById(R.id.cardTopContainer)
        bottomCardContainer = findViewById(R.id.cardBottomContainer)
        topCardView = findViewById(R.id.top_card_view)
        bottomCardView = findViewById(R.id.bottom_card_view)
        playBtTop= findViewById(R.id.play_bt_top)
        playBtBottom= findViewById(R.id.play_bt_bot)


        // Load and display the initial card
        var cardId = displayTopCard(userId, deckId)

        // Set up button click listeners
        findViewById<LinearLayout>(R.id.cardTopContainer).setOnClickListener {
            stopMediaPlayer()
            displayBottomCard(userId, deckId, cardId)
        }

        findViewById<LinearLayout>(R.id.cardBottomContainer).setOnClickListener {
            stopMediaPlayer()
            displayBottomCard(userId, deckId, cardId)
        }

        findViewById<Button>(R.id.btnGood).setOnClickListener {
            stopMediaPlayer()
            cardId =  displayAnotherCardSequence(userId, deckId, cardId, "Good")
        }

        findViewById<Button>(R.id.btnAverage).setOnClickListener {
            stopMediaPlayer()
            cardId = displayAnotherCardSequence(userId, deckId, cardId, "Avg")

        }

        findViewById<Button>(R.id.btnBad).setOnClickListener {
            stopMediaPlayer()
            cardId = displayAnotherCardSequence(userId, deckId, cardId, "Bad")
        }

        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            stopMediaPlayer()
            finish()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Volume up button pressed, simulate a click on the "Good" button
                findViewById<Button>(R.id.btnGood).performClick()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Volume down button pressed, simulate a click on the "Bad" button
                findViewById<Button>(R.id.btnBad).performClick()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
        val card = db.getCardContents(userId, deckId) // gets a card that has timeout on 0

        //set the text views
        if (card != null) {
            if (card.cardTop != "" && card.cardTop != null) {
                topCardView.text = card.cardTop
            } else {
                topCardView.text = "None"
            }
            // set the player
            val filepath = card.cardTopFilePath
            if (filepath.isNotEmpty() && filepath != null && filepath != "") {
                mediaPlayerTop = MediaPlayer()
                mediaPlayerSetup(card.cardTopFilePath ,mediaPlayerTop)
                //set bt
                playBtTop.visibility = View.VISIBLE
                playBtTop.setOnClickListener{
                    playBtClick(mediaPlayerTop, card.cardTopFilePath)
                }

            } else {
                playBtTop.visibility = View.GONE
            }

            return card.id
        } else {
            // Handle the case where card is null
            throw Exception("Card is null")
        }
    }

    private fun displayBottomCard(userId: Int, deckId: Int, cardId: Int) {
        val card = db.getCardContents(userId, deckId, cardId)

        //set the text views
        if (card.cardBottom != "" && card.cardBottom != null) {
            bottomCardView.text = card.cardBottom
        } else {
            bottomCardView.text = "None"
        }

        //set the player
        val filepath = card.cardBottomFilePath
        if (filepath.isNotEmpty() && filepath != null && filepath != "") {
            playBtBottom.visibility = View.VISIBLE
            mediaPlayerBottom = MediaPlayer()

            // if top player is running stop it
            stopMediaPlayer()
            mediaPlayerSetup(card.cardBottomFilePath ,mediaPlayerBottom)
            playBtBottom.setOnClickListener{
                playBtClick(mediaPlayerBottom, card.cardBottomFilePath)
            }

        } else {
            playBtBottom.visibility = View.GONE
        }
    }

    fun playBtClick (mediaPlayer: MediaPlayer, path: String) {
        stopMediaPlayer()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(path)
        mediaPlayer.prepare()
        mediaPlayer.start()
    }
    private fun stopMediaPlayer() {
        if (::mediaPlayerTop.isInitialized && mediaPlayerTop.isPlaying) {
            mediaPlayerTop.stop()
        }
        if (::mediaPlayerBottom.isInitialized && mediaPlayerBottom.isPlaying) {
            mediaPlayerBottom.stop()
        }
    }
    private fun mediaPlayerSetup(filepath: String, mediaPlayer: MediaPlayer ){
        try {

            // Set the data source to the file path
            mediaPlayer.setDataSource(filepath)

            // Prepare and start the media player
            mediaPlayer.prepare()
            mediaPlayer.start()

        } catch (e: IOException) {
            // Handle the exception, e.g., file not found or unable to play
            e.printStackTrace()
        }
    }

}