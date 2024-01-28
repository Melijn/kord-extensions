/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.koin

import okio.withLock
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.KoinContext
import org.koin.core.error.KoinAppAlreadyStartedException
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import java.util.concurrent.locks.ReentrantLock

/**
 * The [KoinContext] for bot instances.
 *
 * This contains the [KoinApplication] and its [Koin] instance for dependency injection.
 *
 * To use this context, implement [KordExKoinComponent].
 *
 * @see org.koin.core.context.GlobalContext
 */
public object KordExContext : KoinContext {
    /** The current [Koin] instance. */
    private var koin: Koin? = null

    /** The current [KoinApplication]. */
    private var koinApp: KoinApplication? = null

    private val mutex: ReentrantLock = ReentrantLock()

    /**
     * Gets the [Koin] instance.
     *
     * @throws IllegalStateException [KoinApplication] has not yet been started.
     */
    override fun get(): Koin = koin ?: error("KordEx KoinApplication has not been started")

    /** Gets the [Koin] instance or null if the [KoinApplication] has not yet been started. */
    override fun getOrNull(): Koin? = koin

    /** Gets the [KoinApplication] or null if the [KoinApplication] has not yet been started. */
    public fun getKoinApplicationOrNull(): KoinApplication? = koinApp

    /**
     * Registers a [KoinApplication] to as the current one for this context.
     *
     * @param koinApplication The application to registers.
     *
     * @throws KoinAppAlreadyStartedException The [KoinApplication] has already been instantiated.
     */
    private fun register(koinApplication: KoinApplication) {
        if (koin != null) {
            throw KoinAppAlreadyStartedException("KordEx Koin Application has already been started")
        }

        koinApp = koinApplication
        koin = koinApplication.koin
    }

    /** Closes and removes the current [Koin] instance. */
    override fun stopKoin(): Unit = mutex.withLock {
        koin?.close()
        koin = null
    }

    /**
     * Starts using the provided [KoinApplication] as the current one for this context.
     *
     * @param koinApplication The application to start with.
     *
     * @throws KoinAppAlreadyStartedException The [KoinApplication] has already been instantiated.
     */
    override fun startKoin(koinApplication: KoinApplication): KoinApplication = mutex.withLock {
        register(koinApplication)
        koinApplication.createEagerInstances()

        return koinApplication
    }

    /**
     * Starts using the provided [KoinAppDeclaration] to create the [KoinApplication] for this context.
     *
     * @param appDeclaration The application declaration to start with.
     *
     * @throws KoinAppAlreadyStartedException The [KoinApplication] has already been instantiated.
     */
    override fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication = mutex.withLock {
        val koinApplication = KoinApplication.init()

        register(koinApplication)
        appDeclaration(koinApplication)
        koinApplication.createEagerInstances()

        return koinApplication
    }

    /**
     * Loads a module into the [Koin] instance.
     *
     * @param module The module to load.
     */
    override fun loadKoinModules(module: Module): Unit = mutex.withLock {
        get().loadModules(listOf(module))
    }

    /**
     * Loads modules into the [Koin] instance.
     *
     * @param modules The modules to load.
     */
    override fun loadKoinModules(modules: List<Module>): Unit = mutex.withLock {
        get().loadModules(modules)
    }

    /**
     * Unloads a module from the [Koin] instance.
     *
     * @param module The module to unload.
     */
    override fun unloadKoinModules(module: Module): Unit = mutex.withLock {
        get().unloadModules(listOf(module))
    }

    /**
     * Unloads modules from the [Koin] instance.
     *
     * @param modules The modules to unload.
     */
    override fun unloadKoinModules(modules: List<Module>): Unit = mutex.withLock {
        get().unloadModules(modules)
    }
}
