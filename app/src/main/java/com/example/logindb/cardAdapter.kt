package com.example.logindb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.logindb.Classes.Card
import java.io.File

class CardAdapter(private val cardList: MutableList<Card>,
                  private val userId: Int,
                  private val deckId: Int,
                  private val db: DatabaseHelper,
                  private val browseCardActivity: BrowseCardActivity

) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {
    
    fun updateData(newCardList: List<Card>) {
        cardList.clear()
        cardList.addAll(newCardList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_view, parent, false)
        return CardViewHolder(itemView, userId)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val currentCard = cardList[position]
        holder.bind(currentCard)

        holder.itemView.setOnLongClickListener{
            showPopupMenu(holder.itemView, currentCard, userId, deckId)
            true
        }

        holder.itemView.setOnClickListener {
            //
        }
    }

    override fun getItemCount(): Int {
        return cardList.size
    }

    inner class CardViewHolder(itemView: View, private val userId: Int) : RecyclerView.ViewHolder(itemView) {
        private val tvCardTop: TextView = itemView.findViewById(R.id.tv_value_card_top)
        private val tvCardCount: TextView = itemView.findViewById(R.id.tv_value_card_time_out)

        fun bind(card: Card) {
//            when (card.cardTop) {
//                is String -> tvCardTop.text = card.cardTop.toString()
//                is File -> tvCardTop.text = "Audio File"
//                else -> throw IllegalArgumentException("Unsupported cardTop type")
//            }
//
//            val cardTimeoutInSeconds = db.getCardTimeOut(card.id, deckId, userId)
//            val cardTimeoutInHours = cardTimeoutInSeconds / 3600.0
//
//            val formattedTimeout = String.format("%.2f", cardTimeoutInHours)
//            tvCardCount.text = formattedTimeout
        }
    }
    private fun showPopupMenu(view: View, card: Card, userId: Int, deckId: Int) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.card_menu, popupMenu.menu)

        val cardId = card.id

        // Set a click listener for each menu item
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    db.deleteCard(cardId, deckId, userId)
                    val dbcardList = db.getAllCards(userId, deckId)
                    updateData(dbcardList)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
}