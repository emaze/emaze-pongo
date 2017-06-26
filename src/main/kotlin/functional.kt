package net.emaze.pongo

import java.util.*

fun <T : Identifiable> EntityRepository<T>.mapAll(query: String = "", vararg params: Any, f: (T) -> T): List<T> =
    searchAll(query, *params).map { save(f(it)) }

fun <T : Identifiable> EntityRepository<T>.mapAllLike(example: Any, f: (T) -> T): List<T> =
    searchAllLike(example).map { save(f(it)) }

fun <T : Identifiable> EntityRepository<T>.mapFirst(query: String = "", vararg params: Any, f: (T) -> T): Optional<T> =
    searchFirst(query, *params).map { save(f(it)) }

fun <T : Identifiable> EntityRepository<T>.mapFirstLike(example: Any, f: (T) -> T): Optional<T> =
    searchFirstLike(example).map { save(f(it)) }