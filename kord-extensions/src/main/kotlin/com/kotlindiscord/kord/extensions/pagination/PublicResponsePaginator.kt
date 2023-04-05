/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination

import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

/**
 * Class representing a button-based paginator that operates by editing the given public interaction response.
 *
 * @param interaction Interaction response behaviour to work with.
 */
public class PublicResponsePaginator(
    pages: Pages,
    owner: Long? = null,
    timeoutSeconds: Long? = null,
    override val keepEmbed: Boolean = true,
    switchEmoji: Emoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,

    interaction: InteractionHook,
) : InteractionPaginator(pages, owner, timeoutSeconds, switchEmoji, bundle, locale, interaction)

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
@Suppress("FunctionNaming")  // Factory function
public fun PublicResponsePaginator(
    builder: PaginatorBuilder,
    interaction: InteractionHook,
): PublicResponsePaginator = PublicResponsePaginator(
    pages = builder.pages,
    owner = builder.owner,
    timeoutSeconds = builder.timeoutSeconds,
    keepEmbed = builder.keepEmbed,
    bundle = builder.bundle,
    locale = builder.locale,
    interaction = interaction,

    switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)
