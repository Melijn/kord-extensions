/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.application.user.UserCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.contexts.UnsafeUserCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialUserCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondPublic
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Like a standard user command, but with less safety features. **/
@UnsafeAPI
public class UnsafeUserCommand(
    extension: Extension,
) : UserCommand<UnsafeUserCommandContext>(extension) {
    /** Initial response type. Change this to decide what happens when this user command action is executed. **/
    public var initialResponse: InitialUserCommandResponse = InitialUserCommandResponse.EphemeralAck

    override suspend fun call(event: UserContextInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        emitEventAsync(UnsafeUserCommandInvocationEvent(this, event))

        try {
            if (!runChecks(event, cache)) {
                emitEventAsync(
                    UnsafeUserCommandFailedChecksEvent(
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
            ).setEphemeral(true).await()

            emitEventAsync(UnsafeUserCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        val response = when (val r = initialResponse) {
            is InitialUserCommandResponse.EphemeralAck -> event.interaction.deferReply(true).await()
            is InitialUserCommandResponse.PublicAck -> event.interaction.deferReply(false).await()

            is InitialUserCommandResponse.EphemeralResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).setEphemeral(true).await()

            is InitialUserCommandResponse.PublicResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).setEphemeral(true).await()

            is InitialUserCommandResponse.None -> null
        }

        val context = UnsafeUserCommandContext(event, this, response, cache)

        firstSentryBreadcrumb(context)

        try {
            checkBotPerms(context)
        } catch (t: DiscordRelayedException) {
            emitEventAsync(UnsafeUserCommandFailedChecksEvent(this, event, t.reason))
            respondText(context, t.reason, FailureReason.OwnPermissionsCheckFailure(t))

            return
        }

        try {
            body(context)
        } catch (t: Throwable) {
            if (t is DiscordRelayedException) {
                respondText(context, t.reason, FailureReason.RelayedFailure(t))
            }

            emitEventAsync(UnsafeUserCommandFailedWithExceptionEvent(this, event, t))
            handleError(context, t)

            return
        }

        emitEventAsync(UnsafeUserCommandSucceededEvent(this, event))
    }

    override suspend fun respondText(
        context: UnsafeUserCommandContext,
        message: String,
        failureType: FailureReason<*>,
    ) {
        when (context.interactionResponse) {
            is InteractionHook -> context.respondPublic {
                settings.failureResponseBuilder(this, message, failureType)
            }
        }
    }
}
