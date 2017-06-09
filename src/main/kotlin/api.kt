package net.emaze.pongo

import com.fasterxml.jackson.annotation.JsonIgnore
import java.lang.RuntimeException
import java.util.*

abstract class Identifiable(@JsonIgnore var metadata: Metadata? = null) {

    data class Metadata(val identity: Long, val version: Long)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (other !is Identifiable) return false
        if (this.javaClass != other.javaClass) return false
        return this.metadata == other.metadata
    }

    override fun hashCode() = metadata?.hashCode() ?: 0

    override fun toString() = "Identifiable(metadata=$metadata)"
}

fun <T : Identifiable> T.attach(metadata: Identifiable.Metadata?): T {
    this.metadata = metadata
    return this
}

fun <T : Identifiable> T.attachTo(identifiable: Identifiable): T {
    this.metadata = identifiable.metadata
    return this
}

class OptimisticLockException(message: String) : RuntimeException(message)

interface EntityRepository<T : Identifiable> {

    @Throws(OptimisticLockException::class)
    fun save(entity: T): T

    fun delete(entity: T)

    fun deleteAll()

    fun findAll(): List<T> = findAll()

    fun findAll(query: String, vararg params: Any): List<T>

    fun findAllLike(example: Any): List<T>

    fun findFirst(): Optional<T> = findFirst("")

    fun findFirst(query: String, vararg params: Any): Optional<T>

    fun findFirstLike(example: Any): Optional<T>
}

