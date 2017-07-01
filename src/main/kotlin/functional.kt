@file:JvmName("Pongoλ")

package net.emaze.pongo.functional

import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.attach

/**
 * Decorate the given function in order to save the resulting entity using the metadata of the original one.
 *
 * @param f the mapping function
 * @return the mapped entity
 */
fun <T : Identifiable> EntityRepository<T>.update(f: (T) -> T): (T) -> T =
    { entity -> save(f(entity).attach(entity)) }

fun <T, U> ((T) -> U).unit(): (T) -> Unit = { this(it) }