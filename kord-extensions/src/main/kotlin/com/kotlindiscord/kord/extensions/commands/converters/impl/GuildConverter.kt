/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for Discord [Guild] arguments.
 *
 * This converter supports specifying guilds by supplying:
 * * A guild ID
 * * The name of the guild - the first matching guild available to the bot will be used
 * * `this` to refer to the current guild
 *
 * @see guild
 * @see guildList
 */
@Converter(
    "guild",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE]
)
public class GuildConverter(
    override var validator: Validator<Guild> = null
) : SingleConverter<Guild>() {
    override val signatureTypeString: String = "converters.guild.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        if (arg.equals("this", true)) {
            val guild = context.guild

            if (guild != null) {
                this.parsed = guild

                return true
            }
        }

        this.parsed = kord.getGuildById(arg)
            ?: throw DiscordRelayedException(
                context.translate("converters.guild.error.missing", replacements = arrayOf(arg))
            )

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false

        this.parsed = kord.getGuildById(optionValue)
            ?: throw DiscordRelayedException(
                context.translate("converters.guild.error.missing", replacements = arrayOf(optionValue))
            )

        return true
    }
}
