/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.usagelimits

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.datetime.Instant
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*

internal fun LinkedList<Instant>.removeSmaller(cutoffTime: Instant) {
    val iterator = this.iterator()

    while (iterator.hasNext()) {
        if (iterator.next() < cutoffTime) {
            iterator.remove()
        } else {
            break
        }
    }
}

/** Send an (ephemeral if possible) message based on the type of event. **/
internal suspend fun Event.sendEphemeralMessage(message: String) {
    when (this) {
        is MessageReceivedEvent -> this.message.channel.sendMessage(message).await()

        is GenericCommandInteractionEvent -> this.interaction.reply(
            MessageCreate {
                this.content = message
            }
        ).setEphemeral(true).await()

        else -> error("Unknown event type: $this")
    }
}
