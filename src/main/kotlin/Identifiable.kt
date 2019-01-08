@file:JvmName("Pongo")
@file:JvmMultifileClass

package net.emaze.pongo

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Identifiable is the base class of an entity.
 * It maintains the metadata (surrogate ID and version number) needed to identify the entity in the store.
 */
abstract class Identifiable(@JsonIgnore var metadata: Metadata? = null) {

    data class Metadata(val identity: Long, val version: Long)

    /**
     * Convenience method to get the object identity.
     *
     * @throws IllegalStateException if this object has no metadata
     */
    @get:JsonIgnore
    val identity: Long
        get() = (metadata ?: throw IllegalStateException("Cannot get identity of a transient object")).identity

    /**
     * Two Identifiables are considered equals if they have the same class and the same metadata.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other === null) return false
        if (other !is Identifiable) return false
        if (this.javaClass != other.javaClass) return false
        return this.metadata != null && other.metadata != null && this.metadata == other.metadata
    }

    override fun hashCode() = metadata?.hashCode() ?: 0

    override fun toString() = "Identifiable(metadata=$metadata)"
}

fun <T : Identifiable> T.attach(metadata: Identifiable.Metadata?): T {
    this.metadata = metadata
    return this
}

fun <T : Identifiable> T.attach(identifiable: T): T {
    this.metadata = identifiable.metadata
    return this
}