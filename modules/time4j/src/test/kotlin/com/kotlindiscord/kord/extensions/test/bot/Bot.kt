/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.test.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import org.koin.core.logger.Level

val TEST_SERVER_ID = 787452339908116521L

suspend fun main() {
    val bot = ExtensibleBot(env("TOKEN")) {
        koinLogLevel = Level.DEBUG

        chatCommands {
            defaultPrefix = "?"

            prefix { default ->
                if (this.guild.idLong == TEST_SERVER_ID) {
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
