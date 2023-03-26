/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for discord [User] arguments.
 *
 * This converter supports specifying members by supplying:
 * * A user or member mention
 * * A user ID
 * * The user's tag (`username#discriminator`)
 * * "me" to refer to the user running the command
 *
 * @param useReply Whether to use the author of the replied-to message (if there is one) instead of trying to parse an
 * argument.
 */
@Converter(
    "user",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],
    builderFields = ["public var useReply: Boolean = true"]
)
public class UserConverter(
    private var useReply: Boolean = true,
    override var validator: Validator<User> = null
) : SingleConverter<User>() {
    override val signatureTypeString: String = "converters.user.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        if (useReply && context is ChatCommandContext<*>) {
            val user = context.message.messageReference?.message?.author

            if (user != null) {
                parsed = user
                return true
            }
        }

        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        if (arg.equals("me", true)) {
            parsed = context.user
            return true
        }

        parsed = findUser(arg, context)
            ?: throw DiscordRelayedException(
                context.translate("converters.user.error.missing", replacements = arrayOf(arg))
            )

        return true
    }

    private suspend fun findUser(arg: String, context: CommandContext): User? =
        if (arg.length > 40) null
        else if (arg.startsWith("<@") && arg.endsWith(">")) { // It's a mention
            val id: String = arg.substring(2, arg.length - 1).replace("!", "")

            try {
                kord.retrieveUserById(id).await()
            } catch (e: IllegalArgumentException) {
                throw DiscordRelayedException(
                    context.translate("converters.user.error.invalid", replacements = arrayOf(id))
                )
            }
        } else {
            try { // Try for a user ID first
                kord.retrieveUserById(arg).await()
            } catch (e: IllegalArgumentException) { // It's not an ID, let's try the tag
                if (!arg.contains("#")) {
                    (context.guild?.members?.firstOrNull { it.effectiveName.startsWith(arg, false) } ?:
                    context.guild?.members?.firstOrNull { it.effectiveName.startsWith(arg, true) }) ?.user
                } else {
                    kord.users.firstOrNull { user ->
                        user.asTag.equals(arg, true)
                    }
                }
            }
        }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.USER, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.USER) option.asUser else null ?: return false
        this.parsed = optionValue
        return true
    }
}
