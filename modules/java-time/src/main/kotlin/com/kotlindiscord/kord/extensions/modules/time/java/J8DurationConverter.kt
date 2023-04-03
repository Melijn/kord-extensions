/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(
    ConverterToDefaulting::class,
    ConverterToMulti::class,
    ConverterToOptional::class
)

package com.kotlindiscord.kord.extensions.modules.time.java

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import com.kotlindiscord.kord.extensions.parsers.DurationParserException
import com.kotlindiscord.kord.extensions.parsers.InvalidTimeUnitException
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.time.Duration
import java.time.LocalDateTime

/**
 * Argument converter for Java 8 [Duration] arguments.
 *
 * For a coalescing version of this converter, see [J8DurationCoalescingConverter].
 * If you're using Time4J instead, see [T4JDurationConverter].
 *
 * @param longHelp Whether to send the user a long help message with specific information on how to specify durations.
 * @param positiveOnly Whether a positive duration is required - `true` by default.
 */
@Converter(
    names = ["j8Duration"],
    types = [ConverterType.DEFAULTING, ConverterType.OPTIONAL, ConverterType.SINGLE],
    imports = ["java.time.*"],

    builderFields = [
        "public var longHelp: Boolean = true",
        "public var positiveOnly: Boolean = true",
    ],
)
public class J8DurationConverter(
    public val longHelp: Boolean = true,
    public val positiveOnly: Boolean = true,
    override var validator: Validator<ChronoContainer> = null
) : SingleConverter<ChronoContainer>() {
    override val signatureTypeString: String = "converters.duration.error.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        try {
            val result: ChronoContainer = J8DurationParser.parse(arg, context.resolvedLocale.await())

            if (positiveOnly) {
                val normalized: ChronoContainer = result.clone()

                normalized.normalize(LocalDateTime.now())

                if (!normalized.isPositive()) {
                    throw DiscordRelayedException(context.translate("converters.duration.error.positiveOnly"))
                }
            }

            parsed = result
        } catch (e: InvalidTimeUnitException) {
            val message: String = context.translate(
                "converters.duration.error.invalidUnit",
                replacements = arrayOf(e.unit)
            ) + if (longHelp) "\n\n" + context.translate("converters.duration.help") else ""

            throw DiscordRelayedException(message)
        } catch (e: DurationParserException) {
            throw DiscordRelayedException(e.error)
        }

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val arg: String = if (option.type == OptionType.STRING) option.asString else return false

        try {
            val result: ChronoContainer = J8DurationParser.parse(arg, context.resolvedLocale.await())

            if (positiveOnly) {
                val normalized: ChronoContainer = result.clone()

                normalized.normalize(LocalDateTime.now())

                if (!normalized.isPositive()) {
                    throw DiscordRelayedException(context.translate("converters.duration.error.positiveOnly"))
                }
            }

            parsed = result
        } catch (e: InvalidTimeUnitException) {
            val message: String = context.translate(
                "converters.duration.error.invalidUnit",
                replacements = arrayOf(e.unit)
            ) + if (longHelp) "\n\n" + context.translate("converters.duration.help") else ""

            throw DiscordRelayedException(message)
        } catch (e: DurationParserException) {
            throw DiscordRelayedException(e.error)
        }

        return true
    }
}
