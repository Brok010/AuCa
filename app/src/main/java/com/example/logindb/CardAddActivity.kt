//decided to make this an activity and not just a popup window cause in an ideal application this would have a lot of functionality
package com.example.logindb


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.logindb.databinding.ActivityAddCardBinding
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class CardAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCardBinding
    private var position: String? = null
    private lateinit var db:DatabaseHelper
    private var userId: Int? = null
    private var deckId: Int? = null
    private var pathTop: String = ""
    private var pathBottom: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId: Int = intent.getStringExtra("USER_ID")?.toIntOrNull() ?: -1
        val deckId: Int = intent.getStringExtra("DECK_ID")?.toIntOrNull() ?: -1

        if (userId == -1 || deckId == -1) {
            throw IllegalArgumentException("Invalid userId or deckId")
        }
        db = DatabaseHelper(this)

        val topFileImageButton: ImageButton = findViewById(R.id.file_image_top)
        val bottomFileImageButton: ImageButton = findViewById(R.id.file_image_bottom)
        val addCardButton: Button = findViewById(R.id.Add_card_bt)
        val cancelButton: Button = findViewById(R.id.cancel_button)

        val cardTopText: EditText = findViewById(R.id.et_top_txt)
        val cardBottomText: EditText = findViewById(R.id.et_bottom_txt)


        addCardButton.setOnClickListener {
            val topText = cardTopText.text.toString()
            val bottomText = cardBottomText.text.toString()

            //check if any file paths are loaded
            var topCard: Any? = null
            var bottomCard: Any? = null

            if (pathTop.isNotEmpty()) {
                // Load the file corresponding to the path
                val file = File(pathTop)
                if (file.exists()) {
                    topCard = file
                } else {
                    Toast.makeText(this, "Top file does not exist", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else if (topText.isNotEmpty()) {
                topCard = topText
            } else {
                Toast.makeText(this, "Top is not filled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pathBottom.isNotEmpty()) {
                // Load the file corresponding to the path
                val file = File(pathBottom)
                if (file.exists()) {
                    bottomCard = file
                } else {
                    Toast.makeText(this, "Bottom file does not exist", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else if (bottomText.isNotEmpty()) {
                bottomCard = bottomText
            } else {
                Toast.makeText(this, "Bottom is not filled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the addCard function with topCard and bottomCard
            db.addCard(userId, deckId, topCard, bottomCard)
            db.incrementCardCount(deckId)

            //exit activity last
            db.updateCardTimeouts(userId)
            finish()
        }

        topFileImageButton.setOnClickListener {
            position = "top"
            showFileChooser()

        }
        bottomFileImageButton.setOnClickListener {
            position = "bottom"
            showFileChooser()

        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data

            uri?.let {
                val path: String? = if (isContentUri(it)) {
                    copyFileFromContentUri(it)
                } else {
                    it.path
                }

                if (path != null) {
                    val file = File(path)
                    when (position) {
                        "top" -> {
                            binding.twFileNameTop.text = "Path: $path File name: ${file.name}".trimIndent()
                            pathTop = path
                        }
                        "bottom" -> {
                            binding.twFileNameBottom.text = "Path: $path File name: ${file.name}".trimIndent()
                            pathBottom = path
                        }
                    }
                } else {
                    Toast.makeText(this, "File path could not be resolved", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100)
        }catch (exception: Exception) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isContentUri(uri: Uri): Boolean {
        return "content" == uri.scheme
    }

    private fun copyFileFromContentUri(contentUri: Uri): String? {
        val inputStream = contentResolver.openInputStream(contentUri)
        val fileName = "${System.currentTimeMillis()}.mp3" // or use appropriate file extension
        val outputFile = File(cacheDir, fileName)
        val outputStream = FileOutputStream(outputFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return outputFile.absolutePath
    }
}