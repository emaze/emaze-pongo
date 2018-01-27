@file:JvmName("Pongoλ")

package net.emaze.pongo

/**
 * Decorate the given function in order to save the resulting entity using the metadata of the original one.
 *
 * @param f the mapping function
 * @return the mapped entity
 */
fun <T : Identifiable> EntityRepository<T>.update(f: (T) -> T): (T) -> T =
    { entity -> save(f(entity).attach(entity)) }

/**
 * Cast this function to a function returning nothing (Unit).
 */
fun <T, U> ((T) -> U).unit(): (T) -> Unit = this as (T) -> Unit