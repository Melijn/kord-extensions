/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("StringLiteralDuplication")

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for discord [Member] arguments.
 *
 * Members represent Discord users that are part of a guild. This converter supports specifying members by supplying:
 * * A user or member mention
 * * A user ID
 * * The user's tag (`username#discriminator`)
 * * "me" to refer to the member running the command
 *
 * @param requiredGuild Lambda returning a specific guild to require the member to be in, if needed.
 * @param useReply Whether to use the author of the replied-to message (if there is one) instead of trying to parse an
 * argument.
 */
@Converter(
    "member",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],
    imports = [],

    builderFields = [
        "public var requiredGuild: (suspend () -> Long)? = null",
        "public var useReply: Boolean = true",
        "public var requireSameGuild: Boolean = true",
    ]
)
public class MemberConverter(
    private var requiredGuild: (suspend () -> Long)? = null,
    private var useReply: Boolean = true,
    private var requireSameGuild: Boolean = true,
    override var validator: Validator<Member> = null,
) : SingleConverter<Member>() {
    override val signatureTypeString: String = "converters.member.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val guild = context.guild

        if (requireSameGuild && requiredGuild == null && guild != null) {
            requiredGuild = { guild.idLong }
        }

        if (useReply && context is ChatCommandContext<*>) {
            val messageReference = context.message.messageReference

            if (messageReference != null) {
                val member = messageReference.message?.member

                if (member != null) {
                    parsed = member
                    return true
                }
            }
        }

        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        if (arg.equals("me", true)) {
            val member = context.member

            if (member != null) {
                this.parsed = member

                return true
            }
        }

        parsed = findMember(arg, context)
            ?: throw DiscordRelayedException(
                context.translate("converters.member.error.missing", replacements = arrayOf(arg))
            )

        return true
    }

    private suspend fun findMember(arg: String, context: CommandContext): Member? {
        val user: User? = if (arg.startsWith("<@") && arg.endsWith(">")) { // It's a mention
            val id: String = arg.substring(2, arg.length - 1).replace("!", "")

            try {
                kord.getUserById(id)
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate("converters.member.error.invalid", replacements = arrayOf(id))
                )
            }
        } else {
            try { // Try for a user ID first
                kord.getUserById(arg)
            } catch (e: NumberFormatException) { // It's not an ID, let's try the tag
                if (!arg.contains("#")) {
                    null
                } else {
                    kord.users.firstOrNull { user ->
                        user.asTag.equals(arg, true)
                    }
                }
            }
        }

        val currentGuild = context.guild ?: return null
        val guildId: Long = requiredGuild?.invoke() ?: currentGuild.idLong

        if (guildId != currentGuild.idLong) {
            throw DiscordRelayedException(
                context.translate("converters.member.error.invalid", replacements = arrayOf(user?.asTag ?: arg))
            )
        }

        return user?.idLong?.let { currentGuild.getMemberById(it) }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.USER, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.USER) option.asMember ?: return false else return false
        val guild = context.guild

        if (requireSameGuild && requiredGuild == null && guild != null) {
            requiredGuild = { guild.idLong }
        }

        val requiredGuildId = requiredGuild?.invoke()

        if (requiredGuildId != null && optionValue.guild.idLong != requiredGuildId) {
            throw DiscordRelayedException(
                context.translate("converters.member.error.invalid", replacements = arrayOf(optionValue.user.asTag))
            )
        }

        this.parsed = optionValue

        return true
    }
}
