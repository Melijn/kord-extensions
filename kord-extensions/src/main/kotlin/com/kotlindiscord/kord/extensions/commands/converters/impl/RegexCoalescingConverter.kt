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
 * Coalescing argument converter for regular expression arguments, combining the arguments into a single [Regex]
 * object by joining them with spaces.
 *
 * Please note that user-provided regular expressions are not safe - they can take down your entire bot.
 *
 * As there is no way to validate individual segments of regex, this converter will consume all remaining arguments.
 *
 * @param options Optional set of [RegexOption]s to pass to the regex parser.
 *
 * @see coalescedRegex
 */
@Converter(
    "regex",

    types = [ConverterType.COALESCING, ConverterType.DEFAULTING, ConverterType.OPTIONAL],
    imports = ["kotlin.text.RegexOption"],
    builderFields = ["public var options: Set<RegexOption> = setOf()"]
)
public class RegexCoalescingConverter(
    private val options: Set<RegexOption> = setOf(),
    shouldThrow: Boolean = false,
    override var validator: Validator<Regex> = null
) : CoalescingConverter<Regex>(shouldThrow) {
    override val signatureTypeString: String = "converters.regex.signatureType.plural"
    override val showTypeInSignature: Boolean = false

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: List<String>?): Int {
        val args: String = named?.joinToString(" ") ?: parser?.consumeRemaining() ?: return 0

        this.parsed = args.toRegex(options)

        return args.length
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false
        this.parsed = optionValue.toRegex(options)

        return true
    }
}
