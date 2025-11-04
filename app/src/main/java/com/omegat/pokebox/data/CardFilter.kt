package com.omegat.pokebox.data



data class CardFilter(

    val nombre: String? = null,
    val supertype: List<String> = emptyList(),
    val subtype: List<String> = emptyList(),
    val type: List<String> = emptyList(),
    val legality: List<String> = emptyList(),
    val rarity: List<String> = emptyList(),
    val artist: String? = null,
    val hasability: Boolean? = null,
    val minHP: Int? = null,
    val maxHP: Int? = null


)
