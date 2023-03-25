/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.usagelimits

import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.UsageHistory
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.UsageHistoryImpl

/** Local cache implementation for [UsageLimitType]. **/
public enum class CachedUsageLimitType : UsageLimitType {

    // a specific user can have a usageLimit
    COMMAND_USER {
        private val userCommandCooldowns: HashMap<Pair<Long, String>, Long> = HashMap()
        private val userCommandHistory: MutableMap<Pair<Long, String>, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong

            return userCommandCooldowns[userId to commandId] ?: 0
        }

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong

            userCommandCooldowns[userId to commandId] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong

            return userCommandHistory[userId to commandId] ?: UsageHistoryImpl()
        }

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong

            userCommandHistory[userId to commandId] = usageHistory
        }
    },

    // a specific user can have a usageLimit in a specific channel for a command
    COMMAND_USER_CHANNEL {
        private val userChannelCommandCooldowns: HashMap<Triple<Long, Long, String>, Long> = HashMap()
        private val userChannelCommandUsageHistory: MutableMap<Triple<Long, Long, String>, UsageHistory> =
            HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            return userChannelCommandCooldowns[Triple(userId, channelId, commandId)] ?: 0
        }

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            userChannelCommandCooldowns[Triple(userId, channelId, commandId)] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            return userChannelCommandUsageHistory[Triple(userId, channelId, commandId)] ?: UsageHistoryImpl()
        }

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val channelId = context.channel.idLong

            userChannelCommandUsageHistory[Triple(userId, channelId, commandId)] = usageHistory
        }
    },

    // user usageLimit in specific guild for a command
    COMMAND_USER_GUILD {
        private val userGuildCommandCooldowns: HashMap<Triple<Long, Long, String>, Long> = HashMap()
        private val userGuildCommandUsageHistory: MutableMap<Triple<Long, Long, String>, UsageHistory> =
            HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val guildId = context.guildId ?: return 0

            return userGuildCommandCooldowns[Triple(userId, guildId, commandId)] ?: 0
        }

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val guildId = context.guildId ?: return

            userGuildCommandCooldowns[Triple(userId, guildId, commandId)] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val guildId = context.guildId ?: return UsageHistoryImpl()

            return userGuildCommandUsageHistory[Triple(userId, guildId, commandId)] ?: UsageHistoryImpl()
        }

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            val commandId = context.event.command.hashCode().toString()
            val userId = context.user.idLong
            val guildId = context.guildId ?: return

            userGuildCommandUsageHistory[Triple(userId, guildId, commandId)] = usageHistory
        }
    },

    // a specific user can have a usageLimit across all commands
    GLOBAL_USER {
        private val userGlobalCooldowns: HashMap<Long, Long> = HashMap()
        private val userGlobalHistory: MutableMap<Long, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long =
            userGlobalCooldowns[context.user.idLong] ?: 0

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            userGlobalCooldowns[context.user.idLong] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory =
            userGlobalHistory[context.user.idLong] ?: UsageHistoryImpl()

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            userGlobalHistory[context.user.idLong] = usageHistory
        }
    },

    // a specific user can have a usageLimit in a specific channel across all commands
    GLOBAL_USER_CHANNEL {
        private val userChannelCooldowns: HashMap<Pair<Long, Long>, Long> = HashMap()
        private val userChannelUsageHistory: MutableMap<Pair<Long, Long>, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long =
            userChannelCooldowns[context.user.idLong to context.channel.idLong] ?: 0

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            userChannelCooldowns[context.user.idLong to context.channel.idLong] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory =
            userChannelUsageHistory[context.user.idLong to context.channel.idLong] ?: UsageHistoryImpl()

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            userChannelUsageHistory[context.user.idLong to context.channel.idLong] = usageHistory
        }
    },

    // user usageLimit in specific guild across all commands
    GLOBAL_USER_GUILD {
        private val userGuildCooldowns: HashMap<Pair<Long, Long>, Long> = HashMap()
        private val userGuildUsageHistory: MutableMap<Pair<Long, Long>, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long {
            val guildId = context.guildId ?: return 0
            return userGuildCooldowns[context.user.idLong to guildId] ?: 0
        }

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            val guildId = context.guildId ?: return
            userGuildCooldowns[context.user.idLong to guildId] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory {
            val guildId = context.guildId ?: return UsageHistoryImpl()
            return userGuildUsageHistory[context.user.idLong to guildId] ?: UsageHistoryImpl()
        }

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            val guildId = context.guildId ?: return
            userGuildUsageHistory[context.user.idLong to guildId] = usageHistory
        }
    },

    // a usageLimit across all commands :thonk: (don't use this lol)
    GLOBAL {
        private var globalCooldown: Long = 0
        private var globalHistory: UsageHistory = UsageHistoryImpl()

        override fun getCooldown(context: DiscriminatingContext): Long = globalCooldown

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            globalCooldown = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory = globalHistory

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            globalHistory = usageHistory
        }
    },

    // a usageLimit for a specific channel across all commands
    GLOBAL_CHANNEL {
        private val channelCooldowns: HashMap<Long, Long> = HashMap()
        private val channelUsageHistory: MutableMap<Long, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long = channelCooldowns[context.channel.idLong] ?: 0

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            channelCooldowns[context.channel.idLong] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory =
            channelUsageHistory[context.channel.idLong] ?: UsageHistoryImpl()

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            channelUsageHistory[context.channel.idLong] = usageHistory
        }
    },

    // a usageLimit for a guild across all commands
    GLOBAL_GUILD {
        private val guildCooldowns: HashMap<Long, Long> = HashMap()
        private val guildUsageHistory: MutableMap<Long, UsageHistory> = HashMap()

        override fun getCooldown(context: DiscriminatingContext): Long {
            val guildId = context.guildId ?: return 0
            return guildCooldowns[guildId] ?: 0
        }

        override fun setCooldown(context: DiscriminatingContext, until: Long) {
            val guildId = context.guildId ?: return
            guildCooldowns[guildId] = until
        }

        override fun getUsageHistory(context: DiscriminatingContext): UsageHistory {
            val guildId = context.guildId ?: return UsageHistoryImpl()
            return guildUsageHistory[guildId] ?: UsageHistoryImpl()
        }

        override fun setUsageHistory(context: DiscriminatingContext, usageHistory: UsageHistory) {
            val guildId = context.guildId ?: return
            guildUsageHistory[guildId] = usageHistory
        }
    };
}
