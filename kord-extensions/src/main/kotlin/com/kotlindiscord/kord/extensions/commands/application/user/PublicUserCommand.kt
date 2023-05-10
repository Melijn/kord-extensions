/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.commands.application.user

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.events.PublicUserCommandFailedChecksEvent
import com.kotlindiscord.kord.extensions.commands.events.PublicUserCommandFailedWithExceptionEvent
import com.kotlindiscord.kord.extensions.commands.events.PublicUserCommandInvocationEvent
import com.kotlindiscord.kord.extensions.commands.events.PublicUserCommandSucceededEvent
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

/** Public user command. **/
public class PublicUserCommand(
    extension: Extension
) : UserCommand<PublicUserCommandContext>(extension) {
    /** @suppress Internal guilder **/
    public var initialResponseBuilder: InitialUserResponseBuilder = null

    /** Call this to open with a response, omit it to ack instead. **/
    public fun initialResponse(body: InitialUserResponseBuilder) {
        initialResponseBuilder = body
    }

    override suspend fun call(event: UserContextInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        val invocationEvent = PublicUserCommandInvocationEvent(this, event)
        emitEventAsync(invocationEvent)

        try {
            // cooldown and rate-limits
            if (useLimited(invocationEvent)) return

            // checks
            if (!runChecks(event, cache)) {
                emitEventAsync(
                    PublicUserCommandFailedChecksEvent(
                        this,
                        event,
                        "Checks failed without a message."
                    )
                )

                return
            }
        } catch (e: DiscordRelayedException) {
            event.interaction.reply(
                MessageCreate {
                settings.failureResponseBuilder(this, e.reason, FailureReason.ProvidedCheckFailure(e))
            }
            ).await()

            emitEventAsync(PublicUserCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        val response = if (initialResponseBuilder != null) {
            event.interaction.reply(MessageCreate { initialResponseBuilder!!(event) }).await()
        } else if (this.deferByDefault) {
            event.interaction.deferReply().await()
        } else {
            event.interaction.hook
        }

        val context = PublicUserCommandContext(event, this, response, cache)

        firstSentryBreadcrumb(context)

        try {
            checkBotPerms(context)
        } catch (e: DiscordRelayedException) {
            respondText(context, e.reason, FailureReason.OwnPermissionsCheckFailure(e))
            emitEventAsync(PublicUserCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        try {
            body(context)
        } catch (t: Throwable) {
            emitEventAsync(PublicUserCommandFailedWithExceptionEvent(this, event, t))
            onSuccessUseLimitUpdate(context, invocationEvent, true)

            if (t is DiscordRelayedException) {
                respondText(context, t.reason, FailureReason.RelayedFailure(t))

                return
            }

            handleError(context, t)
        }

        onSuccessUseLimitUpdate(context, invocationEvent, true)
        emitEventAsync(PublicUserCommandSucceededEvent(this, event))
    }

    override suspend fun respondText(
        context: PublicUserCommandContext,
        message: String,
        failureType: FailureReason<*>
    ) {
        context.respond { settings.failureResponseBuilder(this, message, failureType) }
    }
}
