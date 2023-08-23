/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.testbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.testbot.extensions.ArgumentTestExtension
import com.kotlindiscord.kord.extensions.testbot.extensions.I18nTestExtension
import com.kotlindiscord.kord.extensions.testbot.extensions.NestingTestExtension
import com.kotlindiscord.kord.extensions.testbot.extensions.PaginatorTestExtension
import com.kotlindiscord.kord.extensions.testbot.utils.LogLevel
import com.kotlindiscord.kord.extensions.usagelimits.CachedCommandLimitTypes
import com.kotlindiscord.kord.extensions.usagelimits.cooldowns.DefaultCooldownHandler
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.DefaultRateLimiter
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.RateLimit
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.minn.jda.ktx.jdabuilder.injectKTX
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.GatewayIntent.ALL_INTENTS
import org.koin.core.logger.Level
import kotlin.time.Duration.Companion.seconds

public val TEST_SERVER_ID: Long = env("TEST_SERVER").toLong()

public suspend fun main() {
    LogLevel.enabledLevel = LogLevel.fromString(envOrNull("LOG_LEVEL") ?: "INFO") ?: LogLevel.INFO

    ExtensibleBot(env("TOKEN")) {
        koinLogLevel = Level.DEBUG

        kord {
            injectKTX()
            this.setShardsTotal(1)
            this.setShards(0)
        }

        chatCommands {
            enabled = true

            check { isNotBot() }
        }

        applicationCommands {
            enabled = true
            defaultGuild(TEST_SERVER_ID)

            useLimiter {
                // NOTE: You might want to use the same instances for these between chatCommands and
                // application commands.
                // If you use separate instances, the limits will not be shared between the two command types.
                cooldownHandler = DefaultCooldownHandler()
                rateLimiter = DefaultRateLimiter()

                // Example cooldown, users can only run  command every 5 seconds, per-server
                cooldown(CachedCommandLimitTypes.CommandUserGuild) { 5.seconds }

                // Example ratelimit, there can only be 20 commands ran in a channel during the last 60 seconds.
                ratelimit(CachedCommandLimitTypes.GlobalChannel) { RateLimit(true, 20, 60.seconds) }
            }
        }

        intents {
            this.addAll(GatewayIntent.getIntents(ALL_INTENTS))
        }

        i18n {
            interactionUserLocaleResolver()

            applicationCommandLocale(DiscordLocale.CHINESE_CHINA)
            applicationCommandLocale(DiscordLocale.ENGLISH_UK)
            applicationCommandLocale(DiscordLocale.ENGLISH_US)
            applicationCommandLocale(DiscordLocale.GERMAN)
            applicationCommandLocale(DiscordLocale.JAPANESE)
        }

        members {
            all()
        }

        extensions {
            help {
                paginatorTimeout = 30
            }

            extPhishing {
                appName = "Integration test bot"
                logChannelName = "alerts"
            }

            add(::ArgumentTestExtension)
            add(::I18nTestExtension)
            add(::PaginatorTestExtension)
            add(::NestingTestExtension)
        }

        hooks {
            setup {
                this.start()
            }
        }

        plugins {
            pluginPaths.clear()

            pluginPath("test-bot/build/generated/ksp/main/resources")
            pluginPath("extra-modules/extra-mappings/build/generated/ksp/main/resources")
        }
    }
}
