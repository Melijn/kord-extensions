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
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.time.ZoneOffset

/**
 * Argument converter for zoneOffset arguments.
 */
@Converter(
    "zoneOffset",

    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE]
)
public class ZoneOffsetConverter(
    override var validator: Validator<ZoneOffset> = null,
) : SingleConverter<ZoneOffset>() {
    override val signatureTypeString: String = "converters.zoneOffset.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false
        this.parsed = parseFromString(arg) ?: throw DiscordRelayedException(
            context.translate(
                "converters.zoneOffset.error.invalid",
                replacements = arrayOf(arg)
            )
        )

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false
        this.parsed = parseFromString(optionValue) ?: throw DiscordRelayedException(
            context.translate(
                "converters.zoneOffset.error.invalid",
                replacements = arrayOf(optionValue)
            )
        )

        return true
    }

    internal companion object {
        internal fun parseFromString(string: String): ZoneOffset? = try {
            ZoneOffset.of(string)
        } catch (e: Exception) {
            null
        }
    }
}
