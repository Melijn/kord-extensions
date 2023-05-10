/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters.impl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DateTimeConverterTest {

    @Test
    fun `correct simple dateTime with hours and minutes`() {
        val timestamp = "2023-05-10 16:30"
        val parsed = DateTimeConverter.parseFromString(timestamp)
        require(parsed != null) {
            "Couldn't parse $timestamp"
        }
        assertEquals(parsed.year, 2023)
        assertEquals(parsed.monthValue, 5)
        assertEquals(parsed.dayOfMonth, 10)
        assertEquals(parsed.hour, 16)
        assertEquals(parsed.minute, 30)
        println("Parsed $parsed")
    }

    @Test
    fun `correct simple dateTime with hours, minutes and seconds`() {
        val timestamp = "2023-05-10 16:30:30"
        val parsed = DateTimeConverter.parseFromString(timestamp)
        require(parsed != null) {
            "Couldn't parse $timestamp"
        }
        assertEquals(parsed.year, 2023)
        assertEquals(parsed.monthValue, 5)
        assertEquals(parsed.dayOfMonth, 10)
        assertEquals(parsed.hour, 16)
        assertEquals(parsed.minute, 30)
        assertEquals(parsed.second, 30)
        println("Parsed $parsed")
    }
}
