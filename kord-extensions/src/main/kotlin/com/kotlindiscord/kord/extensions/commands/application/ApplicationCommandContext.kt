/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import org.koin.core.component.inject
import java.util.*

/**
 * Base class representing the shared functionality for an application command's context.
 *
 * @param genericEvent Generic event object to populate data from.
 * @param genericCommand Generic command object that this context belongs to.
 */
public abstract class ApplicationCommandContext(
    public val genericEvent: GenericCommandInteractionEvent,
    public val genericCommand: ApplicationCommand<*>,
    cache: MutableStringKeyedMap<Any>
) : CommandContext(genericCommand, genericEvent, genericCommand.name, cache) {
    /** Current bot setting object. **/
    public val botSettings: ExtensibleBotBuilder by inject()

    /**
     * The permissions applicable to your bot in this execution context (guild, roles, channels), or null if
     * this command wasn't executed on a guild.
     */
    public val appPermissions: EnumSet<Permission>? = (genericEvent.interaction as? GuildApplicationCommandInteraction)
        ?.appPermissions
}
