package com.example.pokebox.data

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
        return getCards().filter { card ->
            (filter.nombre == null || card.name?.let {
                val query = filter.nombre.lowercase()
                it.lowercase().contains(query) ||
                        it.lowercase().startsWith(query) ||
                        it.lowercase().endsWith(query)
            } == true) &&
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
    }
}