/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import com.kotlindiscord.kord.extensions.time.TimestampType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset

internal class TimestampConverterTest {

    @Test
    fun `timestamp without format`() {
        val timestamp = "<t:1420070400>" // 1st second of 2015
        val parsed = TimestampConverter.parseFromString(timestamp)!!
        assertEquals(Instant.fromEpochSeconds(1_420_070_400), parsed.instant)
        assertEquals(TimestampType.Default, parsed.format)
    }

    @Test
    fun `timestamp with format`() {
        val timestamp = "<t:1420070400:R>"
        val parsed = TimestampConverter.parseFromString(timestamp)!!
        assertEquals(Instant.fromEpochSeconds(1_420_070_400), parsed.instant)
        assertEquals(TimestampType.RelativeTime, parsed.format)
    }

    @Test
    fun `empty timestamp`() {
        val timestamp = "<t::>"
        val parsed = TimestampConverter.parseFromString(timestamp)
        assertNull(parsed)
    }

    @Test
    fun `timestamp with empty format`() {
        val timestamp = "<t:1420070400:>"
        val parsed = TimestampConverter.parseFromString(timestamp)
        assertNull(parsed)
    }

    @Test
    fun `zonedDateTime with offset and zone with daylightsaving`() {
        val timestamp = "2011-12-03T10:15:30+01:00[Europe/Brussels]"
        val parsed = ZonedDateTimeConverter.parseFromString(timestamp)
        requireNotNull(parsed) {
            "Failed to parse $timestamp"
        }
        assertEquals(parsed.zone, ZoneId.of("Europe/Brussels"))
        assertEquals(parsed.year, 2011)
        assertEquals(parsed.offset, ZoneOffset.ofHours(1))
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
