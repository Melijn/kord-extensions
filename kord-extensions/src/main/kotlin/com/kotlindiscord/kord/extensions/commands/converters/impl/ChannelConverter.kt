/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(
    FlowPreview::class,
)

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import com.kotlindiscord.kord.extensions.utils.translate
import kotlinx.coroutines.FlowPreview
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for Discord [Channel] arguments.
 *
 * This converter supports specifying channels by supplying:
 *
 * * A channel mention
 * * A channel ID, with or without a `#` prefix
 * * A channel name, with or without a `#` prefix (the required guild will be searched for the first matching channel)
 * * `this` to refer to the current channel
 *
 * @param requireSameGuild Whether to require that the channel passed is on the same guild as the message.
 * @param requiredGuild Lambda returning a specific guild to require the channel to be in, if needed.
 */
@Converter(
    "channel",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],

    imports = ["dev.kord.common.entity.ChannelType", "dev.kord.common.entity.Snowflake"],

    builderExtraStatements = [
        "/** Add a channel type to the set of types the given channel must match. **/",
        "public fun requireChannelType(type: ChannelType) {",
        "    requiredChannelTypes.add(type)",
        "}"
    ],

    builderFields = [
        "public var requireSameGuild: Boolean = true",
        "public var requiredGuild: (suspend () -> Snowflake)? = null",
        "public var requiredChannelTypes: MutableSet<ChannelType> = mutableSetOf()",
    ],
)
public class ChannelConverter(
    private val requireSameGuild: Boolean = true,
    private var requiredGuild: (suspend () -> Long)? = null,
    private val requiredChannelTypes: Set<ChannelType> = setOf(),
    override var validator: Validator<Channel> = null
) : SingleConverter<Channel>() {
    override val signatureTypeString: String = "converters.channel.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        if (arg.equals("this", true)) {
            val channel = context.channel

            if (channel != null) {
                this.parsed = channel

                return true
            }
        }

        val channel: Channel = findChannel(arg, context) ?: throw DiscordRelayedException(
            context.translate(
                "converters.channel.error.missing", replacements = arrayOf(arg)
            )
        )

        parsed = channel
        return true
    }

    private suspend fun findChannel(arg: String, context: CommandContext): Channel? {
        val channel: Channel? = if (arg.startsWith("<#") && arg.endsWith(">")) { // Channel mention
            val id = arg.substring(2, arg.length - 1)

            try {
                kord.getChannelById(Channel::class.java, id.toLong())
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate(
                        "converters.channel.error.invalid", replacements = arrayOf(id)
                    )
                )
            }
        } else {
            val string: String = if (arg.startsWith("#")) arg.substring(1) else arg
            val potentialTargets = context.guild?.channels?.filter { it.name.startsWith(string, true) }
            val bestTarget = potentialTargets?.minByOrNull {
                when {
                    it.name.equals(string, false) -> 0
                    it.name.equals(string, true) -> 1
                    it.name.startsWith(string, false) -> 2
                    else -> 3
                }
            }
            bestTarget
        }

        channel ?: return null

        if (channel is GuildChannel && (requireSameGuild || requiredGuild != null)) {
            val guildId: Long? = if (requiredGuild != null) requiredGuild!!.invoke() else context.guild?.idLong

            if (requireSameGuild && channel.guild.idLong != guildId) {
                return null  // Channel isn't in the right guild
            }
        }

        if (requiredChannelTypes.isNotEmpty() && channel.type !in requiredChannelTypes) {
            val locale = context.resolvedLocale.await()

            throw DiscordRelayedException(
                context.translate(
                    "converters.channel.error.wrongType",
                    replacements = arrayOf(
                        channel.type,
                        requiredChannelTypes.joinToString { "**${it.translate(locale)}**" }
                    )
                )
            )
        }

        return channel
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.CHANNEL, arg.displayName, arg.description, required).apply {
            setChannelTypes(requiredChannelTypes)
        }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.CHANNEL) option.asChannel else return false
        this.parsed = optionValue

        return true
    }
}
