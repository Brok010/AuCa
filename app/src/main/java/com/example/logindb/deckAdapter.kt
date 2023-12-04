package com.example.logindb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.logindb.Classes.Deck

class DeckAdapter(private val deckList: MutableList<Deck>,
                  private val userId: Long,
                  private val db: DatabaseHelper,
                  private val deckActivity: DeckActivity
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    fun updateData(newDeckList: List<Deck>) {
        deckList.clear()
        deckList.addAll(newDeckList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck_view, parent, false)
        return DeckViewHolder(itemView, userId)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val currentDeck = deckList[position]
        holder.bind(currentDeck)

        holder.itemView.setOnLongClickListener{
            showPopupMenu(holder.itemView, currentDeck, userId)
            true
        }

        holder.itemView.setOnClickListener {
            if (db.getAllCards(userId.toInt(), currentDeck.id).isEmpty()){
                deckActivity.startAddCardActivity(currentDeck.id, userId)
            } else if (db.getReadyCardCount(currentDeck.id) > 0) {
                deckActivity.startStudyActivity(currentDeck.id, userId)
            }else{
                deckActivity.startBrowseCardActivity(currentDeck.id, userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return deckList.size
    }

    inner class DeckViewHolder(itemView: View, private val userId: Long) : RecyclerView.ViewHolder(itemView) {
        private val tvDeckName: TextView = itemView.findViewById(R.id.tv_value_deckname)
        private val tvCardCount: TextView = itemView.findViewById(R.id.tv_value_card_count)

        fun bind(deck: Deck) {
            tvDeckName.text = deck.deckName
            tvCardCount.text = db.getReadyCardCount(deck.id).toString()
        }
    }
    private fun showPopupMenu(view: View, deck: Deck, userId: Long) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.deck_menu, popupMenu.menu)

        val deckId = deck.id

        // Set a click listener for each menu item
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {

                    // delete deck from database
                    db.removeDeck(userId, deckId)
                    //update the view
                    val dbdeckList = db.getAllDecks(userId)
                    updateData(dbdeckList)
                    true
                }
                R.id.menu_browse -> {
                    deckActivity.startBrowseCardActivity(deck.id, userId)
                    true
                }
                R.id.menu_rename -> {
                    // new name pop up window
                    showRenameDialog(view.context, userId, deck.id)
                    true
                }
                R.id.menu_add -> {
                    deckActivity.startAddCardActivity(deck.id, userId)
                    true
                }
                R.id.menu_custom_study -> {
                    // Handle custom study option
                    // Implement your logic here
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
    private fun showRenameDialog(context: Context, userId: Long, deckId: Int) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.deck_name_dialog, null)

        val newNameEditText: EditText = dialogView.findViewById(R.id.deckNameEditText)

        builder.setView(dialogView)
            .setTitle("Enter New Name for the Deck")
            .setPositiveButton("OK") { _, _ ->
                // Handle OK button click
                val newDeckName = newNameEditText.text.toString()

                // Validate if the new name is not empty
                if (newDeckName.isNotEmpty()) {
                    // Call the renameDeck function
                    db.renameDeck(newDeckName, userId, deckId)

                    // Update the view with the new deck list
                    val dbDeckList = db.getAllDecks(userId)
                    updateData(dbDeckList)
                } else {
                    // Show an error message or handle empty name case
                    Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Handle Cancel button click
                dialog.cancel()
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }
}
