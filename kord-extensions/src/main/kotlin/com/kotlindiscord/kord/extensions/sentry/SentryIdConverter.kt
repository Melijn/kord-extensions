/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.sentry

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.parser.StringParser
import io.sentry.protocol.SentryId
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for Sentry event ID arguments.
 *
 * @see sentryId
 * @see sentryIdList
 */
public class SentryIdConverter : SingleConverter<SentryId>() {
    override val signatureTypeString: String = "extensions.sentry.converter.sentryId.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        try {
            this.parsed = SentryId(arg)
        } catch (e: IllegalArgumentException) {
            throw DiscordRelayedException(
                context.translate("extensions.sentry.converter.error.invalid", replacements = arrayOf(arg))
            )
        }

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description).apply { isRequired = true }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) { option.asString } else return false

        try {
            this.parsed = SentryId(optionValue)
        } catch (e: IllegalArgumentException) {
            throw DiscordRelayedException(
                context.translate("extensions.sentry.converter.error.invalid", replacements = arrayOf(optionValue))
            )
        }

        return true
    }
}
