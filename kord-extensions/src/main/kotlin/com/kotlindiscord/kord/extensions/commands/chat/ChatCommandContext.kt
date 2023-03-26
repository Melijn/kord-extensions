/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.chat

import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.extensions.base.HelpProvider
import com.kotlindiscord.kord.extensions.pagination.MessageButtonPaginator
import com.kotlindiscord.kord.extensions.pagination.builders.PaginatorBuilder
import com.kotlindiscord.kord.extensions.parser.StringParser
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import com.kotlindiscord.kord.extensions.utils.respond
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * Command context object representing the context given to chat commands.
 *
 * @property chatCommand Chat command object
 * @param parser String parser instance, if any - will be `null` if this isn't a chat command.
 * @property argString String containing the command's unparsed arguments, raw, fresh from Discord itself.
 */
@ExtensionDSL
public open class ChatCommandContext<T : Arguments>(
    public val chatCommand: ChatCommand<out T>,
    eventObj: MessageReceivedEvent,
    commandName: String,
    public open val parser: StringParser,
    public val argString: String,
    cache: MutableStringKeyedMap<Any>
) : CommandContext(chatCommand, eventObj, commandName, cache) {
    /** Event that triggered this command execution. **/
    public val event: MessageReceivedEvent get() = eventObj as MessageReceivedEvent

    /** Message channel this command happened in, if any. **/
    public override val channel: MessageChannel = event.message.channel

    /** Guild this command happened in, if any. **/
    public override val guild: Guild? = event.guild

    /** Guild member responsible for executing this command, if any. **/
    public override val member: Member? = event.member

    /** User responsible for executing this command, if any (if `null`, it's a webhook). **/
    public override val user: User = event.message.author

    /** Message object containing this command invocation. **/
    public open val message: Message = event.message

    /** Arguments object containing this command's parsed arguments. **/
    public open lateinit var arguments: T

    /** @suppress Internal function **/
    public fun populateArgs(args: T) {
        arguments = args
    }

    /**
     * Convenience function to create a button paginator using a builder DSL syntax. Handles the contextual stuff for
     * you.
     */
    public suspend fun paginator(
        defaultGroup: String = "",

        pingInReply: Boolean = true,
        targetChannel: MessageChannel? = null,
        targetMessage: Message? = null,

        body: suspend PaginatorBuilder.() -> Unit
    ): MessageButtonPaginator {
        val builder = PaginatorBuilder(resolvedLocale.await(), defaultGroup = defaultGroup)

        body(builder)

        return MessageButtonPaginator(pingInReply, targetChannel, targetMessage, builder)
    }

    /**
     * Generate and send the help embed for this command, using the first loaded extensions that implements
     * [HelpProvider].
     *
     * @return `true` if a help extension exists and help was sent, `false` otherwise.
     */
    public suspend fun sendHelp(): Boolean {
        val helpExtension = this.command.extension.bot.findExtension<HelpProvider>() ?: return false
        val paginator = helpExtension.getCommandHelpPaginator(this, chatCommand)

        paginator.send()

        return true
    }

    /**
     * Convenience function allowing for message responses with translated content.
     */
    public suspend fun Message.respondTranslated(
        key: String,
        replacements: Array<Any?> = arrayOf(),
        useReply: Boolean = true
    ): Message = respond(translate(key, command.resolvedBundle, replacements), useReply)
}
