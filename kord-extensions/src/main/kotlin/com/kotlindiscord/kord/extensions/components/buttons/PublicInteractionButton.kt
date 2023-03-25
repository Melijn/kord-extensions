/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.components.buttons

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.components.callbacks.PublicButtonCallback
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.internal.interactions.component.ButtonImpl

public typealias InitialPublicButtonResponseBuilder =
    (suspend InlineMessage<MessageCreateData>.(ButtonInteractionEvent) -> Unit)?

/** Class representing a public-only button component. **/
public open class PublicInteractionButton(
    timeoutTask: Task?
) : InteractionButtonWithAction<PublicInteractionButtonContext>(timeoutTask) {
    /** Button style - anything but Link is valid. **/
    public open var style: ButtonStyle = ButtonStyle.PRIMARY

    /** @suppress Initial response builder. **/
    public open var initialResponseBuilder: InitialPublicButtonResponseBuilder = null

    /** Call this to open with a response, omit it to ack instead. **/
    public fun initialResponse(body: InitialPublicButtonResponseBuilder) {
        initialResponseBuilder = body
    }

    override fun useCallback(id: String) {
        action {
            val callback: PublicButtonCallback = callbackRegistry.getOfTypeOrNull(id)
                ?: error("Callback \"$id\" is either missing or is the wrong type.")

            callback.call(this)
        }

        check {
            val callback: PublicButtonCallback = callbackRegistry.getOfTypeOrNull(id)
                ?: error("Callback \"$id\" is either missing or is the wrong type.")

            passed = callback.runChecks(event, cache)
        }
    }

    override fun apply(builder: MutableList<ItemComponent>) {
        builder.add(ButtonImpl(id, label, style, disabled, partialEmoji))
    }

    override suspend fun call(event: ButtonInteractionEvent): Unit = withLock {
        val cache: MutableStringKeyedMap<Any> = mutableMapOf()

        super.call(event)

        try {
            if (!runChecks(event, cache)) {
                return@withLock
            }
        } catch (e: DiscordRelayedException) {
            event.interaction.reply(MessageCreate {
                settings.failureResponseBuilder(this, e.reason, FailureReason.ProvidedCheckFailure(e))
            })

            return@withLock
        }

        val response = if (initialResponseBuilder != null) {
            event.interaction.reply(MessageCreate {
                initialResponseBuilder!!(event)
            }).await()
        } else {
            if (!deferredAck) {
                event.interaction.deferReply().await()
            } else {
                event.interaction.deferEdit().await()
            }
        }

        val context = PublicInteractionButtonContext(this, event, response, cache)

        context.populate()

        firstSentryBreadcrumb(context, this)

        try {
            checkBotPerms(context)
        } catch (e: DiscordRelayedException) {
            respondText(context, e.reason, FailureReason.OwnPermissionsCheckFailure(e))

            return@withLock
        }

        try {
            body(context)
        } catch (e: DiscordRelayedException) {
            respondText(context, e.reason, FailureReason.RelayedFailure(e))
        } catch (t: Throwable) {
            handleError(context, t, this)
        }
    }

    override fun validate() {
        super.validate()

        if (style == ButtonStyle.LINK) {
            error("The Link button style is reserved for link buttons.")
        }
    }

    override suspend fun respondText(
        context: PublicInteractionButtonContext,
        message: String,
        failureType: FailureReason<*>
    ) {
        context.respond { settings.failureResponseBuilder(this, message, failureType) }
    }
}
