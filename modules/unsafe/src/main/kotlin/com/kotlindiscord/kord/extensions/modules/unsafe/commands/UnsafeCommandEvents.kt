/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(UnsafeAPI::class)

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.ArgumentParsingException
import com.kotlindiscord.kord.extensions.commands.events.*
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

// region Message commands

/** Event emitted when an unsafe message command is invoked. **/
public data class UnsafeMessageCommandInvocationEvent(
    override val command: UnsafeMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandInvocationEvent<UnsafeMessageCommand>

/** Event emitted when an unsafe message command invocation succeeds. **/
public data class UnsafeMessageCommandSucceededEvent(
    override val command: UnsafeMessageCommand,
    override val event: MessageContextInteractionEvent
) : MessageCommandSucceededEvent<UnsafeMessageCommand>

/** Event emitted when an unsafe message command's checks fail. **/
public data class UnsafeMessageCommandFailedChecksEvent(
    override val command: UnsafeMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val reason: String
) : MessageCommandFailedChecksEvent<UnsafeMessageCommand>

/** Event emitted when an unsafe message command invocation fails with an exception. **/
public data class UnsafeMessageCommandFailedWithExceptionEvent(
    override val command: UnsafeMessageCommand,
    override val event: MessageContextInteractionEvent,
    override val throwable: Throwable
) : MessageCommandFailedWithExceptionEvent<UnsafeMessageCommand>

// endregion

// region Slash commands

/** Event emitted when an unsafe slash command is invoked. **/
public data class UnsafeSlashCommandInvocationEvent(
    override val command: UnsafeSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandInvocationEvent<UnsafeSlashCommand<*>>

/** Event emitted when an unsafe slash command invocation succeeds. **/
public data class UnsafeSlashCommandSucceededEvent(
    override val command: UnsafeSlashCommand<*>,
    override val event: SlashCommandInteractionEvent
) : SlashCommandSucceededEvent<UnsafeSlashCommand<*>>

/** Event emitted when an unsafe slash command's checks fail. **/
public data class UnsafeSlashCommandFailedChecksEvent(
    override val command: UnsafeSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val reason: String
) : SlashCommandFailedChecksEvent<UnsafeSlashCommand<*>>

/** Event emitted when an unsafe slash command's argument parsing fails. **/
public data class UnsafeSlashCommandFailedParsingEvent(
    override val command: UnsafeSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val exception: ArgumentParsingException,
) : SlashCommandFailedParsingEvent<UnsafeSlashCommand<*>>

/** Event emitted when an unsafe slash command invocation fails with an exception. **/
public data class UnsafeSlashCommandFailedWithExceptionEvent(
    override val command: UnsafeSlashCommand<*>,
    override val event: SlashCommandInteractionEvent,
    override val throwable: Throwable
) : SlashCommandFailedWithExceptionEvent<UnsafeSlashCommand<*>>

// endregion

// region User commands

/** Event emitted when an unsafe user command is invoked. **/
public data class UnsafeUserCommandInvocationEvent(
    override val command: UnsafeUserCommand,
    override val event: UserContextInteractionEvent
) : UserCommandInvocationEvent<UnsafeUserCommand>

/** Event emitted when an unsafe user command invocation succeeds. **/
public data class UnsafeUserCommandSucceededEvent(
    override val command: UnsafeUserCommand,
    override val event: UserContextInteractionEvent
) : UserCommandSucceededEvent<UnsafeUserCommand>

/** Event emitted when an unsafe user command's checks fail. **/
public data class UnsafeUserCommandFailedChecksEvent(
    override val command: UnsafeUserCommand,
    override val event: UserContextInteractionEvent,
    override val reason: String
) : UserCommandFailedChecksEvent<UnsafeUserCommand>

/** Event emitted when an unsafe user command invocation fails with an exception. **/
public data class UnsafeUserCommandFailedWithExceptionEvent(
    override val command: UnsafeUserCommand,
    override val event: UserContextInteractionEvent,
    override val throwable: Throwable
) : UserCommandFailedWithExceptionEvent<UnsafeUserCommand>

// endregion
