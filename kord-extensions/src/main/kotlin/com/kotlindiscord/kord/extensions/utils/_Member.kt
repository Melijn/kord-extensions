/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

/** A more sensible name than `communicationDisabledUntil`. **/
public val Member.timeoutUntil: Instant?
    inline get() = this.timeOutEnd?.toInstant()?.toKotlinInstant()

/**
 * Check if the user has the given [Role].
 *
 * @param role Role to check for
 * @return true if the user has the given role, false otherwise
 */
public fun Member.hasRole(role: Role): Boolean = roles.contains(role)

/**
 * Check if the user has all of the given roles.
 *
 * @param roles Roles to check for.
 * @return `true` if the user has all of the given roles, `false` otherwise.
 */
public fun Member.hasRoles(vararg roles: Role): Boolean = hasRoles(roles.toList())

/**
 * Check if the user has all of the given roles.
 *
 * @param roles Roles to check for.
 * @return `true` if the user has all of the given roles, `false` otherwise.
 */
public fun Member.hasRoles(roles: Collection<Role>): Boolean =
    if (roles.isEmpty()) {
        true
    } else {
        this.roles.containsAll(roles)
    }

/**
 * Convenience function to retrieve a user's top [Role].
 *
 * @receiver The [Member] to get the top role for
 * @return The user's top role, or `null` if they have no roles
 */
public fun Member.getTopRole(): Role? = roles.toList().maxOrNull()
