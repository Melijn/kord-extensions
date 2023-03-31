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
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for Discord [Emoji] arguments.
 *
 * This converter supports specifying emojis by supplying:
 *
 * * The actual emoji itself
 * * The emoji ID, either with or without surrounding colons
 * * The emoji name, either with or without surrounding colons -
 * the first matching emoji available to the bot will be used
 *
 * @see emoji
 * @see emojiList
 */
@Converter(
    "emoji",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE]
)
public class EmojiConverter(
    override var validator: Validator<Emoji> = null
) : SingleConverter<Emoji>() {
    override val signatureTypeString: String = "converters.emoji.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        val emoji: Emoji = findEmoji(arg, context)
            ?: throw DiscordRelayedException(
                context.translate("converters.emoji.error.missing", replacements = arrayOf(arg))
            )

        parsed = emoji
        return true
    }

    private suspend fun findEmoji(arg: String, context: CommandContext): Emoji? =
        if (arg.startsWith("<a:") || arg.startsWith("<:") && arg.endsWith('>')) { // Emoji mention
            val id: String = arg.substring(0, arg.length - 1).split(":").last()

            try {
                kord.guilds.firstNotNullOfOrNull {
                    it.getEmojiById(id)
                }
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate("converters.emoji.error.invalid", replacements = arrayOf(id))
                )
            }
        } else { // ID or name
            val name = if (arg.startsWith(":") && arg.endsWith(":")) arg.substring(1, arg.length - 1) else arg

            try {
                kord.guilds.firstNotNullOfOrNull {
                    it.getEmojiById(name)
                }
            } catch (e: NumberFormatException) {  // Not an ID, let's check names
                kord.guilds.firstNotNullOfOrNull {
                    it.emojis.firstOrNull { emojiObj -> emojiObj.name.equals(name, true) }
                }
            }
        }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.STRING) option.asString else return false

        val emoji: Emoji = findEmoji(optionValue, context)
            ?: throw DiscordRelayedException(
                context.translate("converters.emoji.error.missing", replacements = arrayOf(optionValue))
            )

        parsed = emoji
        return true
    }
}
