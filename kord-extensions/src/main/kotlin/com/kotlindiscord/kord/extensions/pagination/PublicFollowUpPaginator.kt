/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination

import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.*

/**
 * Class representing a button-based paginator that operates by creating and editing a follow-up message for the
 * given public interaction response.
 *
 * @param interaction Interaction response behaviour to work with.
 */
public class PublicFollowUpPaginator(
    pages: Pages,
    owner: Long? = null,
    timeoutSeconds: Long? = null,
    keepEmbed: Boolean = true,
    switchEmoji: Emoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,

    public val interaction: InteractionHook,
) : BaseButtonPaginator(pages, owner, timeoutSeconds, keepEmbed, switchEmoji, bundle, locale) {
    /** Follow-up interaction to use for this paginator's embeds. Will be created by [send]. **/
    public var sent: Boolean = false

    override suspend fun send() {
        if (!sent) {
            setup()

            interaction.sendMessage(
                MessageCreate {
                embed { applyPage() }
                with(this@PublicFollowUpPaginator.components) {
                    this@MessageCreate.applyToMessage()
                }
            }
            ).await()
        } else {
            updateButtons()

            interaction.editOriginal(
                MessageEdit {
                embed { applyPage() }

                with(this@PublicFollowUpPaginator.components) {
                    this@MessageEdit.applyToMessage()
                }
            }
            ).await()
        }
    }

    override suspend fun destroy() {
        if (!active) {
            return
        }

        active = false

        if (!keepEmbed) {
            interaction.deleteOriginal().await()
        } else {
            interaction.editOriginal(
                MessageEdit {
                embed { applyPage() }

                this.builder.setComponents(mutableListOf())
            }
            ).await()
        }

        super.destroy()
    }
}

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
@Suppress("FunctionNaming")  // Factory function
public fun PublicFollowUpPaginator(
    builder: PaginatorBuilder,
    interaction: InteractionHook
): PublicFollowUpPaginator = PublicFollowUpPaginator(
    pages = builder.pages,
    owner = builder.owner,
    timeoutSeconds = builder.timeoutSeconds,
    keepEmbed = builder.keepEmbed,
    bundle = builder.bundle,
    locale = builder.locale,
    interaction = interaction,

    switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)
