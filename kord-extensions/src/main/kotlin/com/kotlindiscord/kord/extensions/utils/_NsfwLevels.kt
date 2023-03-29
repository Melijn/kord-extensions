/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("unused")

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import net.dv8tion.jda.api.entities.Guild.NSFWLevel
import java.util.*

/** Given a [NsfwLevel], return a string representing its translation key. **/
public fun NSFWLevel.toTranslationKey(): String? = when (this) {
    NSFWLevel.AGE_RESTRICTED -> "nsfwLevel.ageRestricted"
    NSFWLevel.DEFAULT -> "nsfwLevel.default"
    NSFWLevel.EXPLICIT -> "nsfwLevel.explicit"
    NSFWLevel.SAFE -> "nsfwLevel.safe"
    NSFWLevel.UNKNOWN -> null
}

/** Given a [CommandContext], translate the [NsfwLevel] to a human-readable string based on the context's locale. **/
public suspend fun NSFWLevel.translate(context: CommandContext): String {
    val key = toTranslationKey()

    return if (key == null) {
        context.translate(
            "nsfwLevel.unknown",
            replacements = arrayOf(this.key)
        )
    } else {
        context.translate(key)
    }
}

/** Given a locale, translate the [NsfwLevel] to a human-readable string. **/
public fun NSFWLevel.translate(locale: Locale): String {
    val key = toTranslationKey()

    return if (key == null) {
        getKoin().get<TranslationsProvider>().translate(
            "nsfwLevel.unknown",
            locale,
            replacements = arrayOf(this.key)
        )
    } else {
        getKoin().get<TranslationsProvider>().translate(key, locale)
    }
}
