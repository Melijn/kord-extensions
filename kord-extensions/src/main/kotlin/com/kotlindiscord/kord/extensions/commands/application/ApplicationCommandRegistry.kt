/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress(
    "UNCHECKED_CAST",
    "TooGenericExceptionCaught",
    "StringLiteralDuplication",
)

package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandParser
import com.kotlindiscord.kord.extensions.commands.application.user.UserCommand
import com.kotlindiscord.kord.extensions.commands.converters.SlashCommandConverter
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.group
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.core.component.inject
import java.util.*

/**
 * Abstract class representing common behavior for application command registries.
 *
 * Deals with the registration and syncing of, and dispatching to, all application commands.
 * Subtypes should build their functionality on top of this type.
 *
 * @see DefaultApplicationCommandRegistry
 */
public abstract class ApplicationCommandRegistry : KordExKoinComponent {

    protected val logger: KLogger = KotlinLogging.logger { }

    /** Current instance of the bot. **/
    public open val bot: ExtensibleBot by inject()

    /** Kord instance, backing the ExtensibleBot. **/
    public open val kord: ShardManager by inject()

    /** Translations provider, for retrieving translations. **/
    public open val translationsProvider: TranslationsProvider by inject()

    /** Command parser to use for slash commands. **/
    public val argumentParser: SlashCommandParser = SlashCommandParser()

    /** Whether the initial sync has been finished, and commands should be registered directly. **/
    public var initialised: Boolean = false
    public var initializing: Boolean = false

    /** Handles the initial registration of commands, after extensions have been loaded. **/
    public suspend fun initialRegistration() {
        if (initialised || initializing) {
            return
        } else {
            initializing = true
        }

        val commands: MutableList<ApplicationCommand<*>> = mutableListOf()

        bot.extensions.values.forEach {
            commands += it.messageCommands
            commands += it.slashCommands
            commands += it.userCommands
        }

        try {
            initialize(commands)
        } catch (t: Throwable) {
            logger.error(t) { "Failed to initialize registry" }
        }

        initialised = true
        initializing = false
    }

    /** Called once the initial registration started and all extensions are loaded. **/
    protected abstract suspend fun initialize(commands: List<ApplicationCommand<*>>)

    /** Register a [SlashCommand] to the registry.
     *
     * This method is only called after the [initialize] method and allows runtime modifications.
     */
    public abstract suspend fun register(command: SlashCommand<*, *>): SlashCommand<*, *>?

    /**
     * Register a [MessageCommand] to the registry.
     *
     * This method is only called after the [initialize] method and allows runtime modifications.
     */
    public abstract suspend fun register(command: MessageCommand<*>): MessageCommand<*>?

    /** Register a [UserCommand] to the registry.
     *
     * This method is only called after the [initialize] method and allows runtime modifications.
     */
    public abstract suspend fun register(command: UserCommand<*>): UserCommand<*>?

    /** Event handler for slash commands. **/
    public abstract suspend fun handle(event: SlashCommandInteractionEvent)

    /** Event handler for message commands. **/
    public abstract suspend fun handle(event: MessageContextInteractionEvent)

    /** Event handler for user commands. **/
    public abstract suspend fun handle(event: UserContextInteractionEvent)

    /** Event handler for command autocomplete interactions. **/
    public abstract suspend fun handle(event: CommandAutoCompleteInteractionEvent)

    /** Unregister a slash command. **/
    public abstract suspend fun unregister(command: SlashCommand<*, *>, delete: Boolean = true): SlashCommand<*, *>?

    /** Unregister a message command. **/
    public abstract suspend fun unregister(command: MessageCommand<*>, delete: Boolean = true): MessageCommand<*>?

    /** Unregister a user command. **/
    public abstract suspend fun unregister(command: UserCommand<*>, delete: Boolean = true): UserCommand<*>?

    // region: Utilities

    /** Unregister a generic [ApplicationCommand]. **/
    public open suspend fun unregisterGeneric(
        command: ApplicationCommand<*>,
        delete: Boolean = true,
    ): ApplicationCommand<*>? =
        when (command) {
            is MessageCommand<*> -> unregister(command, delete)
            is SlashCommand<*, *> -> unregister(command, delete)
            is UserCommand<*> -> unregister(command, delete)

            else -> error("Unsupported application command type: ${command.type.name}")
        }

    /** @suppress Internal function used to delete the given command from Discord. Used by [unregister]. **/
    public open suspend fun deleteGeneric(
        command: ApplicationCommand<*>,
        discordCommandId: Long,
    ) {
        try {
            val guildId = command.guildId
            if (guildId != null) {
                kord.getGuildById(guildId)?.deleteCommandById(discordCommandId)?.await()
            } else {
                kord.shards.firstOrNull()?.deleteCommandById(discordCommandId)?.await()
            }
        } catch (e: ErrorResponseException) {
            logger.warn(e) {
                "Failed to delete ${command.type.name} command ${command.name}" +
                    "\n        Discord error message: ${e.errorResponse}"
            }
        }
    }

    /** Register multiple slash commands. **/
    public open suspend fun <T : ApplicationCommand<*>> registerAll(vararg commands: T): List<T> =
        commands.sortedByDescending { it.name }.mapNotNull {
            try {
                when (it) {
                    is SlashCommand<*, *> -> register(it) as T
                    is MessageCommand<*> -> register(it) as T
                    is UserCommand<*> -> register(it) as T

                    else -> throw IllegalArgumentException(
                        "The registry does not know about this type of ApplicationCommand"
                    )
                }
            } catch (e: ErrorResponseException) {
                logger.warn(e) {
                    "Failed to register ${it.type.name} command: ${it.name}" +
                        "\n        Discord error message: ${e.errorResponse}"
                }

                null
            } catch (t: Throwable) {
                logger.warn(t) { "Failed to register ${it.type.name} command: ${it.name}" }

                null
            }
        }

    /**
     * Creates a KordEx [ApplicationCommand] as discord command and returns the created command's id as [Long].
     */
    public open suspend fun createDiscordCommand(command: ApplicationCommand<*>): Long? = when (command) {
        is SlashCommand<*, *> -> createDiscordSlashCommand(command)
        is UserCommand<*> -> createDiscordUserCommand(command)
        is MessageCommand<*> -> createDiscordMessageCommand(command)

        else -> throw IllegalArgumentException("Unknown ApplicationCommand type")
    }

    /**
     * Creates a KordEx [SlashCommand] as discord command and returns the created command's id as [Long].
     */
    public open suspend fun createDiscordSlashCommand(command: SlashCommand<*, *>): Long? {
        val locale = bot.settings.i18nBuilder.defaultLocale

        val guild = command.guildId?.let { kord.getGuildById(it) }
        val gwSession = kord.shards.first()

        val (name, nameLocalizations) = command.localizedName
        val (description, descriptionLocalizations) = command.localizedDescription

        val response = if (guild == null) {
            // We're registering global commands here, if the guild is null

            gwSession.upsertCommand(name, description) {
                logger.trace { "Adding/updating global ${command.type.name} command: $name" }
                this.setNameLocalizations(nameLocalizations)
                this.setDescriptionLocalizations(descriptionLocalizations)

                this.register(locale, command)
            }.await()
        } else {
            // We're registering guild-specific commands here, if the guild is available

            guild.upsertCommand(name, description) {
                logger.trace { "Adding/updating guild-specific ${command.type.name} command: $name" }

                this.setNameLocalizations(nameLocalizations)
                this.setDescriptionLocalizations(descriptionLocalizations)

                this.register(locale, command)
            }.await()
        }

        return response.idLong
    }

    /**
     * Creates a KordEx [UserCommand] as discord command and returns the created command's id as [Snowflake].
     */
    public open suspend fun createDiscordUserCommand(command: UserCommand<*>): Long? {
        val locale = bot.settings.i18nBuilder.defaultLocale

        val guildId = command.guildId
        val guild = if (guildId != null) {
            kord.getGuildById(guildId)
        } else {
            null
        }
        val gwSession = kord.shards.first()

        val (name, nameLocalizations) = command.localizedName

        val response = if (guild == null) {
            // We're registering global commands here, if the guild is null

            gwSession.upsertCommand(
                Commands.user(name).apply {
                    logger.trace { "Adding/updating global ${command.type.name} command: $name" }
                    this.setNameLocalizations(nameLocalizations)

                    this.register(locale, command)
                }
            ).await()
        } else {
            // We're registering guild-specific commands here, if the guild is available

            guild.upsertCommand(
                Commands.user(name).apply {
                    logger.trace { "Adding/updating guild-specific ${command.type.name} command: $name" }
                    this.setNameLocalizations(nameLocalizations)

                    this.register(locale, command)
                }
            ).await()
        }

        return response.idLong
    }

    /**
     * Creates a KordEx [MessageCommand] as discord command and returns the created command's id as [Long].
     */
    public open suspend fun createDiscordMessageCommand(command: MessageCommand<*>): Long? {
        val locale = bot.settings.i18nBuilder.defaultLocale

        val guild = if (command.guildId != null) {
            kord.getGuildById(command.guildId!!)
        } else {
            null
        }
        val gwSession = kord.shards.first()
        val (name, nameLocalizations) = command.localizedName

        val response = if (guild == null) {
            // We're registering global commands here, if the guild is null

            gwSession.upsertCommand(
                Commands.message(name).apply {
                    logger.trace { "Adding/updating global ${command.type.name} command: $name" }
                    this.setNameLocalizations(nameLocalizations)

                    this.register(locale, command)
                }
            ).await()
        } else {
            // We're registering guild-specific commands here, if the guild is available

            guild.upsertCommand(
                Commands.message(name).apply {
                    logger.trace { "Adding/updating guild-specific ${command.type.name} command: $name" }
                    this.setNameLocalizations(nameLocalizations)

                    this.register(locale, command)
                }
            ).await()
        }

        return response.idLong
    }

    // endregion

    // region: Extensions
    /** Registration logic for slash commands, extracted for clarity. **/
    public open suspend fun SlashCommandData.register(locale: Locale, command: SlashCommand<*, *>) {
        registerGlobalPermissions(locale, command)

        if (command.hasBody) {
            val args = command.arguments?.invoke()

            args?.args?.forEach { arg ->
                val converter = arg.converter

                if (converter !is SlashCommandConverter) {
                    error("Argument ${arg.displayName} does not support slash commands.")
                }

                val option = converter.toSlashOption(arg)
                    .translatedWithAutocomplete(command, arg)

                addOptions(option)
            }
        } else {
            command.subCommands.sortedByDescending { it.name }.forEach {
                val args = it.arguments?.invoke()?.args?.map { arg ->
                    val converter = arg.converter

                    if (converter !is SlashCommandConverter) {
                        error("Argument ${arg.displayName} does not support slash commands.")
                    }

                    val option = converter.toSlashOption(arg)
                        .translatedWithAutocomplete(command, arg)

                    option
                }

                val (name, nameLocalizations) = it.localizedName
                val (description, descriptionLocalizations) = it.localizedDescription

                this.subcommand(
                    name,
                    description
                ) {
                    this.setNameLocalizations(nameLocalizations)
                    this.setDescriptionLocalizations(descriptionLocalizations)

                    if (args != null) {
                        this.addOptions(args)
                    }
                }
            }

            command.groups.values.sortedByDescending { it.name }.forEach { group ->
                val (name, nameLocalizations) = group.localizedName
                val (description, descriptionLocalizations) = group.localizedDescription

                this.group(name, description) {
                    this.setNameLocalizations(nameLocalizations)
                    this.setDescriptionLocalizations(descriptionLocalizations)

                    group.subCommands.sortedByDescending { it.name }.forEach {
                        val args = it.arguments?.invoke()?.args?.map { arg ->
                            val converter = arg.converter

                            if (converter !is SlashCommandConverter) {
                                error("Argument ${arg.displayName} does not support slash commands.")
                            }

                            val option = converter.toSlashOption(arg)
                                .translatedWithAutocomplete(command, arg)

                            option
                        }

                        val (name, nameLocalizations) = it.localizedName
                        val (description, descriptionLocalizations) = it.localizedDescription

                        this.subcommand(
                            name,
                            description
                        ) {
                            this.setNameLocalizations(nameLocalizations)
                            this.setDescriptionLocalizations(descriptionLocalizations)

                            if (args != null) {
                                this.addOptions(args)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Registration logic for message commands, extracted for clarity. **/
    @Suppress("UnusedPrivateMember")  // Only for now...
    public open fun CommandData.register(locale: Locale, command: MessageCommand<*>) {
        registerGuildPermissions(locale, command)
        registerGlobalPermissions(locale, command)
    }

    /** Registration logic for user commands, extracted for clarity. **/
    @Suppress("UnusedPrivateMember")  // Only for now...
    public open fun CommandData.register(locale: Locale, command: UserCommand<*>) {
        registerGuildPermissions(locale, command)
        registerGlobalPermissions(locale, command)
    }

    /**
     * Registers the global permissions of [command].
     */
    public open fun CommandData.registerGlobalPermissions(
        locale: Locale,
        command: ApplicationCommand<*>,
    ) {
        registerGuildPermissions(locale, command)
        this.isGuildOnly = !command.allowInDms
    }

    /**
     * Registers the guild permission of [command].
     */
    public open fun CommandData.registerGuildPermissions(
        locale: Locale,
        command: ApplicationCommand<*>,
    ) {
        command.defaultMemberPermissions?.let { this.setDefaultPermissions(DefaultMemberPermissions.enabledFor(it)) }
    }

    /** Check whether the type and name of an extension-registered application command matches a Discord one. **/
    public open fun ApplicationCommand<*>.matches(
        locale: Locale,
        other: Command,
    ): Boolean = type == other.type && localizedName.default.equals(other.name, true)

    // endregion

    private fun OptionData.translatedWithAutocomplete(command: ApplicationCommand<*>, arg: Argument<*>): OptionData {
        val (localizedName, nameLocalizations) = command.localize(name, true)
        val (localizedDescription, descriptionLocalizations) = command.localize(description)

        val shouldAutoComplete = arg.converter.genericBuilder.autoCompleteCallback != null
        val newData =
            OptionData(this.type, localizedName, localizedDescription, this.isRequired, shouldAutoComplete).apply {
                setNameLocalizations(nameLocalizations)
                setDescriptionLocalizations(descriptionLocalizations)

                if (!shouldAutoComplete) {
                    val mappedChoices = this@translatedWithAutocomplete.choices.map {
                        val (_, choiceLocalizations) = command.localize(it.name)
                        it.setNameLocalizations(choiceLocalizations)
                    }
                    addChoices(mappedChoices)
                }
            }
        return newData
    }
}
