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
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.*

/**
 * Class representing a button-based paginator that operates on standard messages.
 *
 * @param pingInReply Whether to ping the author of [targetMessage] in reply.
 * @param targetMessage Target message to reply to, overriding [targetChannel].
 * @param targetChannel Target channel to send the paginator to, if [targetMessage] isn't provided.
 */
public class MessageButtonPaginator(
    pages: Pages,
    owner: Long? = null,
    timeoutSeconds: Long? = null,
    keepEmbed: Boolean = true,
    switchEmoji: Emoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,

    public val pingInReply: Boolean = true,
    public val targetChannel: MessageChannel? = null,
    public val targetMessage: Message? = null,
) : BaseButtonPaginator(pages, owner, timeoutSeconds, keepEmbed, switchEmoji, bundle, locale) {
    init {
        if (targetChannel == null && targetMessage == null) {
            throw IllegalArgumentException("Must provide either a target channel or target message")
        }
    }

    /** Specific channel to send the paginator to. **/
    public val channel: MessageChannel = targetMessage?.channel ?: targetChannel!!

    /** Message containing the paginator. **/
    public var message: Message? = null

    override suspend fun send() {
        if (message == null) {
            setup()

            message = channel.sendMessage(MessageCreate {
                this.builder.mentionRepliedUser(pingInReply)
                embed { applyPage() }

                with(this@MessageButtonPaginator.components) {
                    this@MessageCreate.applyToMessage()
                }
            }).setMessageReference(targetMessage?.id).await()
        } else {
            updateButtons()

            message!!.editMessage(MessageEdit {
                embed { applyPage() }

                with(this@MessageButtonPaginator.components) {
                    this@MessageEdit.applyToMessage()
                }
            })
        }
    }

    override suspend fun destroy() {
        if (!active) {
            return
        }

        active = false

        if (!keepEmbed) {
            message!!.delete()
        } else {
            message!!.editMessage(MessageEdit {
                embed { applyPage() }

                this.builder.setComponents(mutableListOf())
            }).mentionRepliedUser(pingInReply).await()
        }

        super.destroy()
    }
}

/** Convenience function for creating a message button paginator from a paginator builder. **/
@Suppress("FunctionNaming")  // Factory function
public fun MessageButtonPaginator(
    pingInReply: Boolean = true,
    targetChannel: MessageChannel? = null,
    targetMessage: Message? = null,

    builder: PaginatorBuilder
): MessageButtonPaginator =
    MessageButtonPaginator(
        pages = builder.pages,
        owner = builder.owner,
        timeoutSeconds = builder.timeoutSeconds,
        keepEmbed = builder.keepEmbed,
        bundle = builder.bundle,
        locale = builder.locale,

        pingInReply = pingInReply,
        targetChannel = targetChannel,
        targetMessage = targetMessage,

        switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    )
