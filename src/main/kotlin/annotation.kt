@file:JvmName("AnnotatedRepository")

package net.emaze.pongo.annotation

import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import java.lang.invoke.MethodHandles
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
    val repository = this
    val handlers = mutableMapOf<Method, Handler>()
    @Suppress("UNCHECKED_CAST")
    val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(targetClass), { _, method, args ->
        val handler = handlers[method] ?: throw UnsupportedOperationException("Unsupported method $method")
        handler(args ?: emptyArray())
    }) as R
    val baseMethods: List<Method> = listOf(*EntityRepository::class.java.declaredMethods)
    baseMethods.associateByTo(
        handlers,
        { method -> method },
        { method -> { args -> method.invoke(repository, *args) } }
    )
    val userMethods = listOf(*targetClass.methods) - baseMethods
    userMethods.associateByTo(
        handlers,
        { method -> method },
        { method ->
            if (method.isDefault) invokeDefaultMethod(method, targetClass, proxy)
            else makeHandler(method, repository)
        }
    )
    return proxy
}

private typealias Handler = (Array<Any?>) -> Any?

private fun invokeDefaultMethod(method: Method, targetClass: Class<*>, proxy: Any): Handler {
    val handler = MethodHandles.Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
        .apply { isAccessible = true }
        .newInstance(targetClass, -1 /* trusted */)
        .unreflectSpecial(method, targetClass)
        .bindTo(proxy)
    return { args -> handler.invokeWithArguments(*args) }
}

private fun <T : Identifiable> makeHandler(method: Method, repository: EntityRepository<T>): Handler {
    fun findAllAsList(query: String): Handler = { args -> repository.findAll(query, *args) }
    fun findAllAsSet(query: String): Handler = { args -> LinkedHashSet(repository.findAll(query, *args)) }
    fun findFirstOptional(query: String): Handler = { args -> repository.findFirst(query, *args) }
    fun findFirstNullable(query: String): Handler = { args -> repository.findFirst(query, *args).orElse(null) }
    fun findFirstNotNull(query: String): Handler = { args ->
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
