package net.emaze.pongo.annotation

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.lift
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class AnnotationBasedRepositoryTest {

    data class Entity(val x: Int) : Identifiable()

    interface Entities : EntityRepository<Entity> {
        @Query("methodReturningListQuery")
        fun methodReturningList(param: Int): List<Entity>

        @Query("methodReturningSetQuery")
        fun methodReturningSet(param: Int): Set<Entity>

        @Query("methodReturningNotNullEntityQuery")
        fun methodReturningNotNullEntity(param: String): Entity

        @Query("methodReturningNullableEntityQuery")
        @Nullable
        fun methodReturningNullableEntity(param: String): Entity?

        @Query("methodReturningOptionalEntityQuery")
        fun methodReturningOptionalEntity(param: String): Optional<Entity>
    }

    interface BadEntities : EntityRepository<Entity> {
        fun abstractMethodWithoutAnnotation()
    }

    @Test
    fun itShouldliftRepositoryMethodsToRepository() {
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
        }
        val proxy = repository.lift(Entities::class.java)
        proxy.save(Entity(1))
        proxy.findAll()
        proxy.findAll("query", 1, 2)
        verify(repository).save(Entity(1))
        verify(repository).findAll()
        verify(repository).findAll("query", 1, 2)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun itShouldFailInvokingMethodWithoutAnnotation() {
        val repository = mock<EntityRepository<Entity>>()
        val proxy = repository.lift(BadEntities::class.java)
        proxy.abstractMethodWithoutAnnotation()
    }

    @Test
    fun itShouldliftAnnotatedMethodsReturningListToFindAll() {
        val expected = listOf(Entity(1))
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findAll("methodReturningListQuery", 3) } doReturn expected
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(expected, proxy.methodReturningList(3))
    }

    @Test
    fun itShouldliftAnnotatedMethodsReturningSetToFindAll() {
        val expected = listOf(Entity(1), Entity(1))
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findAll("methodReturningSetQuery", 3) } doReturn expected
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(setOf(Entity(1)), proxy.methodReturningSet(3))
    }

    @Test
    fun itShouldliftAnnotatedMethodsReturningEntityToFindFirst() {
        val expected = Entity(1)
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findFirst("methodReturningNotNullEntityQuery", "found") } doReturn Optional.of(expected)
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(expected, proxy.methodReturningNotNullEntity("found"))
    }

    @Test(expected = Exception::class)
    fun annotatedMethodsReturningNotNullEntityFailsWhenEntityIsNotFound() {
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findFirst("methodReturningNotNullEntityQuery", "notFound") } doReturn Optional.empty()
        }
        val proxy = repository.lift(Entities::class.java)
        proxy.methodReturningNotNullEntity("notFound")
    }

    @Test
    fun annotatedMethodsReturningNullableEntityReturnsNullWhenEntityIsNotFound() {
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findFirst("methodReturningNullableEntityQuery", "notFound") } doReturn Optional.empty()
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(null, proxy.methodReturningNullableEntity("notFound"))
    }

    @Test
    fun annotatedMethodsReturningOptionalEntityReturnsEmptyWhenEntityIsNotFound() {
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findFirst("methodReturningOptionalEntityQuery", "notFound") } doReturn Optional.empty()
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(Optional.empty<Entity>(), proxy.methodReturningOptionalEntity("notFound"))
    }

    @Test
    fun annotatedMethodsReturningOptionalEntityReturnsEntityWhenItIsFound() {
        val repository = mock<EntityRepository<Entity>> {
            on { entityClass } doReturn Entity::class.java
            on { findFirst("methodReturningOptionalEntityQuery", "found") } doReturn Optional.of(Entity(1))
        }
        val proxy = repository.lift(Entities::class.java)
        assertEquals(Optional.of(Entity(1)), proxy.methodReturningOptionalEntity("found"))
    }
}