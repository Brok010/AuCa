package com.example.logindb

import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.example.logindb.databinding.ActivityStudyBinding
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
    private lateinit var btnGood: Button
    private lateinit var btnBad: Button
    lateinit var modeSwitch: Switch
    private var bottomCardViewCheck: Boolean = false
    private lateinit var wakeLock: PowerManager.WakeLock
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "MyApp::StudyActivity"
        )
        wakeLock.acquire()

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
        modeSwitch = findViewById(R.id.mode_switch)
        btnGood = findViewById<Button>(R.id.btnGood)
        btnBad = findViewById<Button>(R.id.btnBad)

        // Load and display the initial card
        var cardId = displayTopCard(userId, deckId)

        // Set up button click listeners
        findViewById<LinearLayout>(R.id.cardTopContainer).setOnClickListener {
            releaseMediaPlayers()
            displayBottomCard(userId, deckId, cardId)
        }

        findViewById<LinearLayout>(R.id.cardBottomContainer).setOnClickListener {
            releaseMediaPlayers()
            displayBottomCard(userId, deckId, cardId)
        }

        btnGood.setOnClickListener {
            if (!::mediaPlayerBottom.isInitialized || !mediaPlayerBottom.isPlaying) {
                stopMediaPlayer()
                val newCardId = botCardCheck(userId, deckId, cardId, "Good")
                if (newCardId != 0) cardId = newCardId
            // if the player is playing
            }else {
                stopMediaPlayer()
                cardId = displayAnotherCardSequence(userId, deckId, cardId, "Good")
            }
        }

        findViewById<Button>(R.id.btnAverage).setOnClickListener {
            if (!::mediaPlayerBottom.isInitialized || !mediaPlayerBottom.isPlaying) {
                stopMediaPlayer()
                val newCardId = botCardCheck(userId, deckId, cardId, "Avg")
                if (newCardId != 0) cardId = newCardId
                // if the player is playing
            }else {
                stopMediaPlayer()
                cardId = displayAnotherCardSequence(userId, deckId, cardId, "Avg")
            }
        }

        btnBad.setOnClickListener {
            if (!::mediaPlayerBottom.isInitialized || !mediaPlayerBottom.isPlaying) {
                stopMediaPlayer()
                val newCardId = botCardCheck(userId, deckId, cardId, "Bad")
                if (newCardId != 0) cardId = newCardId
            }else {
                stopMediaPlayer()
                cardId = displayAnotherCardSequence(userId, deckId, cardId, "Bad")
            }
        }

        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            releaseMediaPlayers()
            finish()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val switchState = modeSwitch.isChecked

        return when {
            switchState && keyCode == KeyEvent.KEYCODE_VOLUME_UP -> {
                // Volume up button pressed and switch is ON, simulate a click on the "Good" button
                findViewById<Button>(R.id.btnGood).performClick()
                true
            }

            switchState && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Volume down button pressed and switch is ON, simulate a click on the "Bad" button
                findViewById<Button>(R.id.btnBad).performClick()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }
    override fun onDestroy() {
        wakeLock.release()
        super.onDestroy()
    }
    interface MediaPlayerCallback {
        fun onMediaPlayerFinished()
    }
    // checks if the new card was
    private fun botCardCheck (userId: Int, deckId: Int, cardId: Int, feedback: String): Int {
        var newCardId: Int = 0

        if (bottomCardViewCheck) {
            bottomCardViewCheck = false
            newCardId = displayAnotherCardSequence(userId, deckId, cardId, feedback)

        // if it hasnt been seen we show it and if there is a mediaplayer we play it
        } else {
            val mediaPlayerBot = displayBottomCard(userId, deckId, cardId)

            if (mediaPlayerBot.isPlaying) {
                // Set up a callback for when mediaPlayerBot finishes playing
                val mediaPlayerCallback = object : MediaPlayerCallback {
                    override fun onMediaPlayerFinished() {
                        // Code to execute after mediaPlayerBot finishes playing
                        newCardId = displayAnotherCardSequence(userId, deckId, cardId, feedback)
                    }
                }

                // Wait for mediaPlayerBot to finish playing using Handler
                val handler = Handler()
                handler.postDelayed({
                    // Notify the callback that mediaPlayerBot has finished playing
                    mediaPlayerCallback.onMediaPlayerFinished()
                }, mediaPlayerBot.duration.toLong())

            }else newCardId = displayAnotherCardSequence(userId, deckId, cardId, feedback)
        }
        return newCardId
    }
    private fun displayAnotherCardSequence(userId: Int, deckId: Int, cardId: Int, feedback: String): Int{
        releaseMediaPlayers()
        db.updateTimeout(userId, deckId, cardId, feedback)
        db.updateCoefficient(userId, deckId, cardId, feedback)
        if (db.getReadyCardCount(deckId) > 0) {
            // reset the bottom-view
            bottomCardView.text = ""
            playBtBottom.visibility = View.GONE
            // resets the top-view
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
                topCardView.text = ""
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

    private fun displayBottomCard(userId: Int, deckId: Int, cardId: Int): MediaPlayer {
        val card = db.getCardContents(userId, deckId, cardId)

        //set the text views
        if (card.cardBottom != "" && card.cardBottom != null) {
            bottomCardView.text = card.cardBottom
        } else {
            bottomCardView.text = ""
        }

        //set the player
        mediaPlayerBottom = MediaPlayer()
        val filepath = card.cardBottomFilePath

        if (filepath.isNotEmpty() && filepath != null && filepath != "") {
            playBtBottom.visibility = View.VISIBLE
            mediaPlayerSetup(card.cardBottomFilePath ,mediaPlayerBottom)
            playBtBottom.setOnClickListener{
                playBtClick(mediaPlayerBottom, card.cardBottomFilePath)
            }

        } else {
            playBtBottom.visibility = View.GONE
        }
        bottomCardViewCheck = true
        return mediaPlayerBottom
    }

    private fun playBtClick (mediaPlayer: MediaPlayer, path: String) {
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
    private fun releaseMediaPlayers() {
        if (::mediaPlayerTop.isInitialized) {
            if (mediaPlayerTop.isPlaying) {
                mediaPlayerTop.stop()
            }
            mediaPlayerTop.release()
            mediaPlayerTop = MediaPlayer()
        }

        if (::mediaPlayerBottom.isInitialized) {
            if (mediaPlayerBottom.isPlaying) {
                mediaPlayerBottom.stop()
            }
            mediaPlayerBottom.release()
            mediaPlayerBottom = MediaPlayer()
        }
    }
}