/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

/** The max number of suggestions allowed. **/
public const val MAX_SUGGESTIONS: Int = 25

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun <V> CommandAutoCompleteInteractionEvent.suggestGenericMap(
    map: Map<String, V>,
    convert: (String, V) -> Choice,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    val option = focusedOption.value as? String
    var options = map

    if (option != null) {
        options = options.filterKeys { strategy.test(option, it) }
    }

    if (options.size > MAX_SUGGESTIONS) {
        options = options.entries.sortedBy { it.key }.take(MAX_SUGGESTIONS).associate { it.toPair() }
    }

    this.replyChoices(options.map { convert(it.key, it.value) }).await()
}

/**
 * Sealed interface representing matching strategies for autocomplete.
 *
 * @property test Lambda that should return `true` for acceptable values.
 */
public open class FilterStrategy(public val test: (provided: String, candidate: String) -> Boolean) {
    /** Filter options based on whether they contain the provided value. **/
    public object Contains : FilterStrategy({ provided, candidate ->
        candidate.contains(provided, true)
    })

    /** Filter options based on whether they start with the provided value. **/
    public object Prefix : FilterStrategy({ provided, candidate ->
        candidate.startsWith(provided, true)
    })

    /** Filter options based on whether they end with the provided value. **/
    public object Suffix : FilterStrategy({ provided, candidate ->
        candidate.endsWith(provided, true)
    })
}

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestStringMap(
    map: Map<String, String>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestGenericMap(map, ::Choice, strategy)
}

/**
 * Use a collection (like a list) to populate an autocomplete interaction, filtering as described by the provided
 * [strategy].
 */
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestStringCollection(
    collection: Collection<String>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestStringMap(
        collection.associateBy { it },
        strategy
    )
}

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestIntMap(
    map: Map<String, Int>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestLongMap(map.mapValues { it.value.toLong() }, strategy)
}

/**
 * Use a collection (like a list) to populate an autocomplete interaction, filtering as described by the provided
 * [strategy].
 */
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestIntCollection(
    collection: Collection<Int>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestIntMap(
        collection.associateBy { it.toString() },
        strategy
    )
}

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestLongMap(
    map: Map<String, Long>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestGenericMap(map, ::Choice, strategy)
}

/**
 * Use a collection (like a list) to populate an autocomplete interaction, filtering as described by the provided
 * [strategy].
 */
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestLongCollection(
    collection: Collection<Long>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestLongMap(
        collection.associateBy { it.toString() },
        strategy
    )
}

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestDoubleMap(
    map: Map<String, Double>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestNumberMap(map, strategy)
}

/**
 * Use a collection (like a list) to populate an autocomplete interaction, filtering as described by the provided
 * [strategy].
 */
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestDoubleCollection(
    collection: Collection<Double>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestDoubleMap(
        collection.associateBy { it.toString() },
        strategy
    )
}

/** Use a map to populate an autocomplete interaction, filtering as described by the provided [strategy]. **/
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestNumberMap(
    map: Map<String, Double>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestGenericMap(map, ::Choice, strategy)
}

/**
 * Use a collection (like a list) to populate an autocomplete interaction, filtering as described by the provided
 * [strategy].
 */
public suspend inline fun CommandAutoCompleteInteractionEvent.suggestNumberCollection(
    collection: Collection<Double>,
    strategy: FilterStrategy = FilterStrategy.Prefix,
) {
    suggestNumberMap(
        collection.associateBy { it.toString() },
        strategy
    )
}
