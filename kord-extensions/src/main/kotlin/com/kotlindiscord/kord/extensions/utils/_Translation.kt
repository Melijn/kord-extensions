/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.ExtensibleBot
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*

internal val localeCache: WeakHashMap<Event, Locale> = WeakHashMap()

/** Attempt to resolve the locale for the given [MessageReceivedEvent] object. **/
public suspend fun MessageReceivedEvent.getLocale(): Locale {
    val existing = localeCache[this]

    if (existing != null) {
        return existing
    }

    val bot = getKoin().get<ExtensibleBot>()
    var result = bot.settings.i18nBuilder.defaultLocale

    for (resolver in bot.settings.i18nBuilder.localeResolvers) {
        val resolved = resolver(guild.idLong, message.channel.idLong, message.author.idLong, null)

        if (resolved != null) {
            result = resolved
            break
        }
    }

    localeCache[this] = result

    return result
}

/** Attempt to resolve the locale for the given [InteractionCreateEvent] object. **/
public suspend fun GenericInteractionCreateEvent.getLocale(): Locale {
    val existing = localeCache[this]

    if (existing != null) {
        return existing
    }

    val bot = getKoin().get<ExtensibleBot>()
    var result = bot.settings.i18nBuilder.defaultLocale

    for (resolver in bot.settings.i18nBuilder.localeResolvers) {
        val channel = interaction.channel

        val guild = if (channel is GuildChannel) {
            channel.guild
        } else {
            null
        }

        val resolved = resolver(guild?.idLong, interaction.channel?.idLong, interaction.user.idLong, interaction)

        if (resolved != null) {
            result = resolved
            break
        }
    }

    localeCache[this] = result

    return result
}
