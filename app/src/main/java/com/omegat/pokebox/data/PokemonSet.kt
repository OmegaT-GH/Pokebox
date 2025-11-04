package com.omegat.pokebox.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PokemonSet(
    val id: String?,
    val name: String?,
    val series: String?,
    val printedTotal: Int?,
    val total: Int?,
    val legalities: Map<String, String>,
    val ptcgoCode: String?,
    val releaseDate: String?,
    val updatedAt: String?,
    val images: Images?
) : Parcelable

@Parcelize
data class Images(
    val symbol: String,
    val logo: String
) : Parcelable
