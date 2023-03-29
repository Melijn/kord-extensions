/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("Filename")

package com.kotlindiscord.kord.extensions.checks.types

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/** Types alias representing a check function for a specific event type. **/
public typealias Check<T> = suspend CheckContext<T>.() -> Unit

/** Types alias representing a check function for a specific event type. **/
public typealias CheckWithCache<T> = suspend CheckContextWithCache<T>.() -> Unit

/** Check type for chat commands. **/
public typealias ChatCommandCheck = CheckWithCache<MessageReceivedEvent>

/** Check type for message commands. **/
public typealias MessageCommandCheck = CheckWithCache<MessageContextInteractionEvent>

/** Check type for slash commands. **/
public typealias SlashCommandCheck = CheckWithCache<SlashCommandInteractionEvent>

/** Check type for user commands. **/
public typealias UserCommandCheck = CheckWithCache<UserContextInteractionEvent>
