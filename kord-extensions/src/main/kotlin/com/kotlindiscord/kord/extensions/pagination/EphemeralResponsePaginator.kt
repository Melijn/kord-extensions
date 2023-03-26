/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination

import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

/**
 * Class representing a button-based paginator that operates by editing the given ephemeral interaction response.
 *
 * @param interaction Interaction response behaviour to work with.
 */
public class EphemeralResponsePaginator(
    pages: Pages,
    owner: Long? = null,
    timeoutSeconds: Long? = null,
    switchEmoji: Emoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,

    public val interaction: InteractionHook,
) : BaseButtonPaginator(pages, owner, timeoutSeconds, true, switchEmoji, bundle, locale) {
    /** Whether this paginator has been set up for the first time. **/
    public var isSetup: Boolean = false

    override suspend fun send() {
        if (!isSetup) {
            isSetup = true

            setup()
        } else {
            updateButtons()
        }

        interaction.editOriginal(MessageEdit {
            embed { applyPage() }

            with(this@EphemeralResponsePaginator.components) {
                this@MessageEdit.applyToMessage()
            }
        })
    }

    override suspend fun destroy() {
        if (!active) {
            return
        }

        active = false

        interaction.editOriginal(MessageEdit {
            embed { applyPage() }

            this.builder.setComponents(mutableListOf())
        })

        super.destroy()
    }
}

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
@Suppress("FunctionNaming")  // Factory function
public fun EphemeralResponsePaginator(
    builder: PaginatorBuilder,
    interaction: InteractionHook
): EphemeralResponsePaginator = EphemeralResponsePaginator(
    pages = builder.pages,
    owner = builder.owner,
    timeoutSeconds = builder.timeoutSeconds,
    bundle = builder.bundle,
    locale = builder.locale,
    interaction = interaction,

    switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)
