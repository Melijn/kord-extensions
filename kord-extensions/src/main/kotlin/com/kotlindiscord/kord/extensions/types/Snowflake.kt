/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.types

import net.dv8tion.jda.api.utils.TimeUtil
import java.time.OffsetDateTime

/**
 * Generic snowflake implementation.
 */
@JvmInline
public value class Snowflake(
    /**
     * Id of the snowflake.
     */
    public val id: Long,
) {
    public constructor(id: String) : this(id.toLong())

    /**
     * The Snowflake id of this entity. This is unique to every entity and will never change.
     *
     * @return Long containing the Id.
     */
    public fun getIdLong(): Long = id

    /**
     * The Snowflake id of this entity. This is unique to every entity and will never change.
     *
     * @return Never-null String containing the Id.
     */
    public fun getId(): String = java.lang.Long.toUnsignedString(getIdLong())

    /**
     * The time this entity was created. Calculated through the Snowflake in [.getIdLong].
     *
     * @return OffsetDateTime - Time this entity was created at.
     *
     * @see TimeUtil.getTimeCreated
     */
    public fun getTimeCreated(): OffsetDateTime = TimeUtil.getTimeCreated(getIdLong())
}
