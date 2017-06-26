package net.emaze.pongo.annotation

import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.proxy.MethodHandler
import net.emaze.pongo.proxy.MethodHandlerFactory
import java.util.*
import kotlin.collections.LinkedHashSet

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable

internal fun <T : Identifiable> annotatedMethodHandlerFactory(): MethodHandlerFactory<EntityRepository<T>> = { repository, method ->
    fun findAllAsList(query: String): MethodHandler = { args -> repository.searchAll(query, *args) }
    fun findAllAsSet(query: String): MethodHandler = { args -> LinkedHashSet(repository.searchAll(query, *args)) }
    fun findFirstOptional(query: String): MethodHandler = { args -> repository.searchFirst(query, *args) }
    fun findFirstNullable(query: String): MethodHandler = { args -> repository.searchFirst(query, *args).orElse(null) }
    fun findFirstNotNull(query: String): MethodHandler = { args ->
        repository.searchFirst(query, *args).orElseThrow { NoSuchElementException("The query $query has no result") }
    }

    val query = method.getAnnotation(Query::class.java)?.value ?: throw UnsupportedOperationException("The method ${method.name} has no @Query annotation")
    val nullable = method.isAnnotationPresent(Nullable::class.java)
    when {
        method.returnType.isAssignableFrom(List::class.java) -> findAllAsList(query)
        method.returnType.isAssignableFrom(Set::class.java) -> findAllAsSet(query)
        method.returnType == repository.entityClass -> if (nullable) findFirstNullable(query) else findFirstNotNull(query)
        method.returnType == Optional::class.java -> findFirstOptional(query)
        else -> throw UnsupportedOperationException("Unsupported return type ${method.returnType} of method ${method.name}")
    }
}
