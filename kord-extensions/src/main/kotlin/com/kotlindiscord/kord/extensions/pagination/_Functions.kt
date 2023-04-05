/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination

import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

/** Create a paginator that edits the original interaction. **/
public inline fun InteractionHook.editingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit
): PublicResponsePaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return PublicResponsePaginator(pages, this)
}

/** Create a paginator that creates a follow-up message, and edits that. **/
public inline fun InteractionHook.respondingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit
): PublicFollowUpPaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return PublicFollowUpPaginator(pages, this)
}

/**
 * Create a paginator that edits the original interaction. This is the only option for an ephemeral interaction, as
 * it's impossible to edit an ephemeral follow-up.
 */
public inline fun InteractionHook.ephemeralEditingPaginator(
    defaultGroup: String = "",
    locale: Locale? = null,
    builder: (PaginatorBuilder).() -> Unit
): InteractionPaginator {
    val pages = PaginatorBuilder(locale = locale, defaultGroup = defaultGroup)

    builder(pages)

    return InteractionPaginator(pages, this)
}
