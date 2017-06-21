@file:JvmName("Pongo")

package net.emaze.pongo

import com.fasterxml.jackson.annotation.JsonIgnore
import net.emaze.pongo.annotation.handlerFactory
import net.emaze.pongo.proxy.delegate
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.util.*

/**
 * Identifiable is the base class of an entity.
 * It maintains the metadata (surrogate ID and version number) needed to identify the entity in the store.
 */
abstract class Identifiable(@JsonIgnore var metadata: Metadata? = null) {

    data class Metadata(val identity: Long, val version: Long)

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

fun <T : Identifiable> T.attach(identifiable: Identifiable): T {
    this.metadata = identifiable.metadata
    return this
}

class OptimisticLockException(message: String) : RuntimeException(message)

interface EntityRepository<T : Identifiable> {

    val entityClass: Class<T>

    /**
     * Save or update the entity.
     *
     * @throws OptimisticLockException if the entity has been already updated by another transaction
     */
    fun save(entity: T): T

    /**
     * Delete the entity.
     *
     * @throws IllegalArgumentException if the given entity has not an identity
     * @throws IllegalStateException if the entity doesn't exists of the store
     */
    fun delete(entity: T)

    /**
     * Delete all the entities without lock the whole repository.
     */
    fun deleteAll()

    /**
     * Get all the entities.
     */
    fun findAll(): List<T> = findAll("")

    /**
     * Get all the entities satisfying the query.
     */
    fun findAll(query: String, vararg params: Any?): List<T>

    /**
     * Get all the entities with the properties equals to the example's ones.
     */
    fun findAllLike(example: Any): List<T>

    /**
     * Get the first entity.
     */
    fun findFirst(): Optional<T> = findFirst("")

    /**
     * Get the first entity satisfying the query.
     */
    fun findFirst(query: String, vararg params: Any?): Optional<T>

    /**
     * Get the first entity with the properties equals to the example's ones.
     */
    fun findFirstLike(example: Any): Optional<T>
}

/**
 * The factory of entity repositories.
 */
interface EntityRepositoryFactory {

    fun <T : Identifiable> create(entityClass: Class<T>): EntityRepository<T>
}

inline fun <reified T : Identifiable> EntityRepositoryFactory.create(): EntityRepository<T> = create(T::class.java)

fun <T : Identifiable, R : EntityRepository<T>> EntityRepositoryFactory.create(entityClass: Class<T>, targetClass: Class<R>): R =
    create(entityClass).lift(targetClass)

fun <T : Identifiable, R : EntityRepository<T>> EntityRepository<T>.lift(targetClass: Class<R>): R =
    delegate(targetClass, handlerFactory(this))