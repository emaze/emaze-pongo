package net.emaze.pongo.proxy

import java.lang.reflect.Method

typealias MethodHandler = (Array<Any?>) -> Any?
typealias MethodHandlerFactory<T> = (receiver: T, method: Method) -> MethodHandler