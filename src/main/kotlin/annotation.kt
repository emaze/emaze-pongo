@file:JvmName("AnnotatedRepository")

package net.emaze.pongo.annotation

import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.LinkedHashSet

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable

fun <T : Identifiable, R : EntityRepository<T>> EntityRepository<T>.create(targetClass: Class<R>): R {
    fun invoke(method: Method): Handler<T> = { repository, args -> method.invoke(repository, *args) }
    val repository: EntityRepository<T> = this
    val baseMethods: List<Method> = listOf(*EntityRepository::class.java.declaredMethods)
    val baseHandlers = baseMethods.associateBy(
        { method -> method },
        { method -> invoke(method) }
    )
    val userMethods = listOf(*targetClass.methods) - baseMethods
    val userHandlers = userMethods.associateBy(
        { method -> method },
        { method -> getHandler(repository, method) }
    )
    val handlers = baseHandlers + userHandlers
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(targetClass), { proxy, method, args ->
        val handler = handlers.get(method) ?: throw UnsupportedOperationException("Unsupported method ${method.returnType}")
        handler(repository, args ?: emptyArray())
    }) as R
}

private typealias Handler<T> = (EntityRepository<T>, Array<Any?>) -> Any?

private fun <T : Identifiable> getHandler(repository: EntityRepository<T>, method: Method): Handler<T> {
    fun findAllAsList(query: String): Handler<T> = { repository, args -> repository.findAll(query, *args) }
    fun findAllAsSet(query: String): Handler<T> = { repository, args -> LinkedHashSet(repository.findAll(query, *args)) }
    fun findFirstOptional(query: String): Handler<T> = { repository, args -> repository.findFirst(query, *args) }
    fun findFirstNullable(query: String): Handler<T> = { repository, args -> repository.findFirst(query, *args).orElse(null) }
    fun findFirstNotNull(query: String): Handler<T> = { repository, args ->
        repository.findFirst(query, *args).orElseThrow { NoSuchElementException("The query $query has no result") }
    }

    val query = method.getAnnotation(Query::class.java)?.value ?: throw UnsupportedOperationException("The method ${method.name} has no @Query annotation")
    val nullable = method.isAnnotationPresent(Nullable::class.java)
    return when {
        method.returnType.isAssignableFrom(List::class.java) -> findAllAsList(query)
        method.returnType.isAssignableFrom(Set::class.java) -> findAllAsSet(query)
        method.returnType == repository.entityClass -> if (nullable) findFirstNullable(query) else findFirstNotNull(query)
        method.returnType == Optional::class.java -> findFirstOptional(query)
        else -> throw UnsupportedOperationException("Unsupported return type ${method.returnType} of method ${method.name}")
    }
}


