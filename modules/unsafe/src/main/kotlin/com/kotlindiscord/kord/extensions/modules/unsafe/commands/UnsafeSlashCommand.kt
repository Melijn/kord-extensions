/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.ArgumentParsingException
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.contexts.UnsafeSlashCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondPublic
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

/** Like a standard slash command, but with less safety features. **/
@UnsafeAPI
public class UnsafeSlashCommand<A : Arguments>(
    extension: Extension,

    public override val arguments: (() -> A)? = null,
    public override val parentCommand: SlashCommand<*, *>? = null,
    public override val parentGroup: SlashGroup? = null,
) : SlashCommand<UnsafeSlashCommandContext<A>, A>(extension) {
    /** Initial response type. Change this to decide what happens when this slash command is executed. **/
    public var initialResponse: InitialSlashCommandResponse = InitialSlashCommandResponse.EphemeralAck

    override suspend fun call(event: SlashCommandInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        findCommand(event).run(event, cache)
    }

    override suspend fun run(event: SlashCommandInteractionEvent, cache: MutableStringKeyedMap<Any>) {
        emitEventAsync(UnsafeSlashCommandInvocationEvent(this, event))

        try {
            if (!runChecks(event, cache)) {
                emitEventAsync(
                    UnsafeSlashCommandFailedChecksEvent(
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

            emitEventAsync(UnsafeSlashCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        val response = when (val r = initialResponse) {
            is InitialSlashCommandResponse.EphemeralAck -> event.interaction.deferReply(true).await()
            is InitialSlashCommandResponse.PublicAck -> event.interaction.deferReply(false).await()

            is InitialSlashCommandResponse.EphemeralResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).setEphemeral(true).await()

            is InitialSlashCommandResponse.PublicResponse -> event.interaction.reply(
                MessageCreate {
                    r.builder(this, event)
                }
            ).setEphemeral(false).await()

            is InitialSlashCommandResponse.None -> null
        }

        val context = UnsafeSlashCommandContext(event, this, response, cache)

        firstSentryBreadcrumb(context, this)

        try {
            checkBotPerms(context)
        } catch (e: DiscordRelayedException) {
            respondText(context, e.reason, FailureReason.OwnPermissionsCheckFailure(e))
            emitEventAsync(UnsafeSlashCommandFailedChecksEvent(this, event, e.reason))

            return
        }

        try {
            if (arguments != null) {
                val args = registry.argumentParser.parse(arguments, context)

                context.populateArgs(args)
            }
        } catch (e: ArgumentParsingException) {
            respondText(context, e.reason, FailureReason.ArgumentParsingFailure(e))
            emitEventAsync(UnsafeSlashCommandFailedParsingEvent(this, event, e))

            return
        }

        try {
            body(context)
        } catch (t: Throwable) {
            if (t is DiscordRelayedException) {
                respondText(context, t.reason, FailureReason.RelayedFailure(t))
            }

            emitEventAsync(UnsafeSlashCommandFailedWithExceptionEvent(this, event, t))
            handleError(context, t, this)

            return
        }

        emitEventAsync(UnsafeSlashCommandSucceededEvent(this, event))
    }

    override suspend fun respondText(
        context: UnsafeSlashCommandContext<A>,
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
