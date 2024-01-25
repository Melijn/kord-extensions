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
import com.kotlindiscord.kord.extensions.utils.getKoin
import com.kotlindiscord.kord.extensions.utils.translate
import dev.minn.jda.ktx.generics.getChannel
import kotlinx.coroutines.FlowPreview
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.sharding.ShardManager

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

    imports = [
        "com.kotlindiscord.kord.extensions.commands.CommandContext",
        "net.dv8tion.jda.api.entities.channel.ChannelType",
        "net.dv8tion.jda.api.Permission",
        "net.dv8tion.jda.api.entities.channel.Channel",
    ],

    builderGeneric = "C: Channel",
    builderConstructorArguments = [
        "public var getter: suspend (String, CommandContext) -> C?"
    ],

    builderExtraStatements = [
        "/** Add a channel type to the set of types the given channel must match. **/",
        "public fun requireChannelType(type: ChannelType) {",
        "    requiredChannelTypes.add(type)",
        "}",
        "",
        "/** Adds channel types to the set of types the given channel must match. **/",
        "public fun requireChannelTypes(vararg types: ChannelType) {",
        "    requiredChannelTypes.addAll(types)",
        "}",
        "",
        "/**",
        " * Add a permission to the set of required perms.",
        " * The bot must have this permission in the supplied guildChannel.",
        " */",
        "public fun requirePermission(perm: Permission) {",
        "    requirePermissions.add(perm)",
        "}",
        "",
        "/**",
        " * Adds permissions to the set of required perms.",
        " * The bot must have these permissions in the supplied guildChannel.",
        " */",
        "public fun requirePermissions(vararg perms: Permission) {",
        "    requirePermissions.addAll(perms)",
        "}"
    ],

    builderFields = [
        "public var requireSameGuild: Boolean = true",
        "public var requirePermissions: MutableSet<Permission> = mutableSetOf()",
        "public var requiredGuild: (suspend () -> Long)? = null",
        "public var requiredChannelTypes: MutableSet<ChannelType> = mutableSetOf()",
    ],

    functionGeneric = "C: Channel",
    functionBuilderArguments = [
        "getter = { s, cc -> findChannel(s, cc) }",
    ]
)
public class ChannelConverter<C : Channel>(
    private val getter: suspend (String, CommandContext) -> C?,
    private val requireSameGuild: Boolean = true,
    private val requirePermissions: Set<Permission> = setOf(),
    private var requiredGuild: (suspend () -> Long)? = null,
    private val requiredChannelTypes: Set<ChannelType> = setOf(),
    override var validator: Validator<C> = null,
) : SingleConverter<C>() {
    override val signatureTypeString: String = "converters.channel.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        if (arg.equals("this", true)) {
            val channel1 = context.channel ?: return false
            val channel = channel1 as? C

            if (channel != null) {
                this.parsed = channel

                return true
            }
        }

        val channel: C = getter.invoke(arg, context) ?: throw DiscordRelayedException(
            context.translate(
                "converters.channel.error.missing", replacements = arrayOf(arg)
            )
        )
        performChecks(channel, context)

        parsed = channel
        return true
    }

    private suspend fun performChecks(channel: C, context: CommandContext) {
        if (channel is GuildChannel && (requireSameGuild || requiredGuild != null)) {
            val guildId: Long? = if (requiredGuild != null) requiredGuild!!.invoke() else context.guild?.idLong

            if (requireSameGuild && channel.guild.idLong != guildId) {
                throw DiscordRelayedException(
                    context.translate(
                        "converters.channel.error.wrongGuild",
                        replacements = arrayOf(channel.asMention)
                    )
                )
            }

            requirePermissions.forEach {
                if (!channel.guild.selfMember.hasPermission(channel, it)) {
                    throw DiscordRelayedException(
                        context.translate(
                            "converters.channel.error.missingPermission",
                            replacements = arrayOf(it.name, channel.asMention)
                        )
                    )
                }
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
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.CHANNEL, arg.displayName, arg.description, required).apply {
            setChannelTypes(requiredChannelTypes)
        }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.CHANNEL) {
            (option.asChannel as? C) ?: return false
        } else {
            return false
        }
        performChecks(optionValue, context)
        this.parsed = optionValue

        return true
    }
}

public inline fun <reified T : Channel> findChannel(arg: String, context: CommandContext): T? {
    val shardManager by getKoin().inject<ShardManager>()
    val channel: T? = if (arg.startsWith("<#") && arg.endsWith(">")) { // Channel mention
        val id = arg.substring(2, arg.length - 1)
        shardManager.getChannel<T>(id.toLong())
    } else {
        val string: String = if (arg.startsWith("#")) arg.substring(1) else arg
        val potentialTargets = context.guild?.channels?.filter { it.name.startsWith(string, true) }
        val bestTarget = potentialTargets?.minByOrNull {
            @Suppress("MagicNumber")
            when {
                it.name.equals(string, false) -> 0
                it.name.equals(string, true) -> 1
                it.name.startsWith(string, false) -> 2
                else -> 3
            }
        }
        bestTarget as? T
        null
    }

    return channel
}
