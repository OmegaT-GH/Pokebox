package com.omegat.pokebox.data

object CardRepository {
    private var allCards: MutableList<PokemonCard> = mutableListOf()

    fun addCards(cards: List<PokemonCard>) {
        allCards.addAll(cards)
    }

    fun getCards(): List<PokemonCard> = allCards

    fun clear() {
        allCards.clear()
    }

    fun getFilteredCards(filter: CardFilter): List<PokemonCard> {

        val base = getCards().filter { card ->
            (filter.supertype.isEmpty() || filter.supertype.contains(card.supertype)) &&
                    (filter.subtype.isEmpty() || card.subtypes?.any { it in filter.subtype } == true) &&
                    (filter.type.isEmpty() || card.types?.any { it in filter.type } == true) &&
                    (filter.legality.isEmpty() || filter.legality.any { l ->
                        card.legalities?.let {
                            (l.startsWith("Unlimited") && it.unlimited != null) ||
                                    (l.startsWith("Expanded") && it.expanded != null) ||
                                    (l.startsWith("Standard") && it.standard != null)
                        } ?: false
                    }) &&
                    (filter.rarity.isEmpty() || filter.rarity.contains(card.rarity)) &&
                    (filter.artist == null || filter.artist == card.artist) &&
                    (filter.hasability == null || (filter.hasability && card.abilities != null) || (!filter.hasability && card.abilities == null)) &&
                    (filter.minHP == null || (card.hp?.toIntOrNull() ?: 0) >= filter.minHP) &&
                    (filter.maxHP == null || (card.hp?.toIntOrNull() ?: 0) <= filter.maxHP)
        }

        if (!filter.nombre.isNullOrBlank()) {
            val query = filter.nombre.lowercase()

            return base
                .filter { nameMatchScore(it.name, query) > 0 }
                .sortedByDescending { nameMatchScore(it.name, query) }
        }

        return base
    }

    private fun nameMatchScore(name: String?, query: String): Int {
        if (name == null) return 0

        val n = name.lowercase()
        val qWords = query.lowercase().split(" ").filter { it.isNotBlank() }

        var score = 0

        for (w in qWords) {
            when {
                n == w -> score += 500
                n.startsWith(w) -> score += 300
                n.contains(w) -> score += 150
            }
        }

        if (qWords.all { n.contains(it) }) score += 200

        return score
    }
}