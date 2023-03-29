/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.storage

import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.messageFor
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.events.Event
import org.koin.core.component.inject
import kotlin.reflect.KClass

/**
 * Class representing a storage unit. Storage units represent specific, single units of data, and explain how
 * to store, retrieve and serialize that data.
 *
 * Storage units instruct the data adapters, explaining exactly what needs to be done. However, those adapters are
 * free to handle the storage as they feel they need to.
 */
@Suppress("DataClassContainsFunctions", "DataClassShouldBeImmutable")
public open class StorageUnit<T : Data>(
    /** The type of data to store. **/
    public open val storageType: StorageType,

    /** The namespace - usually a plugin or extension ID. Represents a folder for file-backed storage. **/
    public open val namespace: String,

    /** The identifier - usually a specific category or name. Represents a filename for file-backed storage. **/
    public open val identifier: String,

    /** The classobj representing your data - usually retrieved via `MyDataClass::class`. **/
    public val dataType: KClass<T>,
) : KordExKoinComponent {
    /** Storage unit key - used to construct paths, or just as a string reference to this storage unit. **/
    public val unitKey: String = "${storageType.type}/$namespace/$identifier"

    protected val dataAdapter: DataAdapter<*> by inject()

    /** Channel context, supplied via [withChannel] or [withChannelFrom]. **/
    public open var channel: Long? = null
        internal set

    /** Guild context, supplied via [withGuild] or [withGuildFrom]. **/
    public open var guild: Long? = null
        internal set

    /** Message context, supplied via [withMessage] or [withMessageFrom]. **/
    public open var message: Long? = null
        internal set

    /** User context, supplied via [withUser] or [withUserFrom]. **/
    public open var user: Long? = null
        internal set

    /** Reference to the serializer for this storage unit's data type. **/
    @OptIn(InternalSerializationApi::class)
    public val serializer: KSerializer<T> = dataType.serializer()

    /**
     * Convenience function, allowing you to delete the data represented by this storage unit.
     *
     * @see DataAdapter.delete
     */
    public suspend fun delete(): Boolean =
        dataAdapter.delete(this)

    /**
     * Convenience function, allowing you to retrieve the data represented by this storage unit.
     *
     * @see DataAdapter.get
     */
    public suspend fun get(): T? =
        dataAdapter.get(this)

    /**
     * Convenience function, allowing you to reload the data represented by this storage unit.
     *
     * @see DataAdapter.reload
     */
    public suspend fun reload(): T? =
        dataAdapter.reload(this)

    /**
     * Convenience function, allowing you to save the cached data represented by this storage unit.
     *
     * @see DataAdapter.save
     */
    public suspend fun save(): T? =
        dataAdapter.save(this)

    /**
     * Convenience function, allowing you to save the given data object, as represented by this storage unit.
     *
     * @see DataAdapter.save
     */
    public suspend fun save(data: T): T =
        dataAdapter.save(this, data)

    /**
     * Copy this [StorageUnit], applying the given channel's ID to its context, but only if it's not a DM channel.
     */
    public suspend fun withChannel(channelObj: Channel): StorageUnit<T> {
        return copy().apply {
            if (channelObj !is PrivateChannel) {
                channel = channelObj.idLong
            }
        }
    }

    /**
     * Copy this [StorageUnit], applying the given channel ID to its context, but only if it's not a DM channel.
     */
    public fun withChannel(channelId: Long): StorageUnit<T> {
        return copy().apply {
            channel = channelId
        }
    }

    /**
     * Copy this [StorageUnit], applying the channel ID from the given event to its context, but only if it's present
     * and not a DM channel.
     */
    public suspend fun withChannelFrom(event: Event): StorageUnit<T> {
        return copy().apply {
            if (guildFor(event) != null) {
                channel = channelFor(event)?.id
            }
        }
    }

    /**
     * Copy this [StorageUnit], applying the given guild's ID to its context.
     */
    public fun withGuild(guildObj: Guild): StorageUnit<T> {
        return copy().apply {
            guild = guildObj.idLong
        }
    }

    /**
     * Copy this [StorageUnit], applying the given guild ID to its context.
     */
    public fun withGuild(guildId: Long): StorageUnit<T> {
        return copy().apply {
            guild = guildId
        }
    }

    /**
     * Copy this [StorageUnit], applying the guild ID from the given event to its context, if present.
     */
    public suspend fun withGuildFrom(event: Event): StorageUnit<T> {
        return copy().apply {
            guild = guildFor(event)?.id
        }
    }

    /**
     * Copy this [StorageUnit], applying the given message's ID to its context.
     */
    public fun withMessage(messageObj: Message): StorageUnit<T> {
        return copy().apply {
            message = messageObj.idLong
        }
    }

    /**
     * Copy this [StorageUnit], applying the given message ID to its context.
     */
    public fun withMessage(messageId: Long): StorageUnit<T> {
        return copy().apply {
            message = messageId
        }
    }

    /**
     * Copy this [StorageUnit], applying the message ID from the given event to its context, if present.
     */
    public suspend fun withMessageFrom(event: Event): StorageUnit<T> {
        return copy().apply {
            message = messageFor(event)?.id
        }
    }

    /**
     * Copy this [StorageUnit], applying the given user's ID to its context.
     */
    public fun withUser(userObj: User): StorageUnit<T> {
        return copy().apply {
            user = userObj.idLong
        }
    }

    /**
     * Copy this [StorageUnit], applying the given user ID to its context.
     */
    public fun withUser(userId: Long): StorageUnit<T> {
        return copy().apply {
            user = userId
        }
    }

    /**
     * Copy this [StorageUnit], applying the user ID from the given event to its context, if present.
     */
    public suspend fun withUserFrom(event: Event): StorageUnit<T> {
        return copy().apply {
            user = userFor(event)?.id
        }
    }

    /** Return a new [StorageUnit] object, containing a copy of all the data that's stored in this one. **/
    public open fun copy(): StorageUnit<T> {
        val unit = StorageUnit(
            storageType = storageType,
            namespace = namespace,
            identifier = identifier,
            dataType = dataType
        )

        unit.channel = channel
        unit.guild = guild
        unit.message = message
        unit.user = user

        return unit
    }

    override fun toString(): String = unitKey

    /** Generated function provided here because data classes don't care about non-constructor properties. **/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StorageUnit<*>

        if (storageType != other.storageType) return false
        if (namespace != other.namespace) return false
        if (identifier != other.identifier) return false
        if (unitKey != other.unitKey) return false
        if (channel != other.channel) return false
        if (guild != other.guild) return false
        if (message != other.message) return false
        if (user != other.user) return false

        return true
    }

    /** Generated function provided here because data classes don't care about non-constructor properties. **/
    override fun hashCode(): Int {
        var result = storageType.hashCode()

        result = 31 * result + namespace.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + unitKey.hashCode()
        result = 31 * result + (channel?.hashCode() ?: 0)
        result = 31 * result + (guild?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (user?.hashCode() ?: 0)

        return result
    }
}

/**
 *  Convenience function allowing you to create a StorageUnit without passing a class manually.
 *
 *  @param storageType The type of data to store.
 *  @param namespace The namespace - usually a plugin or extension ID. Represents a folder for file-backed storage.
 *  @param identifier The identifier - usually a specific category or name. Represents a filename for file-backed
 *  storage.
 */
@Suppress("FunctionName")
public inline fun <reified T : Data> StorageUnit(
    storageType: StorageType,
    namespace: String,
    identifier: String
): StorageUnit<T> =
    StorageUnit(
        storageType,
        namespace,
        identifier,
        T::class
    )
