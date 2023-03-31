/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.usagelimits

import com.kotlindiscord.kord.extensions.commands.events.ApplicationCommandInvocationEvent
import com.kotlindiscord.kord.extensions.commands.events.ChatCommandInvocationEvent
import com.kotlindiscord.kord.extensions.commands.events.CommandInvocationEvent
import com.kotlindiscord.kord.extensions.utils.getLocale
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.*

/** Data holder for information about command invocation. **/
public data class DiscriminatingContext(
    /** Command invoker's [UserData]. **/
    public val user: User,
    /** [MessageChannelBehavior] of the messageChannel in which the command was invoked. **/
    public val channel: MessageChannel,
    /** guildId of the Guild in which the command was invoked, can be null if the command was invoked
     * in DMs. **/
    public val guildId: Long?,
    /** Command invoker's [UserData]. **/
    public val event: CommandInvocationEvent<*, *>,
    /** Locale of this command's executor. **/
    public val locale: suspend () -> Locale
) {
    public constructor(
        event: ChatCommandInvocationEvent,
    ) : this(
        event.event.message.author,
        event.event.message.channel,
        event.event.member?.guild?.idLong,
        event,
        { event.event.getLocale() },
    )

    public constructor(
        event: ApplicationCommandInvocationEvent<*, *>,
    ) : this(
        event.event.interaction.user,
        event.event.interaction.messageChannel,
        event.event.interaction.guild?.idLong,
        event,
        { event.event.getLocale() }
    )
}
