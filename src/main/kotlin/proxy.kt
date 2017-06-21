package net.emaze.pongo.proxy

import net.emaze.pongo.EntityRepository
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Proxy

typealias Handler = (Array<Any?>) -> Any?
typealias HandlerFactory = (method: Method) -> Handler

fun <R : Any> Any.delegate(targetClass: Class<R>, handlerFactory: HandlerFactory): R {
    val proxied = this
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
        { method -> { args -> method.invoke(proxied, *args) } }
    )
    val userMethods = listOf(*targetClass.methods) - baseMethods
    userMethods.associateByTo(
        handlers,
        { method -> method },
        { method ->
            if (method.isDefault) invokeDefaultMethod(method, targetClass, proxy)
            else handlerFactory(method)
        }
    )
    return proxy
}

private fun invokeDefaultMethod(method: Method, targetClass: Class<*>, proxy: Any): Handler {
    val handler = MethodHandles.Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
        .apply { isAccessible = true }
        .newInstance(targetClass, -1 /* trusted */)
        .unreflectSpecial(method, targetClass)
        .bindTo(proxy)
    return { args -> handler.invokeWithArguments(*args) }
}