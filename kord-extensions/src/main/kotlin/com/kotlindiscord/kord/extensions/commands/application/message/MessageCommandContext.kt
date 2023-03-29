/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.message

import com.kotlindiscord.kord.extensions.commands.application.ApplicationCommandContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

/**
 *  Message command context, containing everything you need for your message command's execution.
 *
 *  @param event Event that triggered this message command.
 *  @param command Message command instance.
 */
public abstract class MessageCommandContext<C : MessageCommandContext<C>>(
    public val event: MessageContextInteractionEvent,
    public override val command: MessageCommand<C>,
    cache: MutableStringKeyedMap<Any>
) : ApplicationCommandContext(event, command, cache) {

    /** Messages that this message command is being executed against. **/
    public val targetMessage: Message = event.target

    override val channel: Channel = targetMessage.channel
    override val guild: Guild? = event.guild
    override val member: Member? = event.member
    override val user: User = event.user
}
