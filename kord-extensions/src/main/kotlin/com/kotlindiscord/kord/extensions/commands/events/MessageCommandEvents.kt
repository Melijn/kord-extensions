/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.events

import com.kotlindiscord.kord.extensions.commands.application.message.EphemeralMessageCommand
import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommand
import com.kotlindiscord.kord.extensions.commands.application.message.PublicMessageCommand
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

// region Invocation events

/** Basic event emitted when a message command is invoked. **/
public interface MessageCommandInvocationEvent<C : MessageCommand<*>> :
    ApplicationCommandInvocationEvent<C, MessageContextInteractionEvent>

/** Event emitted when an ephemeral message command is invoked. **/
public data class EphemeralMessageCommandInvocationEvent(
    override val command: EphemeralMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandInvocationEvent<EphemeralMessageCommand>

/** Event emitted when a public message command is invoked. **/
public data class PublicMessageCommandInvocationEvent(
    override val command: PublicMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandInvocationEvent<PublicMessageCommand>

// endregion

// region Succeeded events

/** Basic event emitted when a message command invocation succeeds. **/
public interface MessageCommandSucceededEvent<C : MessageCommand<*>> :
    CommandSucceededEvent<C, MessageContextInteractionEvent>

/** Event emitted when an ephemeral message command invocation succeeds. **/
public data class EphemeralMessageCommandSucceededEvent(
    override val command: EphemeralMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandSucceededEvent<EphemeralMessageCommand>

/** Event emitted when a public message command invocation succeeds. **/
public data class PublicMessageCommandSucceededEvent(
    override val command: PublicMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandSucceededEvent<PublicMessageCommand>

// endregion

// region Failed events

/** Basic event emitted when a message command invocation fails. **/
public sealed interface  MessageCommandFailedEvent<C : MessageCommand<*>> :
    CommandFailedEvent<C, MessageContextInteractionEvent>

/** Basic event emitted when a message command's checks fail. **/
public interface MessageCommandFailedChecksEvent<C : MessageCommand<*>> :
    MessageCommandFailedEvent<C>, CommandFailedChecksEvent<C, MessageContextInteractionEvent>

/** Event emitted when an ephemeral message command's checks fail. **/
public data class EphemeralMessageCommandFailedChecksEvent(
    override val command: EphemeralMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val reason: String
) : MessageCommandFailedChecksEvent<EphemeralMessageCommand>

/** Event emitted when a public message command's checks fail. **/
public data class PublicMessageCommandFailedChecksEvent(
    override val command: PublicMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val reason: String
) : MessageCommandFailedChecksEvent<PublicMessageCommand>

/** Basic event emitted when a message command invocation fails with an exception. **/
public interface MessageCommandFailedWithExceptionEvent<C : MessageCommand<*>> :
    MessageCommandFailedEvent<C>, CommandFailedWithExceptionEvent<C, MessageContextInteractionEvent>

/** Event emitted when an ephemeral message command invocation fails with an exception. **/
public data class EphemeralMessageCommandFailedWithExceptionEvent(
    override val command: EphemeralMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val throwable: Throwable
) : MessageCommandFailedWithExceptionEvent<EphemeralMessageCommand>

/** Event emitted when a public message command invocation fails with an exception. **/
public data class PublicMessageCommandFailedWithExceptionEvent(
    override val command: PublicMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val throwable: Throwable
) : MessageCommandFailedWithExceptionEvent<PublicMessageCommand>

// endregion
