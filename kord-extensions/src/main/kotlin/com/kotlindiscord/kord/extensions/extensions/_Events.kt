/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.extensions

import com.kotlindiscord.kord.extensions.EventHandlerRegistrationException
import com.kotlindiscord.kord.extensions.InvalidEventHandlerException
import com.kotlindiscord.kord.extensions.events.EventHandler
import mu.KotlinLogging
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.requests.GatewayIntent

/**
 * DSL function for easily registering an event handler.
 *
 * Use this in your setup function to register an event handler that reacts to a given event.
 *
 * @param body Builder lambda used for setting up the event handler object.
 */
public suspend inline fun <reified T : Event> Extension.event(
    noinline body: suspend EventHandler<T>.() -> Unit
): EventHandler<T> {
    val eventHandler = EventHandler<T>(this)
    val logger = KotlinLogging.logger {}

    body.invoke(eventHandler)

    try {
        eventHandler.validate()

        eventHandler.listenerRegistrationCallable = {
            eventHandler.job = bot.registerListenerForHandler(eventHandler)
        }

        bot.addEventHandler(eventHandler)
        eventHandlers.add(eventHandler)
    } catch (e: EventHandlerRegistrationException) {
        logger.error(e) { "Failed to register event handler - $e" }
    } catch (e: InvalidEventHandlerException) {
        logger.error(e) { "Failed to register event handler - $e" }
    }

    intents += GatewayIntent.fromEvents(T::class.java)
    return eventHandler
}
