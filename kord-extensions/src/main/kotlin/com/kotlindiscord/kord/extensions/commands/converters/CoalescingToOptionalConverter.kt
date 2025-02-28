/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.converters

import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.parser.StringParser
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.build.OptionData

/**
 * A special [OptionalConverter] that wraps a [SingleConverter], effectively turning it into an optional
 * converter with the same logic.
 *
 * The behaviours specified in [OptionalConverter] also apply to this converter, so it's worth reading about it.
 *
 * @param coalescingConverter The [SingleConverter] to wrap.
 *
 * @param newSignatureTypeString An optional signature type string to override the one set in [coalescingConverter].
 * @param newShowTypeInSignature An optional boolean to override the [showTypeInSignature] setting set in
 * [coalescingConverter].
 * @param newErrorTypeString An optional error type string to override the one set in [coalescingConverter].
 */
public class CoalescingToOptionalConverter<T : Any>(
    public val coalescingConverter: CoalescingConverter<T>,

    newSignatureTypeString: String? = null,
    newShowTypeInSignature: Boolean? = null,
    newErrorTypeString: String? = null,
    outputError: Boolean = false,

    override var validator: Validator<T?> = null
) : OptionalCoalescingConverter<T>(outputError) {
    override val signatureTypeString: String = newSignatureTypeString ?: coalescingConverter.signatureTypeString
    override val showTypeInSignature: Boolean = newShowTypeInSignature ?: coalescingConverter.showTypeInSignature
    override val errorTypeString: String? = newErrorTypeString ?: coalescingConverter.errorTypeString

    override suspend fun parse(parser: StringParser?, context: CommandContext, named: List<String>?): Int {
        val result = coalescingConverter.parse(parser, context, named)

        if (result > 0) {
            this.parsed = coalescingConverter.parsed
        }

        return result
    }

    override suspend fun handleError(
        t: Throwable,
        context: CommandContext
    ): String = coalescingConverter.handleError(t, context)

    override suspend fun toSlashOption(arg: Argument<*>): OptionData {
        val option = coalescingConverter.toSlashOption(arg)
        option.isRequired = false

        return option
    }

    override suspend fun parseOption(context: CommandContext, option: OptionMapping): Boolean {
        val result = coalescingConverter.parseOption(context, option)

        if (result) {
            this.parsed = coalescingConverter.parsed
        }

        return result
    }
}
