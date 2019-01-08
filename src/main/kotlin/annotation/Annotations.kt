package net.emaze.pongo.annotation

/**
 * Declares the "where" clause used by the annotated repository method.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Where(val value: String)

/**
 * Indicates that the annotated repository method can return null.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable
