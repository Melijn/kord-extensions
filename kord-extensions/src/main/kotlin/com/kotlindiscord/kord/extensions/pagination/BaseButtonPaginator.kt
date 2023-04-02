/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination

import com.kotlindiscord.kord.extensions.checks.types.CheckWithCache
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.minn.jda.ktx.messages.InlineMessage
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.util.*

/**
 * Abstract class containing some common functionality needed by interactive button-based paginators.
 */
public abstract class BaseButtonPaginator(
    pages: Pages,
    owner: Long? = null,
    timeoutSeconds: Long? = null,
    keepEmbed: Boolean = true,
    switchEmoji: Emoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
    bundle: String? = null,
    locale: Locale? = null,
) : BasePaginator(pages, owner, timeoutSeconds, keepEmbed, switchEmoji, bundle, locale) {

    /** Buttons of this paginator. **/
    public var components: MutableList<Button> = mutableListOf()

    /** Scheduler used to schedule the paginator's timeout. **/
    public var scheduler: Scheduler = Scheduler()

    /** Scheduler used to schedule the paginator's timeout. **/
    public var task: Task? = if (timeoutSeconds != null) {
        scheduler.schedule(timeoutSeconds) { destroy() }
    } else {
        null
    }

    /** Button builder representing the button that switches to the first page. **/
    public open var firstPageButton: Button? = null

    /** Button builder representing the button that switches to the previous page. **/
    public open var backButton: Button? = null

    /** Button builder representing the button that switches to the next page. **/
    public open var nextButton: Button? = null

    /** Button builder representing the button that switches to the last page. **/
    public open var lastPageButton: Button? = null

    /** Button builder representing the button that switches between groups. **/
    public open var switchButton: Button? = null

    /** Group-specific buttons, if any. **/
    public open val groupButtons: MutableMap<String, Button> = mutableMapOf()

    /** Whether it's possible for us to have a row of group-switching buttons. **/
    @Suppress("MagicNumber")
    public val canUseSwitchingButtons: Boolean by lazy { allGroups.size in 3..5 && "" !in allGroups }

    /** A button-oriented check function that matches based on the [owner] property. **/
    public val defaultCheck: CheckWithCache<GenericComponentInteractionCreateEvent> = {
        if (!active) {
            fail()
        } else if (owner == null) {
            pass()
        } else if (event.interaction.user.idLong == owner) {
            pass()
        } else {
            fail()
        }
    }

    override suspend fun destroy() {
        runTimeoutCallbacks()
        task?.cancel()
    }

    override suspend fun setup() {
        if (pages.groups.values.any { it.size > 1 }) {
            // Add navigation buttons...
            firstPageButton = Button.secondary("first-page", FIRST_PAGE_EMOJI).apply {
                withDisabled(pages.groups[currentGroup]!!.size <= 1)

//                check(defaultCheck)

//                action {
//                    goToPage(0)
//
//                    send()
//                    task?.restart()
//                }
            }

            backButton = Button.secondary("back", FIRST_PAGE_EMOJI).apply {
                withDisabled(pages.groups[currentGroup]!!.size <= 1)

//                check(defaultCheck)
//
//                action {
//                    previousPage()
//
//                    send()
//                    task?.restart()
//                }
            }

            nextButton = Button.secondary("next", FIRST_PAGE_EMOJI).apply {
                withDisabled(pages.groups[currentGroup]!!.size <= 1)

//                check(defaultCheck)
//
//                action {
//                    nextPage()
//
//                    send()
//                    task?.restart()
//                }
            }

            lastPageButton = Button.secondary("last-page", FIRST_PAGE_EMOJI).apply {
                withDisabled(pages.groups[currentGroup]!!.size <= 1)

//                check(defaultCheck)
//
//                action {
//                    goToPage(pages.groups[currentGroup]!!.size - 1)
//
//                    send()
//                    task?.restart()
//                }
            }
        }

        if (pages.groups.values.any { it.size > 1 } || !keepEmbed) {
            // Add the destroy button
            if (keepEmbed) {
                Button.of(ButtonStyle.PRIMARY, "finish", translate("paginator.button.done"), FINISH_EMOJI)
            } else {
                Button.of(ButtonStyle.DANGER, "delete", translate("paginator.button.delete"), DELETE_EMOJI)
            }

//                check(defaultCheck)
//
//                action {
//                    destroy()
//                }
        }

        if (pages.groups.size > 1) {
            if (canUseSwitchingButtons) {
                // Add group-switching buttons

                allGroups.forEach { group ->
                    groupButtons[group] =
                        Button.of(ButtonStyle.SECONDARY, "group-switch", translate(group).capitalizeWords(localeObj))

//                        check(defaultCheck)
//
//                        action {
//                            switchGroup(group)
//                            task?.restart()
//                        }
                }
            } else {
                // Add the singular switch button
                val label = if (allGroups.size == 2) {
                    translate("paginator.button.more")
                } else {
                    translate("paginator.button.group.switch")
                }
                switchButton = Button.of(ButtonStyle.SECONDARY, "switch", label, switchEmoji)

//                    check(defaultCheck)
//
//                    action {
//                        nextGroup()
//
//                        send()
//                        task?.restart()
//                    }
            }
        }

        updateButtons()
    }

    /**
     * Convenience function to switch to a specific group.
     */
    public suspend fun switchGroup(group: String) {
        if (group == currentGroup) {
            return
        }

        // To avoid out-of-bounds
        currentPageNum = minOf(currentPageNum, pages.groups[group]!!.size)
        currentPage = pages.get(group, currentPageNum)
        currentGroup = group

        send()
    }

    override suspend fun nextGroup() {
        val current = currentGroup
        val nextIndex = allGroups.indexOf(current) + 1

        if (nextIndex >= allGroups.size) {
            switchGroup(allGroups.first())
        } else {
            switchGroup(allGroups[nextIndex])
        }
    }

    override suspend fun goToPage(page: Int) {
        if (page == currentPageNum) {
            return
        }

        if (page < 0 || page > pages.groups[currentGroup]!!.size - 1) {
            return
        }

        currentPageNum = page
        currentPage = pages.get(currentGroup, currentPageNum)

        send()
    }

    /**
     * Convenience function that enables and disables buttons as necessary, depending on the current page number.
     */
    public fun updateButtons() {
        val reachedFront = currentPageNum <= 0
        firstPageButton = firstPageButton?.withDisabled(reachedFront)
        backButton = backButton?.withDisabled(reachedFront)

        val reachedLast = currentPageNum >= pages.groups[currentGroup]!!.size - 1
        lastPageButton = lastPageButton?.withDisabled(reachedLast)
        nextButton = nextButton?.withDisabled(reachedLast)

        if (allGroups.size == 2) {
            switchButton = if (currentGroup == pages.defaultGroup) {
                switchButton?.withLabel(translate("paginator.button.more"))
            } else {
                switchButton?.withLabel(translate("paginator.button.less"))
            }
        }

        if (canUseSwitchingButtons) {
            groupButtons.map { (key, value) ->
                key to value.withDisabled(key == currentGroup)
            }
        }
    }

    /** Apply the components in this container to a message that's being created. **/
    public fun InlineMessage<*>.applyToMessage() {
        for (row in this@BaseButtonPaginator.components.chunked(Message.MAX_COMPONENT_COUNT)) {
            actionRow(row)
        }
    }
}
