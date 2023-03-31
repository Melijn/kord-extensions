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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Argument converter for discord [Role] arguments.
 *
 * This converter supports specifying roles by supplying:
 * * A role mention
 * * A role ID
 * * A message name - the first matching role from the given guild will be used.
 *
 * @param requiredGuild Lambda returning a specific guild to require the role to be in. If omitted, defaults to the
 * guild the command was invoked in.
 */
@Converter(
    "role",

    types = [ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],
    imports = [],
    builderFields = ["public var requiredGuild: (suspend () -> Long)? = null"]
)
public class RoleConverter(
    private var requiredGuild: (suspend () -> Long)? = null,
    override var validator: Validator<Role> = null
) : SingleConverter<Role>() {
    override val signatureTypeString: String = "converters.role.signatureType"

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
        val arg: String = named ?: parser?.parseNext()?.data ?: return false

        parsed = findRole(arg, context)
            ?: throw DiscordRelayedException(
                context.translate("converters.role.error.missing", replacements = arrayOf(arg))
            )

        return true
    }

    private suspend fun findRole(arg: String, context: CommandContext): Role? {
        val guildId: Long = if (requiredGuild != null) {
            requiredGuild!!.invoke()
        } else {
            context.guild?.idLong
        } ?: return null

        val guild: Guild = kord.getGuildById(guildId) ?: return null

        @Suppress("MagicNumber")
        return if (arg.startsWith("<@&") && arg.endsWith(">")) { // It's a mention
            val id: String = arg.substring(3, arg.length - 1)

            try {
                guild.getRoleById(id)
            } catch (e: NumberFormatException) {
                throw DiscordRelayedException(
                    context.translate("converters.role.error.invalid", replacements = arrayOf(id))
                )
            }
        } else {
            try { // Try for a role ID first
                guild.getRoleById(arg)
            } catch (e: NumberFormatException) { // It's not an ID, let's try the name
                guild.roles.firstOrNull { role ->
                    role.name.equals(arg, true)
                }
            }
        }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionData =
        OptionData(OptionType.STRING, arg.displayName, arg.description, required)

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val optionValue = if (option.type == OptionType.ROLE) option.asRole else return false
        this.parsed = optionValue

        return true
    }
}
