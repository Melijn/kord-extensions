/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.testbot.utils

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.events.EventContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.testbot.TEST_SERVER_ID
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData

public typealias LogBody = (suspend () -> Any?)?

public suspend fun Extension.logRaw(builder: InlineMessage<MessageCreateData>.() -> Unit): Message? {
    val channel = shardManager.getGuildById(TEST_SERVER_ID)
        ?.channels
        ?.filter { it is TextChannel }
        ?.first {
            it.name == "test-logs"
        }

    return (channel as? TextChannel)?.sendMessage(MessageCreate { builder() })?.await()
}

public suspend fun CommandContext.log(level: LogLevel, body: LogBody = null): Message? {
    if (!level.isEnabled()) {
        return null
    }

    val desc = body?.invoke()?.toString()

    return command.extension.logRaw {
        embed {
            this.color = level.color?.rgb

            title = "[${level.name}] Command log: $commandName"
            description = desc

            field {
                name = "Extension"
                value = command.extension.name
            }

            timestamp = Clock.System.now().toJavaInstant()
        }
    }
}

public suspend fun CommandContext.logError(body: LogBody = null): Message? =
    log(LogLevel.ERROR, body)

public suspend fun CommandContext.logWarning(body: LogBody = null): Message? =
    log(LogLevel.WARNING, body)

public suspend fun CommandContext.logInfo(body: LogBody = null): Message? =
    log(LogLevel.INFO, body)

public suspend fun CommandContext.logDebug(body: LogBody = null): Message? =
    log(LogLevel.DEBUG, body)

public suspend fun EventContext<*>.log(
    level: LogLevel,
    body: LogBody = null
): Message? {
    if (!level.isEnabled()) {
        return null
    }

    val desc = body?.invoke()?.toString()
    val eventClass = event::class.simpleName

    return eventHandler.extension.logRaw {
        embed {
            this.color = level.color?.rgb

            title = "[${level.name}] Event log: $eventClass"
            description = desc

            field {
                name = "Extension"
                value = eventHandler.extension.name
            }

            timestamp = Clock.System.now().toJavaInstant()
        }
    }
}

public suspend fun EventContext<*>.logError(body: LogBody = null): Message? =
    log(LogLevel.ERROR, body)

public suspend fun EventContext<*>.logWarning(body: LogBody = null): Message? =
    log(LogLevel.WARNING, body)

public suspend fun EventContext<*>.logInfo(body: LogBody = null): Message? =
    log(LogLevel.INFO, body)

public suspend fun EventContext<*>.logDebug(body: LogBody = null): Message? =
    log(LogLevel.DEBUG, body)
