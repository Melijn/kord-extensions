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
import dev.minn.jda.ktx.interactions.commands.Option
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Coalescing argument that simply returns the argument as it was given.
 *
 * The multi version of this converter (via [toList]) will consume all remaining arguments.
 *
 * @property maxLength The maximum length allowed for this argument.
 * @property minLength The minimum length allowed for this argument.
 */
@Converter(
    "string",

    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],

    builderFields = [
        "public var maxLength: Int? = null",
        "public var minLength: Int? = null",
    ]
)
public class StringConverter(
    public val maxLength: Int? = null,
    public val minLength: Int? = null,
    override var validator: Validator<String> = null
) : SingleConverter<String>() {
    override val signatureTypeString: String = "converters.string.signatureType"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        this.parsed = arg

        if (minLength != null && this.parsed.length < minLength) {
            throw DiscordRelayedException(
                context.translate(
                    "converters.string.error.invalid.tooLong",
                    replacements = arrayOf(arg, minLength)
                )
            )
        }

        if (maxLength != null && this.parsed.length > maxLength) {
            throw DiscordRelayedException(
                context.translate(
                    "converters.string.error.invalid.tooShort",
                    replacements = arrayOf(arg, maxLength)
                )
            )
        }

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        Option<String>(arg.displayName, arg.description, required).apply {
            this@StringConverter.maxLength?.let { this.setMaxLength(it) }
            this@StringConverter.minLength?.let { this.setMinLength(it) }
        }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false
        this.parsed = optionValue
        return true
    }
}
