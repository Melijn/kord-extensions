/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.usagelimits

import com.kotlindiscord.kord.extensions.usagelimits.cooldowns.CooldownHistory
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.DefaultCooldownHistory
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.DefaultRateLimitHistory
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.RateLimitHistory
import kotlinx.datetime.Instant

private class GenericHistoryStore<Key> {
    private var commandHistory: MutableMap<Key, HistoryHolder> = mutableMapOf()

    fun setCooldownHistory(key: Key, history: CooldownHistory) {
        val rateLimitHistory = getRateLimitHistory(key)
        commandHistory[key] = HistoryHolder(history, rateLimitHistory)
    }

    fun getCooldownHistory(key: Key): CooldownHistory =
        commandHistory[key]?.cooldownHistory ?: DefaultCooldownHistory()

    fun getRateLimitHistory(key: Key): RateLimitHistory =
        commandHistory[key]?.rateLimitHistory ?: DefaultRateLimitHistory()

    fun setRateLimitHistory(key: Key, history: RateLimitHistory) {
        val cooldownHistory = getCooldownHistory(key)
        commandHistory[key] = HistoryHolder(cooldownHistory, history)
    }
}

private data class HistoryHolder(
    val cooldownHistory: CooldownHistory,
    val rateLimitHistory: RateLimitHistory,
)

/**
 * Implementations for [CommandLimitType] using in-memory maps.
 * Each implementation provides a different way of storing the cooldowns and usageHistory (for ratelimits).
 *
 * @see CommandLimitType
 */
public sealed class CachedCommandLimitTypes<Key> : CommandLimitType {

    private val cooldowns = mutableMapOf<Key, Instant>()
    private val commandHistory = GenericHistoryStore<Key>()

    public abstract fun idExtractor(context: DiscriminatingContext): Key?

    override suspend fun setCooldown(context: DiscriminatingContext, until: Instant) {
        val id = idExtractor(context) ?: return
        cooldowns[id] = until
    }

    override suspend fun getCooldown(context: DiscriminatingContext): Instant =
        cooldowns[idExtractor(context)] ?: Instant.DISTANT_PAST

    override suspend fun setCooldownUsageHistory(context: DiscriminatingContext, usageHistory: CooldownHistory) {
        val id = idExtractor(context) ?: return
        commandHistory.setCooldownHistory(id, usageHistory)
    }

    override suspend fun getCooldownUsageHistory(context: DiscriminatingContext): CooldownHistory {
        val id = idExtractor(context) ?: return DefaultCooldownHistory()
        return commandHistory.getCooldownHistory(id)
    }

    override suspend fun setRateLimitUsageHistory(context: DiscriminatingContext, rateLimitHistory: RateLimitHistory) {
        val id = idExtractor(context) ?: return
        commandHistory.setRateLimitHistory(id, rateLimitHistory)
    }

    override suspend fun getRateLimitUsageHistory(context: DiscriminatingContext): RateLimitHistory {
        val id = idExtractor(context) ?: return DefaultRateLimitHistory()
        return commandHistory.getRateLimitHistory(id)
    }

    /**
     * Stores command limits per (command, user).
     *
     * E.g.
     * A user that uses the same command in different channels will share the command limit.
     */
    public object CommandUser : CachedCommandLimitTypes<Pair<Long, String>>() {

        override fun idExtractor(context: DiscriminatingContext): Pair<Long, String> {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong

            return Pair(userId, commandId)
        }
    }

    /**
     * Stores command limits per (command, channel).
     *
     * E.g.
     * Different users that use the same command in the same channel will share the command limit.
     */
    public object CommandUserChannel : CachedCommandLimitTypes<Triple<Long, Long, String>>() {

        override fun idExtractor(context: DiscriminatingContext): Triple<Long, Long, String> {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            return Triple(userId, channelId, commandId)
        }
    }

    /**
     * Stores command limits per (command, guild).
     *
     * E.g.
     * Different users that use the same command in the same guild will share the command limit.
     */
    public object CommandUserGuild : CachedCommandLimitTypes<Triple<Long, Long, String>>() {

        override fun idExtractor(context: DiscriminatingContext): Triple<Long, Long, String>? {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val guildId = context.guildId ?: return null

            return Triple(userId, guildId, commandId)
        }
    }

    /**
     * Stores limits per (user).
     *
     * A global commandLimit for a specific user across all commands and all places.
     */
    public object GlobalUser : CachedCommandLimitTypes<Long>() {

        override fun idExtractor(context: DiscriminatingContext): Long = context.user.idLong
    }

    /**
     * Stores limits per (user, channel).
     *
     * A global commandLimit for a specific user across all commands in a channel.
     */
    public object GlobalUserChannel : CachedCommandLimitTypes<Pair<Long, Long>>() {

        override fun idExtractor(context: DiscriminatingContext): Pair<Long, Long> {
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            return Pair(userId, channelId)
        }
    }

    /**
     * Stores limits per (user, guild).
     *
     * A global commandLimit for a specific user across all commands in a server/guild.
     */
    public object GlobalUserGuild : CachedCommandLimitTypes<Pair<Long, Long>>() {

        override fun idExtractor(context: DiscriminatingContext): Pair<Long, Long>? {
            val userId = context.user.idLong
            val guildId = context.guildId ?: return null

            return Pair(userId, guildId)
        }
    }

    /**
     * Stores limits per (channel).
     *
     * A global commandLimit for a channel.
     * Any user in the channel will share the same command limit.
     */
    public object GlobalChannel : CachedCommandLimitTypes<Long>() {

        override fun idExtractor(context: DiscriminatingContext): Long = context.channel.idLong
    }

    /**
     * Stores limits per (guild).
     *
     * A global commandLimit for a guild.
     * Any user in the guild will share the same command limit.
     */
    public object GlobalGuild : CachedCommandLimitTypes<Long>() {

        override fun idExtractor(context: DiscriminatingContext): Long? = context.guildId
    }
}
