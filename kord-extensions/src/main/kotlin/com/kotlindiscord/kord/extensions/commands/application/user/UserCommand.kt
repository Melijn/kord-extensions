/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application.user

import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.checks.types.CheckContextWithCache
import com.kotlindiscord.kord.extensions.commands.application.ApplicationCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.sentry.tag
import com.kotlindiscord.kord.extensions.sentry.user
import com.kotlindiscord.kord.extensions.types.FailureReason
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import com.kotlindiscord.kord.extensions.utils.getLocale
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command

/** User context command, for right-click actions on users. **/
public abstract class UserCommand<C : UserCommandContext<*>>(
    extension: Extension
) : ApplicationCommand<UserContextInteractionEvent>(extension) {
    private val logger: KLogger = KotlinLogging.logger {}

    /** Command body, to be called when the command is executed. **/
    public lateinit var body: suspend C.() -> Unit

    override val type: Command.Type = Command.Type.USER

    /** Call this to supply a command [body], to be called when the command is executed. **/
    public fun action(action: suspend C.() -> Unit) {
        body = action
    }

    override fun validate() {
        super.validate()

        if (!::body.isInitialized) {
            throw InvalidCommandException(name, "No command body given.")
        }
    }

    /** Override this to implement your command's calling logic. Check subtypes for examples! **/
    public abstract override suspend fun call(
        event: UserContextInteractionEvent,
        cache: MutableStringKeyedMap<Any>
    )

    /** Override this to implement a way to respond to the user, regardless of whatever happens. **/
    public abstract suspend fun respondText(context: C, message: String, failureType: FailureReason<*>)

    /** If enabled, adds the initial Sentry breadcrumb to the given context. **/
    public open suspend fun firstSentryBreadcrumb(context: C) {
        if (sentry.enabled) {
            context.sentry.breadcrumb(BreadcrumbType.User) {
                category = "command.application.user"
                message = "User command \"$name\" called."

                val channel = context.channel
                val guild = context.guild

                data["command"] = name

                if (guildId != null) {
                    data["command.guild"] = guildId.toString()
                }

                if (channel != null) {
                    data["channel"] = when (channel) {
                        is PrivateChannel -> "Private Message (${channel.id})"
                        is GuildMessageChannel -> "#${channel.name} (${channel.id})"

                        else -> channel.id
                    }
                }

                if (guild != null) {
                    data["guild"] = "${guild.name} (${guild.id})"
                }
            }
        }
    }

    override suspend fun runChecks(
        event: UserContextInteractionEvent,
        cache: MutableStringKeyedMap<Any>
    ): Boolean {
        val locale = event.getLocale()
        val result = super.runChecks(event, cache)

        if (result) {
            settings.applicationCommandsBuilder.userCommandChecks.forEach { check ->
                val context = CheckContextWithCache(event, locale, cache)

                check(context)

                if (!context.passed) {
                    context.throwIfFailedWithMessage()

                    return false
                }
            }

            extension.userCommandChecks.forEach { check ->
                val context = CheckContextWithCache(event, locale, cache)

                check(context)

                if (!context.passed) {
                    context.throwIfFailedWithMessage()

                    return false
                }
            }
        }

        return result
    }

    /** A general way to handle errors thrown during the course of a command's execution. **/
    public open suspend fun handleError(context: C, t: Throwable) {
        logger.error(t) { "Error during execution of $name user command (${context.event})" }

        if (sentry.enabled) {
            logger.trace { "Submitting error to sentry." }

            val channel = context.channel
            val author = context.user

            val sentryId = context.sentry.captureException(t) {
                if (author != null) {
                    user(author)
                }

                tag("private", "false")

                if (channel is PrivateChannel) {
                    tag("private", "true")
                }

                tag("command", name)
                tag("extension", extension.name)
            }

            logger.info { "Error submitted to Sentry: $sentryId" }

            val errorMessage = if (extension.bot.extensions.containsKey("sentry")) {
                context.translate("commands.error.user.sentry.slash", null, replacements = arrayOf(sentryId))
            } else {
                context.translate("commands.error.user", null)
            }

            respondText(context, errorMessage, FailureReason.ExecutionError(t))
        } else {
            respondText(
                context,
                context.translate("commands.error.user", null),
                FailureReason.ExecutionError(t)
            )
        }
    }
}
