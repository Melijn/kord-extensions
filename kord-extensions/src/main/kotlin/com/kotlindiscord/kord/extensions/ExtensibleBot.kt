/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.application.ApplicationCommandRegistry
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommandRegistry
import com.kotlindiscord.kord.extensions.events.EventHandler
import com.kotlindiscord.kord.extensions.events.KordExEvent
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.impl.HelpExtension
import com.kotlindiscord.kord.extensions.extensions.impl.SentryExtension
import com.kotlindiscord.kord.extensions.koin.KordExContext
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.types.Lockable
import com.kotlindiscord.kord.extensions.utils.loadModule
import com.kotlindiscord.kord.extensions.utils.scheduling.TaskConfig
import dev.minn.jda.ktx.events.listener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import org.koin.core.component.inject
import org.koin.dsl.bind

/**
 * An extensible bot, wrapping a Kord instance.
 *
 * This is your jumping-off point. ExtensibleBot provides a system for managing extensions, commands and event
 * handlers. Either subclass ExtensibleBot or use it as-is if it suits your needs.
 *
 * You shouldn't construct this class directly - use the builder pattern via the companion object's `invoke` method:
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`.
 *
 * @param settings Bot builder object containing the bot's settings.
 * @param token Token for connecting to Discord.
 */
public open class ExtensibleBot(
    public val settings: ExtensibleBotBuilder,
    private val token: String,
) : KordExKoinComponent, Lockable {

    override var mutex: Mutex? = Mutex()
    override var locking: Boolean = settings.membersBuilder.lockMemberRequests

    /** @suppress Meant for internal use by public inline function. **/
    public val shardManager: ShardManager by inject()

    /**
     * A list of all registered event handlers.
     */
    public open val eventHandlers: MutableList<EventHandler<out Event>> = mutableListOf()

    /**
     * A map of the names of all loaded [Extension]s to their instances.
     */
    public open val extensions: MutableMap<String, Extension> = mutableMapOf()

    /** @suppress **/
    public open val eventPublisher: MutableSharedFlow<Any> = MutableSharedFlow()

    /** A [Flow] representing a combined set of Kord events and Kord Extensions events. **/
    public open val events: SharedFlow<Any> = eventPublisher.asSharedFlow()

    /** @suppress **/
    public open var initialized: Boolean = false

    /** @suppress **/
    public open val logger: KLogger = KotlinLogging.logger {}

    /** @suppress Function that sets up the bot early on, called by the builder. **/
    public open suspend fun setup() {
        val builder = settings.kordBuilder(token) {
            val intents = mutableListOf<GatewayIntent>()
            settings.intentsBuilder?.invoke(intents)
            settings.kordHooks.forEach { it() }
            setEnabledIntents(intents)
        }

        loadModule { single { builder } bind ShardManager::class }

//        settings.cacheBuilder.dataCacheBuilder.invoke(kord, kord.cache)

        addDefaultExtensions()
    }

    /** Start up the bot and log into Discord. **/
    public open suspend fun start() {
        settings.hooksBuilder.runBeforeStart(this)

        val shardManager = getKoin().get<ShardManager>()
        if (!initialized) {
            try {
                registerListeners()
            } catch (t: NullPointerException) {
                logger.error { "If this is a jda NPE, try setting maxShards and shardId" }
                throw t
            }
        }
        shardManager.login() // before login no commands can be loaded due to a jda bug

        loadInterspersedExtensions()

        // at this point all extensions should be loaded, otherwise individual commands will be registered
        shardManager.listener<Event> {
            send(it)
        }

        val (status, activity) = settings.presenceBuilder
        shardManager.setPresenceProvider(status, activity)
    }

    /*
     * Submits extensions to discord if all requirements are met.
     */
    public open suspend fun submitExtensions() {
        val applicationCommandRegistry = getKoin().get<ApplicationCommandRegistry>()
        val guilds = HashSet<Long>()

        // Collect all guildIds from guild specific commands.
        // TODO: This system also doesn't work for sharded multiprocess bots at all
        extensions.values.forEach { extension ->
            extension.slashCommands.forEach { it.guildId?.let { it1 -> guilds.add(it1) } }
            extension.userCommands.forEach { it.guildId?.let { it1 -> guilds.add(it1) } }
            extension.messageCommands.forEach { it.guildId?.let { it1 -> guilds.add(it1) } }
            // TODO: Add chatCommands ?
        }

        while (true) {
            val allRequiredGuildsLoaded = guilds.all { id -> shardManager.getGuildById(id) != null }
            val allShardsLoaded = shardManager.shards.all { it.status == JDA.Status.CONNECTED }
            if (guilds.isEmpty() || allRequiredGuildsLoaded || allShardsLoaded) {
                applicationCommandRegistry.initialRegistration()
                break
            }
            delay(500)
        }
    }

    public open suspend fun loadInterspersedExtensions() {
        @Suppress("TooGenericExceptionCaught")
        settings.extensionsBuilder.extensions.forEach {
            try {
                addExtension(it)
            } catch (e: Exception) {
                logger.error(e) {
                    "Failed to set up extension: $it"
                }
            }
        }

        if (settings.pluginBuilder.enabled) {
            settings.startPlugins()
        }

        submitExtensions()
    }

    /**
     * Stop the bot by logging out [Kord].
     *
     * This will leave the Koin context intact, so subsequent restarting of the bot is possible.
     *
     * @see close
     **/
    public open suspend fun stop() {
        getKoin().get<ShardManager>().setIdle(true)
    }

    /**
     * Stop the bot by shutting down [Kord] and removing its Koin context.
     *
     * Restarting the bot after closing will result in undefined behavior
     * because the Koin context needed to start will no longer exist.
     *
     * If a bot has been closed, then it must be fully rebuilt to start again.
     *
     * If a new bot is going to be built, then the previous bot must be closed first.
     *
     * @see stop
     **/
    public open suspend fun close() {
        getKoin().get<ShardManager>().shutdown()
        KordExContext.stopKoin()
    }

    @Suppress("UnnecessaryParentheses")
    /** This function sets up all of the bot's default event listeners. **/
    public open suspend fun registerListeners() {
        shardManager.listener<SessionDisconnectEvent> {
            logger.warn { "Disconnected: ${it.closeCode}" }
        }

//        on<SelectMenuInteractionCreateEvent> {
//            getKoin().get<ComponentRegistry>().handle(this)
//        }

        if (settings.chatCommandsBuilder.enabled) {
            shardManager.listener<MessageReceivedEvent> {
                getKoin().get<ChatCommandRegistry>().handleEvent(it)
            }
        } else {
            logger.debug {
                "Chat command support is disabled - set `enabled` to `true` in the `chatCommands` builder" +
                    " if you want to use them."
            }
        }

        if (settings.applicationCommandsBuilder.enabled) {
            shardManager.listener<SlashCommandInteractionEvent> {
                getKoin().get<ApplicationCommandRegistry>().handle(it)
            }

            shardManager.listener<MessageContextInteractionEvent> {
                getKoin().get<ApplicationCommandRegistry>().handle(it)
            }

            shardManager.listener<UserContextInteractionEvent> {
                getKoin().get<ApplicationCommandRegistry>().handle(it)
            }

            shardManager.listener<CommandAutoCompleteInteractionEvent> {
                getKoin().get<ApplicationCommandRegistry>().handle(it)
            }
        } else {
            logger.debug {
                "Application command support is disabled - set `enabled` to `true` in the " +
                    "`applicationCommands` builder if you want to use them."
            }
        }

        if (!initialized) {
            eventHandlers.forEach { handler ->
                handler.listenerRegistrationCallable?.invoke() ?: logger.error {
                    "Event handler $handler does not have a listener registration callback. This should never happen!"
                }
            }

            initialized = true
        }
    }

    /** This function adds all of the default extensions when the bot is being set up. **/
    public open suspend fun addDefaultExtensions() {
        val extBuilder = settings.extensionsBuilder

        if (extBuilder.helpExtensionBuilder.enableBundledExtension) {
            this.addExtension(::HelpExtension)
        }

        if (extBuilder.sentryExtensionBuilder.enable && extBuilder.sentryExtensionBuilder.feedbackExtension) {
            this.addExtension(::SentryExtension)
        }
    }

    /**
     * Subscribe to an event. You shouldn't need to use this directly, but it's here just in case.
     *
     * You can subscribe to any type, realistically - but this is intended to be used only with Kord
     * [Event] subclasses, and our own [KordExEvent]s.
     *
     * @param T Types of event to subscribe to.
     * @param scope Coroutine scope to run the body of your callback under.
     * @param consumer The callback to run when the event is fired.
     */
    public inline fun <reified T : Event> on(
        launch: Boolean = true,
        scope: CoroutineScope = TaskConfig.coroutineScope,
        noinline consumer: suspend T.() -> Unit,
    ): Job =
        events.buffer(Channel.UNLIMITED)
            .filterIsInstance<T>()
            .onEach {
                runCatching {
                    if (launch) scope.launch { consumer(it) } else consumer(it)
                }.onFailure { logger.catching(it) }
            }.catch { logger.catching(it) }
            .launchIn(scope)

    /**
     * @suppress
     */
    public suspend inline fun send(event: GenericEvent) {
        eventPublisher.emit(event)
    }

    /**
     * Install an [Extension] to this bot.
     *
     * This function will call the given builder function and store the resulting extension object, ready to be
     * set up when the next [ReadyEvent] happens.
     *
     * @param builder Builder function (or extension constructor) that takes an [ExtensibleBot] instance and
     * returns an [Extension].
     */
    @Throws(InvalidExtensionException::class)
    public open suspend fun addExtension(builder: () -> Extension) {
        val extensionObj = builder.invoke()

        if (extensions.contains(extensionObj.name)) {
            logger.error {
                "Extension with duplicate name ${extensionObj.name} loaded - unloading previously registered extension"
            }
            unloadExtension(extensionObj.name)
        }

        extensions[extensionObj.name] = extensionObj
        loadExtension(extensionObj.name)

        if (!extensionObj.loaded) {
            logger.warn { "Failed to set up extension: ${extensionObj.name}" }
        } else {
            logger.debug { "Loaded extension: ${extensionObj.name}" }

            settings.hooksBuilder.runExtensionAdded(this, extensionObj)
        }
    }

    /**
     * Reload an unloaded [Extension] from this bot, by name.
     *
     * This function **does not** create a new extension object - it simply
     * calls its `setup()` function. Loaded extensions can
     * be unload again by calling [unloadExtension].
     *
     * This function simply returns if the extension isn't found.
     *
     * @param extension The name of the [Extension] to unload.
     */
    @Throws(InvalidExtensionException::class)
    public open suspend fun loadExtension(extension: String) {
        val extensionObj = extensions[extension] ?: return

        if (!extensionObj.loaded) {
            extensionObj.doSetup()
        }
    }

    /**
     * Find the first loaded extension that is an instance of the type provided in `T`.
     *
     * This can be used to find an extension based on, for example, an implemented interface.
     *
     * @param T Types to match extensions against.
     */
    public inline fun <reified T> findExtension(): T? =
        findExtensions<T>().firstOrNull()

    /**
     * Find all loaded extensions that are instances of the type provided in `T`.
     *
     * This can be used to find extensions based on, for example, an implemented interface.
     *
     * @param T Types to match extensions against.
     */
    public inline fun <reified T> findExtensions(): List<T> =
        extensions.values.filterIsInstance<T>()

    /**
     * Unload an installed [Extension] from this bot, by name.
     *
     * This function **does not** remove the extension object - it simply
     * removes its event handlers and commands. Unloaded extensions can
     * be loaded again by calling [loadExtension].
     *
     * This function simply returns if the extension isn't found.
     *
     * @param extension The name of the [Extension] to unload.
     */
    public open suspend fun unloadExtension(extension: String) {
        val extensionObj = extensions[extension] ?: return

        if (extensionObj.loaded) {
            extensionObj.doUnload()
        }
    }

    /**
     * Remove an installed [Extension] from this bot, by name.
     *
     * This function will unload the given extension (if it's loaded), and remove the
     * extension object from the list of registered extensions.
     *
     * @param extension The name of the [Extension] to unload.
     *
     * @suppress This is meant to be used with the module system, and isn't necessarily a user-facing API.
     * You need to be quite careful with this!
     */
    public open suspend fun removeExtension(extension: String) {
        unloadExtension(extension)

        extensions.remove(extension)
    }

    /**
     * Directly register an [EventHandler] to this bot.
     *
     * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
     * call the [Extension.event] function in your [Extension.setup] function.
     *
     * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
     *
     * @param handler The event handler to be registered.
     * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
     */
    @Throws(EventHandlerRegistrationException::class)
    public inline fun <reified T : Event> addEventHandler(handler: EventHandler<T>) {
        if (eventHandlers.contains(handler)) {
            throw EventHandlerRegistrationException(
                "Event handler already registered in '${handler.extension.name}' extension."
            )
        }

        if (initialized) {
            handler.listenerRegistrationCallable?.invoke() ?: error(
                "Event handler $handler does not have a listener registration callback. This should never happen!"
            )
        }

        eventHandlers.add(handler)
    }

    /**
     * Directly register an [EventHandler] to this bot.
     *
     * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
     * call the [Extension.event] function in your [Extension.setup] function.
     *
     * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
     *
     * @param handler The event handler to be registered.
     * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
     */
    @Throws(EventHandlerRegistrationException::class)
    public inline fun <reified T : Event> registerListenerForHandler(handler: EventHandler<T>): Job {
        return on<T> {
            handler.call(this)
        }
    }

    /**
     * Directly remove a registered [EventHandler] from this bot.
     *
     * This function is used when extensions are unloaded, in order to clear out their event handlers.
     * No exception is thrown if the event handler wasn't registered.
     *
     * @param handler The event handler to be removed.
     */
    public open fun removeEventHandler(handler: EventHandler<out Event>): Boolean = eventHandlers.remove(handler)
}

/**
 * DSL function for creating a bot instance. This is the Kord Extensions entrypoint.
 *
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`
 */
@Suppress("FunctionNaming")  // This is a factory function
public suspend fun ExtensibleBot(token: String, builder: suspend ExtensibleBotBuilder.() -> Unit): ExtensibleBot {
    val settings = ExtensibleBotBuilder()

    builder(settings)

    return settings.build(token)
}
