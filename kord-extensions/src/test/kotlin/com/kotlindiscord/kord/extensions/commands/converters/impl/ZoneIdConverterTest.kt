/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class ZoneIdConverterTest {

    @Test
    fun `correct simple europe-brussels zoneId`() {
        val timestamp = "Europe/Brussels"
        val parsed = ZoneIdConverter.parseFromString(timestamp)

        require(parsed != null) {
            "Couldn't parse zoneId"
        }
        require(!parsed.rules.isFixedOffset)

        val first = parsed.rules.transitionRules[0]
        val second = parsed.rules.transitionRules[1]
        requireNotNull(first)
        requireNotNull(second)
        require(first.standardOffset == ZoneOffset.ofHours(1))
        require(first.offsetAfter == ZoneOffset.ofHours(2))
        require(second.offsetAfter == ZoneOffset.ofHours(1))
    }
}
