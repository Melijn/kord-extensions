/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber", "RethrowCaughtException", "TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.i18n.SupportedLocales
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.util.*

/**
 * Argument converter for supported locale, converting them into [Locale] objects.
 *
 * This converter only supports locales defined in [com.kotlindiscord.kord.extensions.i18n.SupportedLocales]. It's
 * intended for use with commands that allow users to specify what locale they want the bot to use when interacting
 * with them, rather than a more general converter.
 *
 * If the locale you want to use isn't supported yet, feel free to contribute translations for it to
 * [our Weblate project](https://hosted.weblate.org/projects/kord-extensions/main/).
 */
@Converter(
    "supportedLocale",
    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],
)
public class SupportedLocaleConverter(
    override var validator: Validator<Locale> = null
) : SingleConverter<Locale>() {
    override val signatureTypeString: String = "converters.supportedLocale.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        this.parsed = SupportedLocales.ALL_LOCALES[arg.lowercase().trim()] ?: throw DiscordRelayedException(
            context.translate("converters.supportedLocale.error.unknown", replacements = arrayOf(arg))
        )

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false

        this.parsed = SupportedLocales.ALL_LOCALES[optionValue.lowercase().trim()] ?: throw DiscordRelayedException(
            context.translate("converters.supportedLocale.error.unknown", replacements = arrayOf(optionValue))
        )

        return true
    }
}
