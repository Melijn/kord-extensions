/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress(
    "TooGenericExceptionCaught",
    "StringLiteralDuplication",
    "AnnotationSpacing",
    "SpacingBetweenAnnotations"
)

package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.commands.application.message.MessageCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.user.UserCommand
import com.kotlindiscord.kord.extensions.commands.getDefaultTranslatedDisplayName
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.updateCommands
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

/** Registry for all Discord application commands. **/
public open class DefaultApplicationCommandRegistry : ApplicationCommandRegistry() {

    /** Mapping of Discord-side command ID to a message command object. **/
    public open val messageCommands: MutableMap<Long, MessageCommand<*>> = mutableMapOf()

    /** Mapping of Discord-side command ID to a slash command object. **/
    public open val slashCommands: MutableMap<Long, SlashCommand<*, *>> = mutableMapOf()

    /** Mapping of Discord-side command ID to a user command object. **/
    public open val userCommands: MutableMap<Long, UserCommand<*>> = mutableMapOf()

    public override suspend fun initialize(commands: List<ApplicationCommand<*>>) {
        if (!bot.settings.applicationCommandsBuilder.register) {
            logger.debug {
                "Application command registration is disabled, pairing existing commands with extension commands"
            }
        }

        try {
            logger.debug { "Syncing all application commands.." }
            syncAll(true, commands)
        } catch (t: Throwable) {
            logger.error(t) { "Failed to synchronise application commands" }
        }
    }

    // region: Untyped sync functions

    /** Register multiple generic application commands. **/
    public open suspend fun syncAll(removeOthers: Boolean = false, commands: List<ApplicationCommand<*>>) {
        val groupedCommands = commands.groupBy { it.guildId }.toMutableMap()

        if (removeOthers && !groupedCommands.containsKey(null)) {
            groupedCommands[null] = listOf()
        }

        groupedCommands.forEach {
            try {
                logger.debug { "Syncing for: ${it.key?.toString() ?: "Global"}" }
                sync(removeOthers, it.key, it.value)
            } catch (e: ErrorResponseException) {
                logger.error(e) {
                    buildString {
                        if (it.key == null) {
                            append("Failed to synchronise global application commands")
                        } else {
                            append("Failed to synchronise application commands for guild with ID: ${it.key}")
                        }
                        append("\n        Discord error message: ${e.errorResponse}")

                        if (e.errorResponse == ErrorResponse.MISSING_ACCESS) {
                            append(
                                "\n        Double-check that the bot was added to this guild with the " +
                                    "`application.commands` scope enabled"
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                logger.error(t) {
                    if (it.key == null) {
                        "Failed to synchronise global application commands"
                    } else {
                        "Failed to synchronise application commands for guild with ID: ${it.key}"
                    }
                }
            }
        }

        if (!bot.settings.applicationCommandsBuilder.syncPermissions) {
            logger.debug { "Skipping permissions synchronisation, as it was disabled." }

            return
        }
    }

    /** Register multiple generic application commands. **/
    public open suspend fun sync(
        removeOthers: Boolean = false,
        guildId: Long?,
        commands: List<ApplicationCommand<*>>,
    ) {
        // NOTE: Someday, discord will make real i18n possible, we hope...
        val locale = bot.settings.i18nBuilder.defaultLocale

        val guild = if (guildId != null) {
            kord.getGuildById(guildId)
                ?: return logger.debug {
                    "Cannot register application commands for guild ID $guildId, " +
                        "as the guild seems to be missing."
                }
        } else {
            null
        }

        // Get guild commands if we're registering them (guild != null), otherwise get global commands
        val gwSessions = kord.shards.firstOrNull() ?: return logger.error { "No gw session." }
        val registered = guild?.retrieveCommands()?.await()
            ?: gwSessions.retrieveCommands().await()

        if (!bot.settings.applicationCommandsBuilder.register) {
            commands.forEach { commandObj ->
                val existingCommand = registered.firstOrNull { commandObj.matches(locale, it) }

                if (existingCommand != null) {
                    when (commandObj) {
                        is MessageCommand<*> -> messageCommands[existingCommand.idLong] = commandObj
                        is SlashCommand<*, *> -> slashCommands[existingCommand.idLong] = commandObj
                        is UserCommand<*> -> userCommands[existingCommand.idLong] = commandObj
                    }
                }
            }

            return  // We're only syncing them up, there's no other API work to do
        }

//        // Extension commands that haven't been registered yet
//        val toAdd = commands.filter { aC -> registered.all { dC -> !aC.matches(locale, dC) } }
//
//        // Extension commands that were previously registered
//        val toUpdate = commands.filter { aC -> registered.any { dC -> aC.matches(locale, dC) } }
//
//        // Registered Discord commands that haven't been provided by extensions
//        val toRemove = if (removeOthers) {
//            registered.filter { dC -> commands.all { aC -> !aC.matches(locale, dC) } }
//        } else {
//            listOf()
//        }
//
//        logger.info {
//            buildString {
//                if (guild == null) {
//                    append(
//                        "Global application commands: ${toAdd.size} to add / " +
//                            "${toUpdate.size} to update / " +
//                            "${toRemove.size} to remove"
//                    )
//                } else {
//                    append(
//                        "Application commands for guild ${guild.name}: ${toAdd.size} to add / " +
//                            "${toUpdate.size} to update / " +
//                            "${toRemove.size} to remove"
//                    )
//                }
//
//                if (!removeOthers) {
//                    append("\nThe `removeOthers` parameter is `false`, so no commands will be removed.")
//                }
//            }
//        }

        val builder: CommandListUpdateAction.() -> Unit = {
            for (cmd in commands) {
                val (localizedName, nameLocalizations) = cmd.localizedName

                val commandData = when (cmd) {
                    is MessageCommand<*> -> Commands.message(localizedName).apply {
                        this.setNameLocalizations(nameLocalizations)
                        this.register(locale, cmd)
                    }

                    is SlashCommand<*, *> -> {
                        val (localizedDescription, descriptionLocalizations) = cmd.localizedDescription
                        Commands.slash(localizedName, localizedDescription).apply {
                            this.setNameLocalizations(nameLocalizations)
                            this.setDescriptionLocalizations(descriptionLocalizations)
                            runBlocking { register(locale, cmd) }
                        }
                    }

                    is UserCommand<*> -> Commands.user(cmd.name).apply {
                        this.setNameLocalizations(nameLocalizations)
                        this.register(locale, cmd)
                    }
                    else -> {
                        logger.error {
                            "Cannot register: $cmd because it is an unknown command implementation."
                        }
                        break
                    }
                }
                this.addCommands(
                    commandData.apply {
                        cmd.fillCommandData()
                    }
                )
            }
        }

        val discordCommandList =
            if (guild != null) {
                guild.updateCommands(builder).await()
            } else {
                gwSessions.updateCommands(builder).await()
            }

        for (appCmd in commands) {
            val match = discordCommandList.first { appCmd.matches(locale, it) }

            when (appCmd) {
                is MessageCommand<*> -> messageCommands[match.idLong] = appCmd
                is SlashCommand<*, *> -> slashCommands[match.idLong] = appCmd
                is UserCommand<*> -> userCommands[match.idLong] = appCmd
            }
        }
//        val toCreate = toAdd + toUpdate
//
//        val builder: suspend MultiApplicationCommandBuilder.() -> Unit = {
//            toCreate.forEach {
//                val (name, nameLocalizations) = it.localizedName
//
//                logger.trace { "Adding/updating ${it.type.name} command: $name" }
//
//                when (it) {
//                    is MessageCommand<*> -> message(name) {
//                        this.nameLocalizations = nameLocalizations
//                        this.register(locale, it)
//                    }
//                    is UserCommand<*> -> user(name) {
//                        this.nameLocalizations = nameLocalizations
//                        this.register(locale, it)
//                    }
//
//                    is SlashCommand<*, *> -> {
//                        val (description, descriptionLocalizations) = it.localizedDescription
//                        input(name, description) {
//                            this.nameLocalizations = nameLocalizations
//                            this.descriptionLocalizations = descriptionLocalizations
//                            this.register(locale, it)
//                        }
//                    }
//                }
//            }
//        }
//
//        @Suppress("IfThenToElvis")  // Ultimately, this is far more readable
//        val response = if (guild == null) {
//            // We're registering global commands here, if the guild is null
//
//            kord.createGlobalApplicationCommands { builder() }.toList()
//        } else {
//            // We're registering guild-specific commands here, if the guild is available
//            guild.createApplicationCommands { builder() }.toList()
//        }
//
//        // Next, we need to associate all the commands we just registered with the commands in our extensions

//
//        if (toAdd.isEmpty() && toUpdate.isEmpty()) {
//            // Finally, we can remove anything that needs to be removed
//            toRemove.forEach {
//                logger.trace { "Removing ${it.type.name} command: ${it.name}" }
//
//                @Suppress("MagicNumber")  // not today, Detekt
//                try {
//                    it.delete()
//                } catch (e: KtorRequestException) {
//                    if (e.status.code != 404) {
//                        throw e
//                    }
//                }
//            }
//        }
//
        logger.info {
            if (guild == null) {
                "Finished synchronising global application commands"
            } else {
                "Finished synchronising application commands for guild ${guild.name}"
            }
        }
    }

    // endregion

    // region: Typed registration functions

    /** Register a message command. **/
    public override suspend fun register(command: MessageCommand<*>): MessageCommand<*>? {
        val commandId = createDiscordCommand(command) ?: return null

        messageCommands[commandId] = command

        return command
    }

    /** Register a slash command. **/
    public override suspend fun register(command: SlashCommand<*, *>): SlashCommand<*, *>? {
        val commandId = createDiscordCommand(command) ?: return null

        slashCommands[commandId] = command

        return command
    }

    /** Register a user command. **/
    public override suspend fun register(command: UserCommand<*>): UserCommand<*>? {
        val commandId = createDiscordCommand(command) ?: return null

        userCommands[commandId] = command

        return command
    }

    // endregion

    // region: Unregistration functions

    /** Unregister a message command. **/
    public override suspend fun unregister(command: MessageCommand<*>, delete: Boolean): MessageCommand<*>? {
        val filtered = messageCommands.filter { it.value == command }
        val id = filtered.keys.firstOrNull() ?: return null

        if (delete) {
            deleteGeneric(command, id)
        }

        return messageCommands.remove(id)
    }

    /** Unregister a slash command. **/
    public override suspend fun unregister(command: SlashCommand<*, *>, delete: Boolean): SlashCommand<*, *>? {
        val filtered = slashCommands.filter { it.value == command }
        val id = filtered.keys.firstOrNull() ?: return null

        if (delete) {
            deleteGeneric(command, id)
        }

        return slashCommands.remove(id)
    }

    /** Unregister a user command. **/
    public override suspend fun unregister(command: UserCommand<*>, delete: Boolean): UserCommand<*>? {
        val filtered = userCommands.filter { it.value == command }
        val id = filtered.keys.firstOrNull() ?: return null

        if (delete) {
            deleteGeneric(command, id)
        }

        return userCommands.remove(id)
    }

    // endregion

    // region: Event handlers

    /** Event handler for message commands. **/
    public override suspend fun handle(event: MessageContextInteractionEvent) {
        val commandId = event.interaction.commandIdLong
        val command = messageCommands[commandId]

        command ?: return logger.warn { "Received interaction for unknown message command: $commandId" }

        command.doCall(event)
    }

    /** Event handler for slash commands. **/
    public override suspend fun handle(event: SlashCommandInteractionEvent) {
        val commandId = event.interaction.commandIdLong
        val command = slashCommands[commandId]

        command ?: return logger.warn { "Received interaction for unknown slash command: $commandId" }

        command.doCall(event)
    }

    /** Event handler for user commands. **/
    public override suspend fun handle(event: UserContextInteractionEvent) {
        val commandId = event.interaction.commandIdLong
        val command = userCommands[commandId]

        command ?: return logger.warn { "Received interaction for unknown user command: $commandId" }

        command.doCall(event)
    }

    override suspend fun handle(event: CommandAutoCompleteInteractionEvent) {
        val commandId = event.interaction.commandIdLong
        val command = slashCommands[commandId]?.findCommand(event)

        command ?: return logger.warn { "Received autocomplete interaction for unknown command: $commandId" }

        if (command.arguments == null) {
            return logger.trace { "Command $command doesn't have any arguments." }
        }

        val option = event.interaction.focusedOption

        val arg = command.arguments!!().args.firstOrNull {
            it.getDefaultTranslatedDisplayName(translationsProvider, command) == option.name
        }

        arg ?: return logger.warn {
            "Autocomplete event for command $command has an unknown focused option: ${option.name}."
        }

        val callback = arg.converter.genericBuilder.autoCompleteCallback

        callback ?: return logger.trace {
            "Autocomplete event for command $command has an focused option without a callback: ${option.name}."
        }

        callback(event.interaction, event)
    }

    // endregion
}
