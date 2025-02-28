/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.modules.unsafe.contexts

import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommand
import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.types.UnsafeInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Command context for an unsafe message command. **/
@UnsafeAPI
public class UnsafeMessageCommandContext(
    event: MessageContextInteractionEvent,
    override val command: MessageCommand<UnsafeMessageCommandContext>,
    override var interactionResponse: InteractionHook?,
    cache: MutableStringKeyedMap<Any>
) : MessageCommandContext<UnsafeMessageCommandContext>(event, command, cache), UnsafeInteractionContext
