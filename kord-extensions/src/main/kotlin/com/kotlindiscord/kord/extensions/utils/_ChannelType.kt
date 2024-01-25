/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import net.dv8tion.jda.api.entities.channel.ChannelType
import java.util.*

/** Given a [ChannelType], return a string representing its translation key. **/
public fun ChannelType.toTranslationKey(): String = when (this) {
    ChannelType.PRIVATE -> "channelType.dm"
    ChannelType.GROUP -> "channelType.groupDm"
    ChannelType.CATEGORY -> "channelType.guildCategory"
    ChannelType.NEWS -> "channelType.guildNews"
    ChannelType.STAGE -> "channelType.guildStageVoice"
    ChannelType.TEXT -> "channelType.guildText"
    ChannelType.VOICE -> "channelType.guildVoice"
    ChannelType.GUILD_NEWS_THREAD -> "channelType.publicNewsThread"
    ChannelType.GUILD_PUBLIC_THREAD -> "channelType.publicGuildThread"
    ChannelType.GUILD_PRIVATE_THREAD -> "channelType.privateThread"
    ChannelType.FORUM -> "channelType.guildDirectory"
    ChannelType.UNKNOWN -> "channelType.unknown"
    ChannelType.MEDIA -> "channelType.media"
}

/**
 * Given a [CommandContext], translate the [ChannelType] to a human-readable string based on the context's locale.
 */
public suspend fun ChannelType.translate(context: CommandContext): String =
    context.translate(toTranslationKey())

/**
 * Given a locale, translate the [ChannelType] to a human-readable string.
 */
public fun ChannelType.translate(locale: Locale): String =
    getKoin().get<TranslationsProvider>().translate(toTranslationKey(), locale)
