/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

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

/**
 * Argument converter for arbitrary enum arguments.
 *
 * As this converter is generic, it takes a getter lambda. You can either provide one yourself, or use the default
 * one via the provided extension functions - the default getter simply checks for case-insensitive matches on enum
 * names.
 *
 * @see enum
 * @see enumList
 */
@Converter(
    "enum",

    types = [ConverterType.SINGLE, ConverterType.DEFAULTING, ConverterType.OPTIONAL, ConverterType.LIST],
    imports = ["com.kotlindiscord.kord.extensions.commands.converters.impl.getEnum"],

    builderGeneric = "E: Enum<E>",
    builderConstructorArguments = [
        "public var getter: suspend (String) -> E?"
    ],

    builderFields = [
        "public lateinit var typeName: String",
        "public var bundle: String? = null"
    ],

    functionGeneric = "E: Enum<E>",
    functionBuilderArguments = [
        "getter = { getEnum(it) }",
    ]
)
public class EnumConverter<E : Enum<E>>(
    typeName: String,
    private val getter: suspend (String) -> E?,
    override val bundle: String? = null,
    override var validator: Validator<E> = null
) : SingleConverter<E>() {
    override val signatureTypeString: String = typeName

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        try {
            parsed = getter.invoke(arg) ?: return false
        } catch (e: IllegalArgumentException) {
            return false
        }

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false

        try {
            parsed = getter.invoke(optionValue) ?: return false
        } catch (e: IllegalArgumentException) {
            return false
        }

        return true
    }
}

/**
 * The default enum value getter - matches enums based on a case-insensitive string comparison with the name.
 */
public inline fun <reified E : Enum<E>> getEnum(arg: String): E? =
    enumValues<E>().firstOrNull {
        it.name.equals(arg, true)
    }
