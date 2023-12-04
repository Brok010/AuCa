package com.example.logindb.Classes

data class Deck(
    val id: Int = -1,
    var deckName: String = "",
    var cardCount :Int = -1,
    val userIdFk :Int = -1
)