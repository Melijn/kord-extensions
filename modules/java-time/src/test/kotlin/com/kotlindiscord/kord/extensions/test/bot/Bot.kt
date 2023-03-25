/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.test.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.i18n.SupportedLocales
import com.kotlindiscord.kord.extensions.types.Snowflake
import com.kotlindiscord.kord.extensions.utils.env
import org.koin.core.logger.Level

val TEST_SERVER_ID = Snowflake(787452339908116521UL)

suspend fun main() {
    val bot = ExtensibleBot(env("TOKEN")) {
        koinLogLevel = Level.DEBUG

        i18n {
            localeResolver { _, _, user, _ ->
                @Suppress("UnderscoresInNumericLiterals")
                when (user?.idLong) {
                    560515299388948500 -> SupportedLocales.FINNISH
                    242043299022635020 -> SupportedLocales.FRENCH
                    407110650217627658 -> SupportedLocales.FRENCH
                    667552017434017794 -> SupportedLocales.CHINESE_SIMPLIFIED
                    185461862878543872 -> SupportedLocales.GERMAN

                    else -> defaultLocale
                }
            }
        }

        chatCommands {
            defaultPrefix = "?"

            prefix { default ->
                if (guild.idLong == TEST_SERVER_ID.id) {
                    "!"
                } else {
                    default  // "?"
                }
            }
        }

        extensions {
            add(::TestExtension)
        }
    }

    bot.start()
}
