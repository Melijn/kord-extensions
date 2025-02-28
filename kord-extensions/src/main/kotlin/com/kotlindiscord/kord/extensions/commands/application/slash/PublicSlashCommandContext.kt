/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.slash

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.CommandInteraction

/** Public-only slash command context. **/
public class PublicSlashCommandContext<A : Arguments>(
    event: SlashCommandInteractionEvent,
    override val command: SlashCommand<PublicSlashCommandContext<A>, A>,
    override val interaction: CommandInteraction,
    override var interactionResponse: InteractionHook,
    cache: MutableStringKeyedMap<Any>,
) : SlashCommandContext<PublicSlashCommandContext<A>, A>(event, command, cache), PublicInteractionContext
