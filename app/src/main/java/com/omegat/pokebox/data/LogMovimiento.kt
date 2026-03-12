package com.omegat.pokebox.data

data class LogMovimiento(
    val logId: Int,
    val colId: Int,
    val cardId: String,
    val cantidadAnterior: Int,
    val cantidadNueva: Int,
    val timestamp: Long,
    val cardName: String?,
    val setName: String?,
    val cardNumber: String?,
    val cardImageUrl: String?
)
