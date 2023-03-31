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

private const val DEFAULT_RADIX = 10

/**
 * Argument converter for long arguments, converting them into [Long].
 *
 * @property maxValue The maximum value allowed for this argument.
 * @property minValue The minimum value allowed for this argument.
 */
@Converter(
    "long",

    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],

    builderFields = [
        "public var radix: Int = $DEFAULT_RADIX",

        "public var maxValue: Long? = null",
        "public var minValue: Long? = null",
    ]
)
public class LongConverter(
    private val radix: Int = DEFAULT_RADIX,
    public val maxValue: Long? = null,
    public val minValue: Long? = null,

    override var validator: Validator<Long> = null
) : SingleConverter<Long>() {
    override val signatureTypeString: String = "converters.number.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        try {
            this.parsed = arg.toLong(radix)
        } catch (e: NumberFormatException) {
            val errorString = if (radix == DEFAULT_RADIX) {
                context.translate("converters.number.error.invalid.defaultBase", replacements = arrayOf(arg))
            } else {
                context.translate("converters.number.error.invalid.otherBase", replacements = arrayOf(arg, radix))
            }

            throw DiscordRelayedException(errorString)
        }

        if (minValue != null && this.parsed < minValue) {
            throw DiscordRelayedException(
                context.translate(
                    "converters.number.error.invalid.tooSmall",
                    replacements = arrayOf(arg, minValue)
                )
            )
        }

        if (maxValue != null && this.parsed > maxValue) {
            throw DiscordRelayedException(
                context.translate(
                    "converters.number.error.invalid.tooLarge",
                    replacements = arrayOf(arg, maxValue)
                )
            )
        }

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.INTEGER, arg.displayName, arg.description, required).apply {
            this@LongConverter.maxValue?.let { this.setMaxValue(it) }
            this@LongConverter.minValue?.let { this.setMinValue(it) }
        }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.INTEGER) option.asLong else return false
        this.parsed = optionValue

        return true
    }
}
