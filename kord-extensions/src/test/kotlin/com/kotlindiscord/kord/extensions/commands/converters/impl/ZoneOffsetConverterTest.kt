/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ZoneOffsetConverterTest {

    @Test
    fun `correct simple zoneOffset`() {
        val timestamp = "+01:00"
        val parsed = ZoneOffsetConverter.parseFromString(timestamp)
        require(parsed != null) {
            "Couldn't parse zoneId"
        }
        assertEquals(parsed.totalSeconds, 3600)
    }
}
