/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

private val logger = KotlinLogging.logger {}

/**
 * Argument converter for discord [Message] arguments.
 *
 * This converter supports specifying messages by supplying:
 * * A Discord message jump link
 * * A message ID (it will be assumed that the message is in the current channel).
 *
 * @param requireGuild Whether to require messages to be in a specified guild.
 * @param requiredGuild Lambda returning a specific guild to require the member to be in. If omitted, defaults to the
 * guild the command was invoked in.
 * @param useReply Whether to use the replied-to message (if there is one) instead of trying to parse an argument.
 */
@Converter(
    "message",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],
    imports = [],

    builderFields = [
        "public var requireGuild: Boolean = false",
        "public var requiredGuild: (suspend () -> Long)? = null",
        "public var useReply: Boolean = true",
    ]
)
public class MessageConverter(
    private var requireGuild: Boolean = false,
    private var requiredGuild: (suspend () -> Long)? = null,
    private var useReply: Boolean = true,
    override var validator: Validator<Message> = null
) : SingleConverter<Message>() {
    override val signatureTypeString: String = "converters.message.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        if (useReply && context is ChatCommandContext<*>) {
            val messageReference = context.message.messageReference

            if (messageReference != null) {
                val message = messageReference.message

                if (message != null) {
                    parsed = message
                    return true
                }
            }
        }

        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        parsed = findMessage(arg, context)

        return true
    }

    private suspend fun findMessage(arg: String, context: CommandContext): Message {
        val requiredGid: Long? = if (requiredGuild != null) {
            requiredGuild!!.invoke()
        } else {
            context.guild?.idLong
        }

        return if (arg.startsWith("https://")) { // It's a message URL
            @Suppress("MagicNumber")
            val split: List<String> = arg.substring(8).split("/").takeLast(3)

            @Suppress("MagicNumber")
            if (split.size < 3) {
                throw DiscordRelayedException(
                    context.translate("converters.message.error.invalidUrl", replacements = arrayOf(arg))
                )
            }

            @Suppress("MagicNumber")
            val gid: Long = try {
                split[0].toLong()
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate("converters.message.error.invalidGuildId", replacements = arrayOf(split[0]))
                )
            }

            if (requireGuild && requiredGid != gid) {
                logger.trace { "Matching guild ($requiredGid) required, but guild ($gid) doesn't match." }

                errorNoMessage(arg, context)
            }

            @Suppress("MagicNumber")
            val cid: Long = try {
                split[1].toLong()
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate(
                        "converters.message.error.invalidChannelId",
                        replacements = arrayOf(split[1])
                    )
                )
            }

            val channel: GuildChannel? = kord.getGuildById(gid)?.getGuildChannelById(cid)
            if (channel == null) {
                logger.trace { "Unable to find channel ($cid) for guild ($gid)." }

                errorNoMessage(arg, context)
            }

            if (channel !is GuildMessageChannel) {
                logger.trace { "Specified channel ($cid) is not a guild message channel." }

                errorNoMessage(arg, context)
            }

            @Suppress("MagicNumber")
            val mid: Long = try {
                split[2].toLong()
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate(
                        "converters.message.error.invalidMessageId",
                        replacements = arrayOf(split[2])
                    )
                )
            }

            try {
                channel.retrieveMessageById(mid).await()
            } catch (e: ErrorResponseException) {
                errorNoMessage(mid.toString(), context)
            }
        } else { // Try a message ID
            val channel: Channel? = context.channel

            if (channel !is GuildMessageChannel && channel !is PrivateChannel) {
                logger.trace { "Current channel is not a guild message channel or DM channel." }

                errorNoMessage(arg, context)
            }

            if (channel !is MessageChannel) {
                logger.trace { "Current channel is not a message channel, so it can't contain messages." }

                errorNoMessage(arg, context)
            }

            try {
                channel.retrieveMessageById(arg.toLong()).await()
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate(
                        "converters.message.error.invalidMessageId",
                        replacements = arrayOf(arg)
                    )
                )
            } catch (e: ErrorResponseException) {
                errorNoMessage(arg, context)
            }
        }
    }

    private suspend fun errorNoMessage(arg: String, context: CommandContext): Nothing {
        throw DiscordRelayedException(
            context.translate("converters.message.error.missing", replacements = arrayOf(arg))
        )
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false

        parsed = findMessage(optionValue, context)

        return true
    }
}
