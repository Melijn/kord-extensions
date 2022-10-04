package com.kotlindiscord.kord.extensions.usagelimits.cooldowns

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.usagelimits.CachedUsageLimitType
import com.kotlindiscord.kord.extensions.usagelimits.DiscriminatingContext
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.RateLimitType
import com.kotlindiscord.kord.extensions.usagelimits.ratelimits.UsageHistory
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Default [CooldownHandler] implementation, serves as a usable example, it is however very opinionated, so you might
 * want to create your own implementation. **/
public open class DefaultCooldownHandler : CooldownHandler {

    /** Holds the message back-off duration, if the user triggered a cooldown within [backOffTime] ago and now,
     * no message will be sent as the user is considered spamming and wasting our discord api uses. **/
    public open var backOffTime: Duration = 10.seconds

    // cooldown settings provider, collects configured settings for cooldowns :)
    private val cooldownProvider = DefaultCooldownProvider()

    /**
     * Checks if the command should not be run due to a cooldown.
     * If it is on cooldown it saves the cooldown hit and responds with an info message if there was no cooldown hit
     * in the last [backOffTime].
     *
     * Mutates the associated [UsageHistory] of various [UsageLimitTypes][CachedUsageLimitType]
     *
     * @return true if the command is on cooldown, false if not on cooldown.
     */
    override suspend fun checkCommandOnCooldown(context: DiscriminatingContext): Boolean {
        val hitCooldowns = ArrayList<Triple<CooldownType, UsageHistory, Long>>()
        val currentTime = System.currentTimeMillis()
        var shouldSendMessage = true

        for (type in CachedUsageLimitType.values()) {
            val until = type.getCooldown(context)
            val usageHistory = type.getUsageHistory(context)
            if (until > currentTime) {
                if (!shouldSendMessage(until, usageHistory, type)) shouldSendMessage = false

                hitCooldowns.add(Triple(type, usageHistory, until))
            }
        }

        if (shouldSendMessage) {
            val (maxType, maxUsageHistory, maxUntil) = hitCooldowns.maxBy {
                it.third
            }
            sendCooldownMessage(context, maxType, maxUsageHistory, maxUntil)
        }

        return hitCooldowns.isNotEmpty()
    }

    /**
     * @return true if an "on cooldown" message should be sent, false otherwise.
     */
    override suspend fun shouldSendMessage(
        cooldownUntil: Long,
        usageHistory: UsageHistory,
        type: RateLimitType,
    ): Boolean = usageHistory.crossedCooldowns.last() < System.currentTimeMillis() - backOffTime.inWholeMilliseconds

    /**
     * Sends a message in the discord channel where the command was used with information about what cooldown
     * was hit and when the user can use the/a command again.
     *
     * The message wil be ephemeral for application commands.
     *
     * @param context the [DiscriminatingContext] that caused this ratelimit hit
     * @param usageHistory the involved [UsageHistory]
     * @param cooldownUntil the involved [epochMillis][Long] timestamp which indicated when the cooldown will ended
     */
    override suspend fun sendCooldownMessage(
        context: DiscriminatingContext,
        type: CooldownType,
        usageHistory: UsageHistory,
        cooldownUntil: Long,
    ) {
        val discordTimeStamp = Instant.fromEpochMilliseconds(cooldownUntil)
            .toMessageFormat(DiscordTimestampStyle.RelativeTime)
        val message = "You are on cooldown until $discordTimeStamp for $type"

        when (val discordEvent = context.event.event) {
            is MessageCreateEvent -> discordEvent.message.channel.createMessage(message)
            is ApplicationCommandInteractionCreateEvent -> discordEvent.interaction.respondEphemeral {
                content = message
            }
        }
    }

    override suspend fun onExecCooldownUpdate(
        commandContext: CommandContext,
        context: DiscriminatingContext,
        success: Boolean
    ) {
        if (!success) return
        for ((t, u) in commandContext.command.cooldownMap) {
            val commandDuration = u(context)
            val providedCooldown = cooldownProvider.getCooldown(context, t)
            val progressiveCommandDuration = commandContext.cooldownCounters[t] ?: Duration.ZERO

            val cooldowns = arrayOf(commandDuration, providedCooldown, progressiveCommandDuration)
            val longestDuration = cooldowns.max()

            t.setCooldown(context, longestDuration.inWholeMilliseconds)
        }
    }
}
