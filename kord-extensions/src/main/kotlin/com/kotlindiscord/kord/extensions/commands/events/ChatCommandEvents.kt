/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.events

import com.kotlindiscord.kord.extensions.ArgumentParsingException
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommand
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/** Event emitted when a chat command is invoked. **/
public data class ChatCommandInvocationEvent(
    override val command: ChatCommand<*>,
    override val event: MessageReceivedEvent
) : CommandInvocationEvent<ChatCommand<*>, MessageReceivedEvent>

/** Event emitted when a chat command invocation succeeds. **/
public data class ChatCommandSucceededEvent(
    override val command: ChatCommand<*>,
    override val event: MessageReceivedEvent
) : CommandSucceededEvent<ChatCommand<*>, MessageReceivedEvent>

/** Event emitted when a chat command's checks fail. **/
public data class ChatCommandFailedChecksEvent(
    override val command: ChatCommand<*>,
    override val event: MessageReceivedEvent,
    override val reason: String,
) : CommandFailedChecksEvent<ChatCommand<*>, MessageReceivedEvent>

/** Event emitted when a chat command's argument parsing fails. **/
public data class ChatCommandFailedParsingEvent(
    override val command: ChatCommand<*>,
    override val event: MessageReceivedEvent,
    override val exception: ArgumentParsingException,
) : CommandFailedParsingEvent<ChatCommand<*>, MessageReceivedEvent>

/** Event emitted when a chat command's invocation fails with an exception. **/
public data class ChatCommandFailedWithExceptionEvent(
    override val command: ChatCommand<*>,
    override val event: MessageReceivedEvent,
    override val throwable: Throwable
) : CommandFailedWithExceptionEvent<ChatCommand<*>, MessageReceivedEvent>
