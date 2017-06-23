package net.emaze.pongo.annotation

import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.proxy.Handler
import net.emaze.pongo.proxy.HandlerFactory
import java.util.*
import kotlin.collections.LinkedHashSet

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable

internal fun <T : Identifiable> annotatedMethodHandlerFactory(): HandlerFactory<EntityRepository<T>> = { repository, method ->
    fun findAllAsList(query: String): Handler = { args -> repository.findAll(query, *args) }
    fun findAllAsSet(query: String): Handler = { args -> LinkedHashSet(repository.findAll(query, *args)) }
    fun findFirstOptional(query: String): Handler = { args -> repository.findFirst(query, *args) }
    fun findFirstNullable(query: String): Handler = { args -> repository.findFirst(query, *args).orElse(null) }
    fun findFirstNotNull(query: String): Handler = { args ->
        repository.findFirst(query, *args).orElseThrow { NoSuchElementException("The query $query has no result") }
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
