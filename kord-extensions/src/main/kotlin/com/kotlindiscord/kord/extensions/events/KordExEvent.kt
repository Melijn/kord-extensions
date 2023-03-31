/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.events

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.data.DataObject

/**
 * Base interface for events fired by Kord Extensions.
 */
public interface KordExEvent : KordExKoinComponent, GenericEvent {
    override fun getJDA(): JDA {
        val shardManager: ShardManager by getKoin().inject()
        return shardManager.getShardById(0)!!
    }

    override fun getRawData(): DataObject? = null

    override fun getResponseNumber(): Long = 5
}
