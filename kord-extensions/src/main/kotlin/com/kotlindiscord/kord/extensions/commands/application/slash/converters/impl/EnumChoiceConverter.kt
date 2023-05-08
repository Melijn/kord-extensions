/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl

import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceConverter
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.choice
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Choice converter for enum arguments. Supports mapping up to 25 choices to an enum type.
 *
 * All enums used for this must implement the [ChoiceEnum] interface.
 */
@Converter(
    "enum",

    types = [ConverterType.SINGLE, ConverterType.DEFAULTING, ConverterType.OPTIONAL, ConverterType.CHOICE],
    imports = [
        "com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.getEnum",
        "com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum"
    ],

    builderGeneric = "E",
    builderConstructorArguments = [
        "public var getter: suspend (String) -> E?",
        "!! argMap: Map<String, E>",
    ],

    builderFields = [
        "public lateinit var typeName: String",
        "public var bundle: String? = null"
    ],

    builderInitStatements = [
        "choices(argMap)"
    ],

    builderSuffixedWhere = "E : Enum<E>, E : ChoiceEnum",

    functionGeneric = "E",
    functionBuilderArguments = [
        "getter = { getEnum(it) }",
        "argMap = enumValues<E>().associateBy { it.readableName }",
    ],

    functionSuffixedWhere = "E : Enum<E>, E : ChoiceEnum"
)

public class EnumChoiceConverter<E>(
    typeName: String,
    private val getter: suspend (String) -> E?,
    choices: Map<String, E>,
    override var validator: Validator<E> = null,
    override val bundle: String? = null,
) : ChoiceConverter<E>(choices) where E : Enum<E>, E : ChoiceEnum {
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
        Option<String>(arg.displayName, arg.description, true).apply {
            this@EnumChoiceConverter.choices.forEach {
                choice(it.key, it.value.readableName)
            }
        }

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
 * The default choice enum value getter - matches choice enums via a case-insensitive string comparison with the names.
 */
public inline fun <reified E> getEnum(arg: String): E? where E : Enum<E>, E : ChoiceEnum =
    enumValues<E>().firstOrNull {
        it.readableName.equals(arg, true) ||
        it.name.equals(arg, true)
    }
