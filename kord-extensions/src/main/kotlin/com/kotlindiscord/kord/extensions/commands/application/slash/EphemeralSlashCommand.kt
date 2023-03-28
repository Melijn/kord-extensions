/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.commands.application.slash

import com.kotlindiscord.kord.extensions.ArgumentParsingException
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.events.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateData

public typealias InitialSlashResponseBuilder =
    (suspend InlineMessage<MessageCreateData>.(SlashCommandInteractionEvent) -> Unit)?

/** Ephemeral slash command. **/
public class EphemeralSlashCommand<A : Arguments>(
    extension: Extension,

    public override val arguments: (() -> A)? = null,
    public override val parentCommand: SlashCommand<*, *>? = null,
    public override val parentGroup: SlashGroup? = null,
) : SlashCommand<EphemeralSlashCommandContext<A>, A>(extension) {
    /** @suppress Internal guilder **/
    public var initialResponseBuilder: InitialSlashResponseBuilder = null

    /** Call this to open with a response, omit it to ack instead. **/
    public fun initialResponse(body: InitialSlashResponseBuilder) {
        initialResponseBuilder = body
    }

    override suspend fun call(event: SlashCommandInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        findCommand(event).run(event, cache)
    }

    override suspend fun run(event: SlashCommandInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        val invocationEvent = EphemeralSlashCommandInvocationEvent(this, event)
        emitEventAsync(invocationEvent)

        try {
            // cooldown and rate-limits
            if (useLimited(invocationEvent)) return

            // checks
            if (!runChecks(event, cache)) {
                emitEventAsync(
                    EphemeralSlashCommandFailedChecksEvent(
                        this,
                        event,
                        "Checks failed without a message."
                    )
                )

                return
            }
        } catch (e: DiscordRelayedException) {
            event.interaction.reply(MessageCreate {
                settings.failureResponseBuilder(this, e.reason, FailureReason.ProvidedCheckFailure(e))
            }).setEphemeral(true).await()

            emitEventAsync(
                EphemeralSlashCommandFailedChecksEvent(
                    this,
                    event,
                    e.reason
                )
            )

            return
        }

        val response = if (initialResponseBuilder != null) {
            event.interaction.reply(MessageCreate { initialResponseBuilder!!(event) }).setEphemeral(true).await()
        } else {
            event.interaction.deferReply(true).await()
        }

        val context = EphemeralSlashCommandContext(event, this, response, cache)

        firstSentryBreadcrumb(context, this)

        try {
            checkBotPerms(context)
        } catch (e: DiscordRelayedException) {
            respondText(context, e.reason, FailureReason.OwnPermissionsCheckFailure(e))

            emitEventAsync(
                EphemeralSlashCommandFailedChecksEvent(
                    this,
                    event,
                    e.reason
                )
            )

            return
        }
        if (arguments != null) {
            try {
                val args = registry.argumentParser.parse(arguments, context)

                context.populateArgs(args)
            } catch (e: ArgumentParsingException) {
                respondText(context, e.reason, FailureReason.ArgumentParsingFailure(e))
                emitEventAsync(EphemeralSlashCommandFailedParsingEvent(this, event, e))

                return
            }
        }

        try {
            body(context)
        } catch (t: Throwable) {
            emitEventAsync(EphemeralSlashCommandFailedWithExceptionEvent(this, event, t))
            onSuccessUseLimitUpdate(context, invocationEvent, false)

            if (t is DiscordRelayedException) {
                respondText(context, t.reason, FailureReason.RelayedFailure(t))

                return
            }

            handleError(context, t, this)

            return
        }

        onSuccessUseLimitUpdate(context, invocationEvent, true)
        emitEventAsync(EphemeralSlashCommandSucceededEvent(this, event))
    }

    override suspend fun respondText(
        context: EphemeralSlashCommandContext<A>,
        message: String,
        failureType: FailureReason<*>,
    ) {
        context.respond { settings.failureResponseBuilder(this, message, failureType) }
    }
}
