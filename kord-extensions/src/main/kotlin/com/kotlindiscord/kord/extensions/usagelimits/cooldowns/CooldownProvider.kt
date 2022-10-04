package com.kotlindiscord.kord.extensions.usagelimits.cooldowns

import com.kotlindiscord.kord.extensions.usagelimits.CachedUsageLimitType
import com.kotlindiscord.kord.extensions.usagelimits.DiscriminatingContext
import kotlin.time.Duration

/**
 * CooldownProvider interface provides functions to get the cooldown [Duration] in a situation for a specific
 * [CachedUsageLimitType].
 * **/
public interface CooldownProvider {
    /** Function returns a cooldown [Duration] based on the [context] and [type]. **/
    public suspend fun getCooldown(context: DiscriminatingContext, type: CooldownType): Duration
}
