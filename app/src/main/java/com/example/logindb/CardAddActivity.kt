//decided to make this an activity and not just a popup window cause in an ideal application this would have a lot of functionality
package com.example.logindb


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.logindb.databinding.ActivityAddCardBinding
import java.io.File
import java.io.FileInputStream
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

        val cardFilesDirectory = File(filesDir, CARD_FILES_DIRECTORY_NAME)
        if (!cardFilesDirectory.exists()) {
            cardFilesDirectory.mkdirs()
        }

        addCardButton.setOnClickListener {
            addCardHandler(userId, deckId, pathTop, pathBottom)
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
            //TODO: delete any files that were selected
            finish()
        }
    }


    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100)
        } catch (exception: Exception) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show()
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data

            uri?.let {
                val path: String? = if (isContentUri(it)) {
                    copyFileFromContentUri(it)
                } else {
                    it.path
                }
                if (path != null) {
                    when (position) {
                        "top" -> {
                            pathTop = path
                        }
                        "bottom" -> {
                            pathBottom = path
                        }
                    }
                } else {
                    Toast.makeText(this, "File path could not be resolved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun isContentUri(uri: Uri): Boolean {
        return "content" == uri.scheme
    }

    // takes the file and copies it into my apps dir
    private fun copyFileFromContentUri(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri)
        val fileExtension = getFileExtension(uri) ?: ""
        val fileName = "${System.currentTimeMillis()}.$fileExtension"
        val outputFile = File(filesDir, fileName)
        val outputStream = FileOutputStream(outputFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return outputFile.absolutePath
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    fun addCardHandler(
        userId: Int,
        deckId: Int,
        pathTop: String,
        pathBottom: String,
    ) {
        val cardTopText: EditText = findViewById(R.id.et_top_txt)
        val cardBottomText: EditText = findViewById(R.id.et_bottom_txt)
        val topText = cardTopText.text.toString().trim()
        val bottomText = cardBottomText.text.toString().trim()
        var topCard: String = ""
        var bottomCard: String = ""


        // Check if either path or text is provided
        if (pathTop.isEmpty() && topText.isEmpty()) {
            Toast.makeText(this, "Please provide information for Card Top", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (pathBottom.isEmpty() && bottomText.isEmpty()) {
            Toast.makeText(this, "Please provide information for Card Bottom", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (pathTop.isEmpty() && topText.isEmpty() && pathBottom.isEmpty() && bottomText.isEmpty()) {
            Toast.makeText(this, "Provide more information", Toast.LENGTH_SHORT).show()
            return
        }
        //if there is txt
        if (topText.isNotEmpty()){
            topCard = topText
        }
        if (bottomText.isNotEmpty()) {
            bottomCard = bottomText
        }

        if (topCard != null && bottomCard != null) {
            db.addCard(userId, deckId, topCard, bottomCard, pathTop, pathBottom)
        }
    }


    companion object {
        private const val CARD_FILES_DIRECTORY_NAME = "CardFiles"
    }
}
