package net.emaze.pongo.proxy

import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

typealias MethodHandler = (Array<Any?>) -> Any?
typealias MethodHandlerFactory<T> = (receiver: T, method: Method) -> MethodHandler

inline fun <reified T : Any, R : T> Class<R>.delegateTo(receiver: T, noinline methodHandlers: MethodHandlerFactory<T>): R =
    delegateTo(receiver, T::class.java, methodHandlers)

fun <T : Any, R : T> Class<R>.delegateTo(receiver: T, receiverClass: Class<T>, methodHandlers: MethodHandlerFactory<T>): R {
    val targetClass = this
    val handlers = mutableMapOf<Method, MethodHandler>()
    @Suppress("unchecked_cast")
    val proxy = Proxy.newProxyInstance(targetClass.classLoader, arrayOf(targetClass), { _, method, args ->
        val handler = handlers[method] ?: throw UnsupportedOperationException("Unsupported method $method")
        handler(args ?: emptyArray())
    }) as R
    val baseMethods = listOf(*receiverClass.methods)
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
    handlers[Object::class.java.getMethod("equals", Any::class.java)] = { args -> args[0] === proxy }
    handlers[Object::class.java.getMethod("hashCode")] = { Objects.hash(receiver, targetClass) }
    handlers[Object::class.java.getMethod("toString")] = { "Proxy of ${targetClass.simpleName} delegating to $receiver" }
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