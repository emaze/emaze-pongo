package net.emaze.pongo.annotation

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Where(val value: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable
