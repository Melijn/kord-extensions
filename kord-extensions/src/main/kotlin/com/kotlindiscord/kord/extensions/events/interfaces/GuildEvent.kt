/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.events.interfaces

import net.dv8tion.jda.api.entities.Guild

/** Generic interface for custom events that can contain guild behaviors. Mostly used by checks. **/
public interface GuildEvent {
    /** The guild behavior for this event, if any. **/
    public val guild: Guild?

    /** Get a Guild object, or throw if one can't be retrieved. **/
    public suspend fun getGuild(): Guild

    /** Get a Guild object, or return null if one can't be retrieved. **/
    public suspend fun getGuildOrNull(): Guild?
}
