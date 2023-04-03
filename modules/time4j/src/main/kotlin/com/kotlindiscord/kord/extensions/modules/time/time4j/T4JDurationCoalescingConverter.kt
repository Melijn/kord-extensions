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

package com.kotlindiscord.kord.extensions.modules.time.time4j

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.i18n.EMPTY_VALUE_STRING
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import com.kotlindiscord.kord.extensions.parser.tokens.PositionalArgumentToken
import com.kotlindiscord.kord.extensions.parsers.DurationParserException
import com.kotlindiscord.kord.extensions.parsers.InvalidTimeUnitException
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.time4j.Duration
import net.time4j.IsoUnit

/**
 * Coalescing argument converter for Time4J [Duration] arguments.
 *
 * This converter will take individual duration specifiers ("1w", "2y", "3d" etc) until it no longer can, and then
 * combine them into a single [Duration].
 *
 * @param longHelp Whether to send the user a long help message with specific information on how to specify durations.
 *
 * @see coalescedT4jDuration
 * @see parseT4JDuration
 */
@Converter(
    names = ["t4JDuration"],
    types = [ConverterType.COALESCING, ConverterType.DEFAULTING, ConverterType.OPTIONAL],
    imports = ["net.time4j.*"],

    builderFields = [
        "public var longHelp: Boolean = true",
        "public var shouldThrow: Boolean = true"
    ],
)
public class T4JDurationCoalescingConverter(
    public val longHelp: Boolean = true,
    shouldThrow: Boolean = false,
    override var validator: Validator<Duration<IsoUnit>> = null
) : CoalescingConverter<Duration<IsoUnit>>(shouldThrow) {
    override val signatureTypeString: String = "converters.duration.error.signatureType"
    private val logger: KLogger = KotlinLogging.logger {}

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: List<String>?): Int {
        val durations: MutableList<String> = mutableListOf<String>()

        val ignoredWords: List<String> = context.translate("utils.durations.ignoredWords")
            .split(",")
            .toMutableList()
            .apply { remove(EMPTY_VALUE_STRING) }

        var skipNext: Boolean = false

        val locale = context.resolvedLocale.await()
        val args: List<String> = named ?: parser?.run {
            val tokens: MutableList<String> = mutableListOf()

            while (hasNext) {
                val nextToken: PositionalArgumentToken? = peekNext()

                if (nextToken!!.data.all { T4JDurationParser.charValid(it, locale) }) {
                    tokens.add(parseNext()!!.data)
                } else {
                    break
                }
            }

            tokens
        } ?: return 0

        @Suppress("LoopWithTooManyJumpStatements")  // Well you rewrite it then, detekt
        for (index in args.indices) {
            if (skipNext) {
                skipNext = false

                continue
            }

            val arg: String = args[index]

            if (arg in ignoredWords) continue

            try {
                // We do it this way so that we stop parsing as soon as an invalid string is found
                T4JDurationParser.parse(arg, locale)
                T4JDurationParser.parse(durations.joinToString("") + arg, locale)

                durations.add(arg)
            } catch (e: DurationParserException) {
                try {
                    val nextIndex: Int = index + 1

                    if (nextIndex >= args.size) {
                        throw e
                    }

                    val nextArg: String = args[nextIndex]
                    val combined: String = arg + nextArg

                    T4JDurationParser.parse(combined, locale)
                    T4JDurationParser.parse(durations.joinToString("") + combined, locale)

                    durations.add(combined)
                    skipNext = true
                } catch (t: InvalidTimeUnitException) {
                    throwIfNecessary(t, context)

                    break
                } catch (t: DurationParserException) {
                    throwIfNecessary(t, context)

                    break
                }
            }
        }

        try {
            parsed = T4JDurationParser.parse(
                durations.joinToString(""),
                locale
            )
        } catch (e: InvalidTimeUnitException) {
            throwIfNecessary(e, context, true)
        } catch (e: DurationParserException) {
            throwIfNecessary(e, context, true)
        }

        return durations.size
    }

    private suspend fun throwIfNecessary(
        e: Exception,
        context: CommandContext,
        override: Boolean = false
    ): Unit = if (shouldThrow || override) {
        when (e) {
            is InvalidTimeUnitException -> {
                val message: String = context.translate(
                    "converters.duration.error.invalidUnit",
                    replacements = arrayOf(e.unit)
                ) + if (longHelp) "\n\n" + context.translate("converters.duration.help") else ""

                throw DiscordRelayedException(message)
            }

            is DurationParserException -> throw DiscordRelayedException(e.error)

            else -> throw e
        }
    } else {
        logger.debug(e) { "Error thrown during duration parsing" }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val arg: String = if (option.type == OptionType.STRING) option.asString else return false

        try {
            this.parsed = T4JDurationParser.parse(arg, context.resolvedLocale.await())
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
