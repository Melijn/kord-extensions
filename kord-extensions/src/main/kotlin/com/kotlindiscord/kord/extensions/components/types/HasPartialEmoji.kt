/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.components.types

import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.EmojiUnion
import net.dv8tion.jda.internal.entities.emoji.CustomEmojiImpl
import net.dv8tion.jda.internal.entities.emoji.UnicodeEmojiImpl

/**
 * Interface representing a button type that has a partial emoji property. This is used to keep the [emoji]
 * function DRY.
 */
public interface HasPartialEmoji {
    /**
     * A partial emoji object, either a guild or Unicode emoji. Optional if you've got a label.
     *
     * @see emoji
     */
    public var partialEmoji: EmojiUnion?
}

/** Convenience function for setting [HasPartialEmoji.partialEmoji] based on a given Unicode emoji. **/
public fun HasPartialEmoji.emoji(unicodeEmoji: String) {
    partialEmoji = UnicodeEmojiImpl(
        unicodeEmoji
    )
}

/** Convenience function for setting [HasPartialEmoji.partialEmoji] based on a given guild custom emoji. **/
public fun HasPartialEmoji.emoji(guildEmoji: CustomEmoji) {
    partialEmoji = CustomEmojiImpl(
        guildEmoji.name,
        guildEmoji.idLong,
        guildEmoji.isAnimated
    )
}