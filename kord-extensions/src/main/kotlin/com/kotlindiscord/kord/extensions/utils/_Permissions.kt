/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("unused")

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import net.dv8tion.jda.api.Permission
import java.util.*

/** Given a [Permission], return a string representing its translation key. **/
public fun Permission.toTranslationKey(): String? = when (this) {
    Permission.MESSAGE_ADD_REACTION -> "permission.addReactions"
    Permission.ADMINISTRATOR -> "permission.administrator"
    Permission.MESSAGE_ATTACH_FILES -> "permission.attachFiles"
    Permission.BAN_MEMBERS -> "permission.banMembers"
    Permission.NICKNAME_CHANGE -> "permission.changeNickname"
    Permission.VOICE_CONNECT -> "permission.connect"
    Permission.CREATE_INSTANT_INVITE -> "permission.createInstantInvite"
    Permission.VOICE_DEAF_OTHERS -> "permission.deafenMembers"
    Permission.MESSAGE_EMBED_LINKS -> "permission.embedLinks"
    Permission.KICK_MEMBERS -> "permission.kickMembers"
    Permission.MANAGE_CHANNEL -> "permission.manageChannels"
    Permission.MANAGE_EMOJIS_AND_STICKERS -> "permission.manageExpressions"
    Permission.MANAGE_GUILD_EXPRESSIONS -> "permission.manageExpressions"
    Permission.MANAGE_EVENTS -> "permission.manageEvents"
    Permission.MANAGE_SERVER -> "permission.manageGuild"
    Permission.MANAGE_PERMISSIONS -> "permission.managePermissions"
    Permission.MESSAGE_MANAGE -> "permission.manageMessages"
    Permission.NICKNAME_MANAGE -> "permission.manageNicknames"
    Permission.MANAGE_ROLES -> "permission.manageRoles"
    Permission.MANAGE_THREADS -> "permission.manageThreads"
    Permission.MANAGE_WEBHOOKS -> "permission.manageWebhooks"
    Permission.MESSAGE_MENTION_EVERYONE -> "permission.mentionEveryone"
    Permission.VOICE_MOVE_OTHERS -> "permission.moveMembers"
    Permission.VOICE_MUTE_OTHERS -> "permission.muteMembers"
    Permission.PRIORITY_SPEAKER -> "permission.prioritySpeaker"
    Permission.MESSAGE_HISTORY -> "permission.readMessageHistory"
    Permission.REQUEST_TO_SPEAK -> "permission.requestToSpeak"
    Permission.MESSAGE_SEND -> "permission.sendMessages"
    Permission.MESSAGE_TTS -> "permission.sendTTSMessages"
    Permission.VOICE_SPEAK -> "permission.speak"
    Permission.VOICE_STREAM -> "permission.stream"
    Permission.MODERATE_MEMBERS -> "permission.timeoutMembers"
    Permission.MESSAGE_EXT_EMOJI -> "permission.useExternalEmojis"
    Permission.USE_APPLICATION_COMMANDS -> "permission.useApplicationCommands"
    Permission.VOICE_USE_VAD -> "permission.useVAD"
    Permission.VIEW_AUDIT_LOGS -> "permission.viewAuditLog"
    Permission.VIEW_CHANNEL -> "permission.viewChannel"
    Permission.VIEW_GUILD_INSIGHTS -> "permission.viewGuildInsights"

    Permission.CREATE_PUBLIC_THREADS -> "permission.createPublicThreads"
    Permission.CREATE_PRIVATE_THREADS -> "permission.createPrivateThreads"
    Permission.MESSAGE_SEND_IN_THREADS -> "permission.sendMessagesInThreads"

    Permission.MESSAGE_EXT_STICKER -> "permission.useExternalStickers"
    Permission.VOICE_START_ACTIVITIES -> "permission.useEmbeddedActivities"

    Permission.UNKNOWN -> null
    Permission.VIEW_CREATOR_MONETIZATION_ANALYTICS -> "permission.viewMonetization"
    Permission.MESSAGE_ATTACH_VOICE_MESSAGE -> "permission.attachVoiceMessage"
    Permission.VOICE_USE_SOUNDBOARD -> "permission.useSoundboard"
    Permission.VOICE_USE_EXTERNAL_SOUNDS -> "permission.useExternalSounds"
    Permission.VOICE_SET_STATUS -> "permission.setVoiceChannelStatus"
}

/** Given a [CommandContext], translate the [Permission] to a human-readable string based on the context's locale. **/
public suspend fun Permission.translate(context: CommandContext): String {
    val key = toTranslationKey()

    return if (key == null) {
        context.translate("permission.unknown", replacements = arrayOf(this.rawValue))
    } else {
        context.translate(key)
    }
}

/** Given a locale, translate the [Permission] to a human-readable string. **/
public fun Permission.translate(locale: Locale): String {
    val key = toTranslationKey()

    return if (key == null) {
        getKoin().get<TranslationsProvider>().translate(
            "permission.unknown",
            locale,
            replacements = arrayOf(this.rawValue)
        )
    } else {
        getKoin().get<TranslationsProvider>().translate(key, locale)
    }
}
