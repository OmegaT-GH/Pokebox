package com.omegat.pokebox.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PokemonCard(
    val id: String?,
    val name: String?,
    val supertype: String?,
    val subtypes: List<String>?,
    val level: String?,
    val hp: String?,
    val types: List<String>?,
    val evolvesFrom: String?,
    val evolvesTo: List<String>?,
    val abilities: List<Ability>?,
    val attacks: List<Attack>?,
    val weaknesses: List<Weakness>?,
    val resistances: List<Resistance>?,
    val retreatCost: List<String>?,
    val convertedRetreatCost: Int?,
    val number: String?,
    val artist: String?,
    val rarity: String?,
    val flavorText: String?,
    val nationalPokedexNumbers: List<Int>?,
    val legalities: Legalities?,
    val images: CardImages?,
    var releaseDate: String? = null,
    var ptcgoCode: String? = null
)  : Parcelable

@Parcelize
data class Ability(
    val name: String,
    val text: String,
    val type: String
) : Parcelable

@Parcelize
data class Attack(
    val name: String,
    val cost: List<String>?,
    val convertedEnergyCost: Int?,
    val damage: String?,
    val text: String?
) : Parcelable

@Parcelize
data class Weakness(
    val type: String,
    val value: String
) : Parcelable

@Parcelize
data class Resistance(
    val type: String,
    val value: String
) : Parcelable

@Parcelize
data class Legalities(
    val unlimited: String?,
    val expanded: String?,
    val standard: String?
) : Parcelable

@Parcelize
data class CardImages(
    val small: String?,
    val large: String?
) : Parcelable
