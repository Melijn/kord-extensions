/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands

import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.checks.channelIdFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.interactionFor
import com.kotlindiscord.kord.extensions.checks.userIdFor
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.sentry.SentryContext
import com.kotlindiscord.kord.extensions.usagelimits.cooldowns.CooldownType
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import com.kotlindiscord.kord.extensions.utils.scheduling.LOOM
import com.kotlindiscord.kord.extensions.utils.scheduling.TaskConfig
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.Event
import org.koin.core.component.inject
import java.util.*
import kotlin.time.Duration

/**
 * Light wrapper class representing the context for a command's action.
 *
 * This is what `this` refers to in a command action body. You shouldn't have to
 * instantiate this yourself.
 *
 * @param command Respective command for this context object.
 * @param eventObj Event that triggered this command.
 * @param commandName Command name given by the user to invoke the command - lower-cased.
 * @param cache Data cache map shared with the defined checks.
 */
@ExtensionDSL
public abstract class CommandContext(
    public open val command: Command,
    public open val eventObj: Event,
    public open val commandName: String,
    public open val cache: MutableStringKeyedMap<Any>,
) : KordExKoinComponent {
    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Current Sentry context, containing breadcrumbs and other goodies. **/
    public val sentry: SentryContext = SentryContext()

    /** Cached locale variable, stored and retrieved by [getLocale]. **/
    public val resolvedLocale: Deferred<Locale> =
        TaskConfig.coroutineScope.async(Dispatchers.LOOM, CoroutineStart.LAZY) {
            getLocale()
        }

    /**
     * Progressive cooldown counters, can be set using the [inc] and [dec] extension functions on [cooldowns].
     * Or the normal [MutableMap.set].
     *
     * E.g.
     * You have a command that interacts with a user-bound api, and pass on the api rateLimits to the user as cooldown.
     * You can then modify the cooldown based on the api response.
     */
    public open var cooldowns: MutableMap<CooldownType, Duration> = mutableMapOf()

    /** Extract channel information from event data. **/
    public abstract val channel: Channel?

    /** Extract guild information from event data, if that context is available. **/
    public abstract val guild: Guild?

    /** Extract member information from event data, if that context is available. **/
    public abstract val member: Member?

    /** Extract user information from event data, if that context is available. **/
    public abstract val user: User

    /** Resolve the locale for this command context. **/
    private suspend fun getLocale(): Locale {
        var locale: Locale? = null

        val guild = guildFor(eventObj)?.idLong
        val channel = channelIdFor(eventObj)
        val user = userIdFor(eventObj)

        for (resolver in command.extension.bot.settings.i18nBuilder.localeResolvers) {
            val result = resolver(guild, channel, user, interactionFor(eventObj))

            if (result != null) {
                locale = result
                break
            }
        }
        return locale ?: command.extension.bot.settings.i18nBuilder.defaultLocale
    }

    /**
     * Given a translation key and bundle name, return the translation for the locale provided by the bot's configured
     * locale resolvers.
     */
    public suspend fun translate(
        key: String,
        bundleName: String?,
        replacements: Array<Any?> = arrayOf(),
    ): String {
        val locale = getLocale()

        return translationsProvider.translate(key, locale, bundleName, replacements)
    }

    /**
     * Given a translation key and possible replacements,return the translation for the given locale in the
     * extension's configured bundle, for the locale provided by the bot's configured locale resolvers.
     */
    public suspend fun translate(key: String, replacements: Array<Any?> = arrayOf()): String = translate(
        key,
        command.resolvedBundle,
        replacements
    )

    /**
     * Increases the cooldown for [cooldownType] with [amount] duration.
     *
     * @return the new cooldown duration for [cooldownType]
     */
    public fun MutableMap<CooldownType, Duration>.inc(cooldownType: CooldownType, amount: Duration): Duration {
        val currentCooldown = this[cooldownType] ?: Duration.ZERO
        val sum = currentCooldown + amount

        this[cooldownType] = sum

        return sum
    }

    /**
     * Decreases the cooldown for [cooldownType] with [amount] duration.
     *
     * @return the new cooldown duration for [cooldownType]
     */
    public fun MutableMap<CooldownType, Duration>.dec(cooldownType: CooldownType, amount: Duration): Duration {
        val currentCooldown = this[cooldownType] ?: Duration.ZERO
        val difference = currentCooldown - amount
        val boundedDifference = if (difference < Duration.ZERO) {
            Duration.ZERO
        } else {
            difference
        }

        this[cooldownType] = boundedDifference

        return boundedDifference
    }

    /**
     * Sets the cooldown for [cooldownType] to [amount] duration.
     */
    public fun setCooldown(cooldownType: CooldownType, amount: Duration) {
        cooldowns[cooldownType] = amount
    }
}
