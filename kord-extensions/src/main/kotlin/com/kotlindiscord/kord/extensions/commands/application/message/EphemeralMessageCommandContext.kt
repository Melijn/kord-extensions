/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.message

import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Ephemeral-only message command context. **/
public class EphemeralMessageCommandContext(
    event: MessageContextInteractionEvent,
    override val command: MessageCommand<EphemeralMessageCommandContext>,
    override val interactionResponse: InteractionHook,
    cache: MutableStringKeyedMap<Any>
) : MessageCommandContext<EphemeralMessageCommandContext>(event, command, cache), EphemeralInteractionContext
