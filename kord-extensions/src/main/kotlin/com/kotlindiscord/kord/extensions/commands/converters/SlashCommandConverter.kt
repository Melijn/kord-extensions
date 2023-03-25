/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters

import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * Interface representing converters that can be made use of in slash commands.
 */
public interface SlashCommandConverter {
    /**
     * Return a slash command option that corresponds to this converter.
     *
     * Only applicable to converter types that make sense for slash commands.
     */
    public suspend fun toSlashOption(arg: Argument<*>): OptionData

    /** Use the given [option] taken straight from the slash command invocation to fill the converter. **/
    public suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean
}
