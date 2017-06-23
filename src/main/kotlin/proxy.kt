package net.emaze.pongo.proxy

import net.emaze.pongo.EntityRepository
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Proxy

typealias MethodHandler = (Array<Any?>) -> Any?
typealias MethodHandlerFactory<T> = (receiver: T, method: Method) -> MethodHandler

fun <T : Any, R : T> T.delegate(targetClass: Class<R>, methodHandlers: MethodHandlerFactory<T>): R {
    val receiver = this
    val handlers = mutableMapOf<Method, MethodHandler>()
    @Suppress("UNCHECKED_CAST")
    val proxy = Proxy.newProxyInstance(this.javaClass.classLoader, arrayOf(targetClass), { _, method, args ->
        val handler = handlers[method] ?: throw UnsupportedOperationException("Unsupported method $method")
        handler(args ?: emptyArray())
    }) as R
    val baseMethods: List<Method> = listOf(*EntityRepository::class.java.declaredMethods)
    baseMethods.associateByTo(
        handlers,
        { method -> method },
        { method -> { args -> method.invoke(receiver, *args) } }
    )
    val userMethods = listOf(*targetClass.methods) - baseMethods
    userMethods.associateByTo(
        handlers,
        { method -> method },
        { method ->
            if (method.isDefault) invokeDefaultMethod(method, targetClass, proxy)
            else methodHandlers(receiver, method)
        }
    )
    return proxy
}

private fun invokeDefaultMethod(method: Method, targetClass: Class<*>, proxy: Any): MethodHandler {
    val handler = MethodHandles.Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
        .apply { isAccessible = true }
        .newInstance(targetClass, -1 /* trusted */)
        .unreflectSpecial(method, targetClass)
        .bindTo(proxy)
    return { args -> handler.invokeWithArguments(*args) }
}