/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.pagination.pages

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import dev.minn.jda.ktx.messages.InlineEmbed
import org.koin.core.component.inject
import java.util.*

/**
 * Representation of a single paginator page. You can extend this to customize it if you wish!
 *
 * @param bundle Optional: Translations bundle for group names
 * @param builder Embed builder callable for building the page's embed
 */
public open class Page(
    public open val bundle: String? = null,
    public open val builder: suspend InlineEmbed.() -> Unit,
) : KordExKoinComponent {
    /** Current instance of the bot. **/
    public open val bot: ExtensibleBot by inject()

    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Create an embed builder for this page. **/
    public open suspend fun build(
        locale: Locale,
        pageNum: Int,
        pages: Int,
        group: String?,
        groupIndex: Int,
        groups: Int,
    ): suspend InlineEmbed.() -> Unit = {
        builder()
        val msgEmbed = build()
        val curFooterIcon = msgEmbed.footer?.iconUrl
        val curFooterText = msgEmbed.footer?.text

        footer {
            iconUrl = curFooterIcon

            name = buildString {
                if (pages > 1) {
                    append(
                        translationsProvider.translate(
                            "paginator.footer.page",
                            locale,
                            replacements = arrayOf(pageNum + 1, pages)
                        )
                    )
                }

                if (!group.isNullOrBlank() || groups > 2) {
                    if (isNotBlank()) {
                        append(" • ")
                    }

                    if (group.isNullOrBlank()) {
                        append(
                            translationsProvider.translate(
                                "paginator.footer.group",
                                locale,
                                replacements = arrayOf(groupIndex + 1, groups)
                            )
                        )
                    } else {
                        val groupName = translationsProvider.translate(
                            group, locale, bundle
                        ).capitalizeWords(locale)

                        append("$groupName (${groupIndex + 1}/$groups)")
                    }
                }

                if (!curFooterText.isNullOrEmpty()) {
                    if (isNotBlank()) {
                        append(" • ")
                    }

                    append(curFooterText)
                }
            }
        }
    }
}
