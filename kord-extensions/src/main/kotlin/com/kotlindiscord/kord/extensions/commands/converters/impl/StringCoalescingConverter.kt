/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Coalescing argument converter that simply joins all arguments with spaces to produce a single string.
 *
 * This converter will consume all remaining arguments.
 *
 * @see coalescedString
 */
@Converter(
    "string",

    types = [ConverterType.COALESCING, ConverterType.DEFAULTING, ConverterType.OPTIONAL]
)
public class StringCoalescingConverter(
    shouldThrow: Boolean = false,
    override var validator: Validator<String> = null
) : CoalescingConverter<String>(shouldThrow) {
    override val signatureTypeString: String = "converters.string.signatureType"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: List<String>?): Int {
        this.parsed = named?.joinToString(" ") ?: parser?.consumeRemaining() ?: return 0

        return parsed.length
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false
        this.parsed = optionValue

        return true
    }
}
