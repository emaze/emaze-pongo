@file:JvmName("Pongo")
@file:JvmMultifileClass

package net.emaze.pongo

import net.emaze.pongo.annotation.annotatedMethodHandlerFactory
import net.emaze.pongo.proxy.delegateTo
import java.util.*
import kotlin.NoSuchElementException

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
    fun searchAll(): List<T> = searchAll("")

    /**
     * Get all the entities satisfying the query.
     */
    fun searchAll(query: String, vararg params: Any?): List<T>

    /**
     * Get all the entities with the properties equals to the example's ones.
     */
    fun searchAllLike(example: Any): List<T>

    /**
     * Get the first entity or empty if it is not found.
     */
    fun searchFirst(): Optional<T> = searchFirst("")

    /**
     * Get the first entity satisfying the query or empty.
     */
    fun searchFirst(query: String, vararg params: Any?): Optional<T>

    /**
     * Get the first entity with the properties equals to the example's ones.
     */
    fun searchFirstLike(example: Any): Optional<T>

    /**
     * Get the first entity.
     */
    fun findFirst(): T = findFirst("")

    /**
     * Get the first entity satisfying the query.
     */
    fun findFirst(query: String, vararg params: Any?): T =
        searchFirst(query, *params).orElseThrow {
            NoSuchElementException("Query on $entityClass have returned no results")
        }

    /**
     * Get the first entity with the properties equals to the example's ones.
     */
    fun findFirstLike(example: Any): T =
        searchFirstLike(example).orElseThrow {
            NoSuchElementException("Query on $entityClass have returned no results")
        }
}

/**
 * The factory of entity repositories.
 */
interface EntityRepositoryFactory {

    fun <T : Identifiable> create(entityClass: Class<T>): EntityRepository<T>
}

inline fun <reified T : Identifiable> EntityRepositoryFactory.create(): EntityRepository<T> = create(T::class.java)

/**
 * Create a proxied repository implementing the given target class, delegating methods
 * to the effective repository created by this factory for the specified entity class.
 *
 * The abstract methods of target class should be annotated with @Query.
 */
fun <T : Identifiable, R : EntityRepository<T>> EntityRepositoryFactory.create(entityClass: Class<T>, targetClass: Class<R>): R =
    create(entityClass).lift(targetClass)

/**
 * Lift the effective repository to the specified abstract target class.
 *
 * The abstract methods should be annotated with @Query.
 */
fun <T : Identifiable, R : EntityRepository<T>> EntityRepository<T>.lift(targetClass: Class<R>): R =
    targetClass.delegateTo(this, annotatedMethodHandlerFactory())