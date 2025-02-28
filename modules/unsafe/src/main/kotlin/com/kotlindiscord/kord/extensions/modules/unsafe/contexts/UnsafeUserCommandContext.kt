/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.modules.unsafe.contexts

import com.kotlindiscord.kord.extensions.commands.application.user.UserCommand
import com.kotlindiscord.kord.extensions.commands.application.user.UserCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.types.UnsafeInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Command context for an unsafe user command. **/
@UnsafeAPI
public class UnsafeUserCommandContext(
    event: UserContextInteractionEvent,
    override val command: UserCommand<UnsafeUserCommandContext>,
    override var interactionResponse: InteractionHook?,
    cache: MutableStringKeyedMap<Any>
) : UserCommandContext<UnsafeUserCommandContext>(event, command, cache), UnsafeInteractionContext
