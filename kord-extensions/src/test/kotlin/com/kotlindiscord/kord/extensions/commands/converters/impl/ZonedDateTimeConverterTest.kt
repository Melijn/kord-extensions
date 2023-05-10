/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ZonedDateTimeConverterTest {

    @Test
    fun `zonedDateTime with offset and zone with daylightsaving`() {
        val timestamp = "2011-12-03T10:15:30+01:00[Europe/Brussels]"
        val parsed = ZonedDateTimeConverter.parseFromString(timestamp)
        requireNotNull(parsed) {
            "Failed to parse $timestamp"
        }
        Assertions.assertEquals(parsed.zone, ZoneId.of("Europe/Brussels"))
        Assertions.assertEquals(parsed.year, 2011)
        Assertions.assertEquals(parsed.offset, ZoneOffset.ofHours(1))
    }

    @Test
    fun `zonedDateTime with offset and incorrect zone with daylightsaving`() {
        val timestamp = "2011-12-03T10:15:30+01:00[EuropeBrussels]"
        val parsed = ZonedDateTimeConverter.parseFromString(timestamp)
        require(parsed == null) {
            "Parsed incorrect format"
        }
    }
}
