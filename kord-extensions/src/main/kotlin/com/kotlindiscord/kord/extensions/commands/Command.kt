/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("UnnecessaryAbstractClass")  // No idea why we're getting this

package com.kotlindiscord.kord.extensions.commands

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.events.CommandEvent
import com.kotlindiscord.kord.extensions.commands.events.CommandInvocationEvent
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.sentry.SentryAdapter
import com.kotlindiscord.kord.extensions.types.Lockable
import com.kotlindiscord.kord.extensions.usagelimits.DiscriminatingContext
import com.kotlindiscord.kord.extensions.usagelimits.cooldowns.CooldownType
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.RateLimit
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.RateLimitType
import com.kotlindiscord.kord.extensions.utils.scheduling.TaskConfig
import com.kotlindiscord.kord.extensions.utils.translate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.utils.PermissionUtil
import org.koin.core.component.inject
import java.util.*
import kotlin.time.Duration

/**
 * Abstract base class representing the few things that command objects can have in common.
 *
 * This should be used as a base class only for command types that aren't related to the other command types.
 *
 * @property extension The extension object this command belongs to.
 */
@ExtensionDSL
public abstract class Command(public val extension: Extension) : Lockable, KordExKoinComponent {
    /**
     * The name of this command, for invocation and help commands.
     */
    public open lateinit var name: String

    /** Set this to `true` to lock command execution with a Mutex. **/
    public override var locking: Boolean = false

    /** Translation bundle to use, if not the one provided by the extension. **/
    public var bundle: String? = null

    /**
     * @suppress Bundle getter that exists because the extension bundle may have changed by the time the command is
     * registered.
     */
    public val resolvedBundle: String?
        get() = bundle ?: extension.bundle

    override var mutex: Mutex? = null

    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Bot settings object. **/
    public val settings: ExtensibleBotBuilder by inject()

    /** Sentry adapter, for easy access to Sentry functions. **/
    public val sentry: SentryAdapter by inject()

    /** Kord instance, backing the ExtensibleBot. **/
    public val kord: ShardManager by inject()

    /** Permissions required to be able to run this command. **/
    public open val requiredPerms: MutableSet<Permission> = mutableSetOf()

    /** Translation cache, so we don't have to look up translations every time. **/
    public open val nameTranslationCache: MutableMap<Locale, String> = mutableMapOf()

    /** Command specific cooldown lambdas, stored per [CooldownType], use [cooldown] to set these. **/
    public open val cooldowns: MutableMap<CooldownType, suspend (context: DiscriminatingContext) -> Duration> =
        mutableMapOf()

    /** Command specific ratelimit lambdas, stored per [RateLimitType], use [ratelimit] to set these. **/
    public open val ratelimits: MutableMap<RateLimitType, suspend (context: DiscriminatingContext) -> RateLimit> =
        mutableMapOf()

    // region: DSL functions

    /**
     * Defines a cooldown for this command.
     *
     * @param cooldownType The type of cooldown.
     * @param func Used for retrieving a context specific cooldown.
     */
    public open fun cooldown(cooldownType: CooldownType, func: suspend (context: DiscriminatingContext) -> Duration) {
        cooldowns[cooldownType] = func
    }

    /**
     * Defines a rateLimit for this command.
     *
     * @param rateLimitType The type of rateLimit.
     * @param func Used for retrieving a context specific cooldown.
     */
    public open fun ratelimit(
        rateLimitType: RateLimitType,
        func: suspend (context: DiscriminatingContext) -> RateLimit,
    ) {
        ratelimits[rateLimitType] = func
    }

    // endregion

    /**
     * An internal function used to ensure that all of a command's required arguments are present and correct.
     *
     * @throws InvalidCommandException Thrown when a required argument hasn't been set or is invalid.
     */
    @Throws(InvalidCommandException::class)
    public open fun validate() {
        if (!::name.isInitialized || name.isEmpty()) {
            throw InvalidCommandException(null, "No command name given.")
        }

        if (locking && mutex == null) {
            mutex = Mutex()
        }
    }

    /** Quick shortcut for emitting a command event without blocking. **/
    public open suspend fun emitEventAsync(event: CommandEvent<*, *>): Job =
        TaskConfig.coroutineScope.launch {
            extension.bot.send(event)
        }

    /** Returns the full hierarchy of names until this command, locale can be used for translation. **/
    public open fun getFullName(locale: Locale? = null): String = name

    internal open suspend fun onSuccessUseLimitUpdate(
        commandContext: CommandContext,
        invocationEvent: CommandInvocationEvent<*, *>,
        success: Boolean,
    ) {
    }

    /** Checks whether the bot has the specified required permissions, throwing if it doesn't. **/
    @Throws(DiscordRelayedException::class)
    public open suspend fun checkDiscordPerms(context: CommandContext) {
        if (requiredPerms.isEmpty()) {
            return  // Nothing to check, don't try to hit the cache
        }

        val guild = context.guild ?: return
        val guildChannel = context.channel as GuildChannel
        val effectivePermissions = Permission.getPermissions(
            PermissionUtil.getEffectivePermission(guildChannel, guild.selfMember)
        )

        // create copy and subtract the permissions we have
        val missingPerms = requiredPerms.toMutableSet().subtract(effectivePermissions)
        if (missingPerms.isNotEmpty()) {
            throw DiscordRelayedException(
                context.translate(
                    "commands.error.missingBotPermissions",
                    null,

                    replacements = arrayOf(
                        missingPerms
                            .map { it.translate(context.resolvedLocale.await()) }
                            .joinToString()
                    )
                )
            )
        }
    }
}
