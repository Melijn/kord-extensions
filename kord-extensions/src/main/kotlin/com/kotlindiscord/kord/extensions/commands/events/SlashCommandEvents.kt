/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.events

import com.kotlindiscord.kord.extensions.ArgumentParsingException
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

// region Invocation events

/** Basic event emitted when a slash command is invoked. **/
public interface SlashCommandInvocationEvent<C : SlashCommand<*, *>> :
    ApplicationCommandInvocationEvent<C, SlashCommandInteractionEvent>

/** Event emitted when an ephemeral slash command is invoked. **/
public data class EphemeralSlashCommandInvocationEvent(
    override val command: EphemeralSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandInvocationEvent<EphemeralSlashCommand<*>>

/** Event emitted when a public slash command is invoked. **/
public data class PublicSlashCommandInvocationEvent(
    override val command: PublicSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandInvocationEvent<PublicSlashCommand<*>>

// endregion

// region Succeeded events

/** Basic event emitted when a slash command invocation succeeds. **/
public interface SlashCommandSucceededEvent<C : SlashCommand<*, *>> :
    CommandSucceededEvent<C, SlashCommandInteractionEvent>

/** Event emitted when an ephemeral slash command invocation succeeds. **/
public data class EphemeralSlashCommandSucceededEvent(
    override val command: EphemeralSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandSucceededEvent<EphemeralSlashCommand<*>>

/** Event emitted when a public slash command invocation succeeds. **/
public data class PublicSlashCommandSucceededEvent(
    override val command: PublicSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandSucceededEvent<PublicSlashCommand<*>>

// endregion

// region Failed events

/** Basic event emitted when a slash command invocation fails. **/
public sealed interface SlashCommandFailedEvent<C : SlashCommand<*, *>> :
    CommandFailedEvent<C, SlashCommandInteractionEvent>

/** Basic event emitted when a slash command's checks fail. **/
public interface SlashCommandFailedChecksEvent<C : SlashCommand<*, *>> :
    SlashCommandFailedEvent<C>, CommandFailedChecksEvent<C, SlashCommandInteractionEvent>

/** Event emitted when an ephemeral slash command's checks fail. **/
public data class EphemeralSlashCommandFailedChecksEvent(
    override val command: EphemeralSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val reason: String,
) : SlashCommandFailedChecksEvent<EphemeralSlashCommand<*>>

/** Event emitted when a public slash command's checks fail. **/
public data class PublicSlashCommandFailedChecksEvent(
    override val command: PublicSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val reason: String,
) : SlashCommandFailedChecksEvent<PublicSlashCommand<*>>

/** Basic event emitted when a slash command's argument parsing fails'. **/
public interface SlashCommandFailedParsingEvent<C : SlashCommand<*, *>> :
    SlashCommandFailedEvent<C>, CommandFailedParsingEvent<C, SlashCommandInteractionEvent>

/** Event emitted when an ephemeral slash command's argument parsing fails'. **/
public data class EphemeralSlashCommandFailedParsingEvent(
    override val command: EphemeralSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val exception: ArgumentParsingException,
) : SlashCommandFailedParsingEvent<EphemeralSlashCommand<*>>

/** Event emitted when a public slash command's argument parsing fails'. **/
public data class PublicSlashCommandFailedParsingEvent(
    override val command: PublicSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val exception: ArgumentParsingException,
) : SlashCommandFailedParsingEvent<PublicSlashCommand<*>>

/** Basic event emitted when a slash command invocation fails with an exception. **/
public interface SlashCommandFailedWithExceptionEvent<C : SlashCommand<*, *>> :
    SlashCommandFailedEvent<C>, CommandFailedWithExceptionEvent<C, SlashCommandInteractionEvent>

/** Event emitted when an ephemeral slash command invocation fails with an exception. **/
public data class EphemeralSlashCommandFailedWithExceptionEvent(
    override val command: EphemeralSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val throwable: Throwable
) : SlashCommandFailedWithExceptionEvent<EphemeralSlashCommand<*>>

/** Event emitted when a public slash command invocation fails with an exception. **/
public data class PublicSlashCommandFailedWithExceptionEvent(
    override val command: PublicSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val throwable: Throwable
) : SlashCommandFailedWithExceptionEvent<PublicSlashCommand<*>>

// endregion
