/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.events

import com.kotlindiscord.kord.extensions.checks.channelIdFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.interactionFor
import com.kotlindiscord.kord.extensions.checks.userIdFor
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.sentry.SentryContext
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import net.dv8tion.jda.api.events.Event
import org.koin.core.component.inject
import java.util.*

/**
 * Light wrapper representing the context for an event handler's action.
 *
 * This is what `this` refers to in an event handler action body. You shouldn't need to instantiate this yourself.
 *
 * @param eventHandler Respective event handler for this context object.
 * @param event Event that triggered this event handler.
 * @param cache Data cache map shared with the defined checks.
 */
public open class EventContext<T : Event>(
    public open val eventHandler: EventHandler<T>,
    public open val event: T,
    public open val cache: MutableStringKeyedMap<Any>
) : KordExKoinComponent {
    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Current Sentry context, containing breadcrumbs and other goodies. **/
    public val sentry: SentryContext = SentryContext()

    /**
     * Given a translation key and optional bundle name, return the translation for the locale provided by the bot's
     * configured locale resolvers.
     */
    public suspend fun translate(
        key: String,
        bundleName: String?,
        replacements: Array<Any?> = arrayOf()
    ): String {
        val eventObj = event as Event
        var locale: Locale? = null

        val guild = guildFor(eventObj)?.idLong
        val channel = channelIdFor(eventObj)
        val user = userIdFor(eventObj)

        for (resolver in eventHandler.extension.bot.settings.i18nBuilder.localeResolvers) {
            val result = resolver(guild, channel, user, interactionFor(eventObj))

            if (result != null) {
                locale = result
                break
            }
        }

        return if (locale != null) {
            translationsProvider.translate(key, locale, bundleName, replacements)
        } else {
            translationsProvider.translate(key, bundleName, replacements)
        }
    }

    /**
     * Given a translation key and possible replacements,return the translation for the given locale in the
     * extension's configured bundle, for the locale provided by the bot's configured locale resolvers.
     */
    public suspend fun translate(
        key: String,
        replacements: Array<Any?> = arrayOf()
    ): String = translate(key, eventHandler.extension.bundle, replacements)
}
