package com.example.logindb.Classes

data class Card(
    val userId :Int = -1,
    val deckId :Int = -1,
    val id: Int = -1,
    var cardTop: Any? = null,
    var cardBottom: Any? = null,
    var timeout :Long = -1,
    var coefficient :Double = -1.0
)