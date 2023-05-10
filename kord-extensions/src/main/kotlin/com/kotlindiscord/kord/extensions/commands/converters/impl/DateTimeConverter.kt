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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Argument converter for dateTime arguments.
 */
@Converter(
    "dateTime",

    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE]
)
public class DateTimeConverter(
    override var validator: Validator<LocalDateTime> = null
) : SingleConverter<LocalDateTime>() {
    override val signatureTypeString: String = "converters.dateTime.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false
        this.parsed = parseFromString(arg) ?: throw DiscordRelayedException(
            context.translate(
                "converters.dateTime.error.invalid",
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
                "converters.dateTime.error.invalid",
                replacements = arrayOf(optionValue)
            )
        )

        return true
    }

    public companion object {
        /**
         * Parser for dateTime arguments.
         */
        public fun parseFromString(string: String): LocalDateTime? {
            val parsers = listOf<DateTimeFormatter>(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.RFC_1123_DATE_TIME
            )
            val parsed = parsers.firstNotNullOfOrNull {
                try {
                    val parse = it.parse(string)
                    LocalDateTime.from(parse)
                } catch (e: Exception) {
                    null
                }
            }
            return parsed
        }
    }
}
