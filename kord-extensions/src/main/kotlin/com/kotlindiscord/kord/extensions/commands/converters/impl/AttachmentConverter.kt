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
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for Discord attachments.
 *
 * This converter can only be used in slash commands.
 */
@Converter(
    "attachment",

    types = [ConverterType.OPTIONAL, ConverterType.SINGLE],
)
public class AttachmentConverter(
    override var validator: Validator<Message.Attachment> = null
) : SingleConverter<Message.Attachment>() {
    override val signatureTypeString: String = "converters.attachment.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean =
        throw DiscordRelayedException(context.translate("converters.attachment.error.slashCommandsOnly"))

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.ATTACHMENT, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.ATTACHMENT) option.asAttachment else return false
        this.parsed = optionValue

        return true
    }
}
