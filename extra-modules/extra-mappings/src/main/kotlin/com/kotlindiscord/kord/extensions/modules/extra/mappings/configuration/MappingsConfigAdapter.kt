/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.modules.extra.mappings.configuration

/**
 * Simple config adapter interface, which you can implement yourself if you need some kind of alternative config
 * backend.
 */
interface MappingsConfigAdapter {
    /** Get a list of category IDs mappings commands are explicitly allowed in. **/
    suspend fun getAllowedCategories(): List<Long>

    /** Get a list of category IDs mappings commands are explicitly disallowed in. **/
    suspend fun getBannedCategories(): List<Long>

    /** Get a list of channel IDs mappings commands are explicitly allowed in. **/
    suspend fun getAllowedChannels(): List<Long>

    /** Get a list of channel IDs mappings commands are explicitly disallowed in. **/
    suspend fun getBannedChannels(): List<Long>

    /** Get a list of guild IDs mappings commands are explicitly allowed in. **/
    suspend fun getAllowedGuilds(): List<Long>

    /** Get a list of guild IDs mappings commands are explicitly disallowed in. **/
    suspend fun getBannedGuilds(): List<Long>

    /** Get a list of enabled mappings namespaces. **/
    suspend fun getEnabledNamespaces(): List<String>

    /** Get the paginator timeout, in seconds. **/
    suspend fun getTimeout(): Long
}
