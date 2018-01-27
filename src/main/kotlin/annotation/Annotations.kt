package net.emaze.pongo.annotation

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Nullable
