/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("Filename")

package com.kotlindiscord.kord.extensions.commands.converters

import com.kotlindiscord.kord.extensions.commands.converters.builders.ValidationContext
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction

/** Type alias representing a validator callable. Keeps things relatively maintainable. **/
public typealias Validator<T> = (suspend ValidationContext<T>.() -> Unit)?

/** Type alias representing a mutator callable. Keeps things relatively maintainable. **/
public typealias Mutator<T> = ((T) -> T)?

/** Type alias representing an autocomplete callable. **/
public typealias AutoCompleteCallback =
    (suspend CommandAutoCompleteInteraction.(event: CommandAutoCompleteInteractionEvent) -> Unit)?
