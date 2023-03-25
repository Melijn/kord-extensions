/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.components.menus

import com.kotlindiscord.kord.extensions.components.Component
import com.kotlindiscord.kord.extensions.types.PublicInteractionContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu


/** Class representing the execution context for a public-only select (dropdown) menu. **/
public class PublicSelectMenuContext<T, S : SelectMenu>(
    override val component: Component,
    override val event: GenericSelectMenuInteractionEvent<T, S>,
    override val interactionResponse: InteractionHook,
    cache: MutableStringKeyedMap<Any>
) : SelectMenuContext(component, event, cache), PublicInteractionContext
