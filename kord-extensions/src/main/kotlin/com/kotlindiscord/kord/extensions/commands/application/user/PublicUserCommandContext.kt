/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.user

import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Public-only user command context. **/
public class PublicUserCommandContext(
    event: UserContextInteractionEvent,
    override val command: UserCommand<PublicUserCommandContext>,
    override val interactionResponse: InteractionHook,
    cache: MutableStringKeyedMap<Any>
) : UserCommandContext<PublicUserCommandContext>(event, command, cache), PublicInteractionContext
