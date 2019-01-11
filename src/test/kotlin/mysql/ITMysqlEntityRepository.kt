package net.emaze.pongo.integration

import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.emaze.pongo.Identifiable
import net.emaze.pongo.Identifiable.Metadata
import net.emaze.pongo.Json
import net.emaze.pongo.OptimisticLockException
import net.emaze.pongo.attach
import net.emaze.pongo.postgres.MysqlEntityRepositoryFactory
import org.jdbi.v3.core.Jdbi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Optional

object MysqlContext {
    val port = System.getenv("PONGO_MYSQL_PORT")
        ?.let { if (it.isBlank()) null else it }
        ?.toInt() ?: 3306
    val factory = MysqlEntityRepositoryFactory(Jdbi.create("jdbc:mysql://127.0.0.1:$port/pongo", "root", ""))
}

class ITMysqlEntityRepository {

    data class SomeEntity(var x: Int, var y: Int) : Identifiable()

    val repository = MysqlContext.factory.create(SomeEntity::class.java).apply {
        createTable().deleteAll()
    }

    init {
        Json.jackson.registerModule(KotlinModule())
    }

    @Test
    fun itCanSaveNewEntity() {
        repository.save(SomeEntity(1, 2))
        val got = repository.searchAll()
        assertEquals(1, got.size)
        assertEquals(listOf(SomeEntity(1, 2)), got)
    }

    @Test
    fun itCanCreateNewEntity() {
        repository.create(SomeEntity(1, 2).attach(Metadata(identity = 123)))
        val got = repository.search(123)
        assertEquals(Optional.of(SomeEntity(1, 2)), got)
    }

    @Test(expected = RuntimeException::class)
    fun itCannotCreateDuplicatedEntity() {
        val entity = SomeEntity(1, 2).attach(Metadata(identity = 123))
        repository.create(entity)
        repository.create(entity)
    }

    @Test
    fun newEntityShouldHaveMetadata() {
        val entity = repository.save(SomeEntity(1, 2))
        val (got) = repository.searchAll()
        assertNotNull(got.metadata)
        assertEquals(entity.metadata, got.metadata)
    }

    @Test(expected = OptimisticLockException::class)
    fun itCannotUpdateAnOldVersionOfEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        repository.save(entity.copy(x = 3).attach(entity))
        repository.save(entity.copy(x = 4).attach(entity))
    }

    @Test
    fun itCanUpdateAnExistingEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        entity.x = 3
        repository.save(entity)
        val got = repository.searchAll()
        assertEquals(1, got.size)
        assertEquals(listOf(SomeEntity(3, 2)), got)
    }


    @Test
    fun itCanDeleteAnExistingEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        repository.delete(entity)
        val got = repository.searchAll()
        assertEquals(0, got.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun itCannotDeleteATransientEntity() {
        repository.delete(SomeEntity(1, 2))
    }


    @Test(expected = IllegalStateException::class)
    fun itCannotDeleteANotExistingEntity() {
        val entity = SomeEntity(1, 2).attach(Metadata(identity = 1, version = 4))
        repository.delete(entity)
    }

    @Test
    fun itCanFindAnExistingEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        val got = repository.find(entity.metadata!!.identity)
        assertEquals(entity, got)
    }

    @Test(expected = NoSuchElementException::class)
    fun itCannotFindANotExistingEntity() {
        repository.find(-1)
    }

    @Test
    fun itCanSearchAllByCriteria() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.searchAll("JSON_EXTRACT(this, '$.x') < ?", 3)
        assertEquals(listOf(SomeEntity(1, 2), SomeEntity(2, 5)), got)
    }

    @Test
    fun itCanSearchAllByExample() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.searchAllLike(mapOf("x" to 2))
        assertEquals(listOf(SomeEntity(2, 5)), got)
    }

    @Test
    fun itCanSearchFirstByCriteria() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.searchFirst("JSON_EXTRACT(this, '$.x') < ?", 3)
        assertEquals(Optional.of(SomeEntity(1, 2)), got)
    }


    @Test
    fun itCanSearchFirstByExample() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(1, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.searchFirstLike(mapOf("x" to 1))
        assertEquals(Optional.of(SomeEntity(1, 2)), got)
    }

    @Test
    fun itCanSearchNothing() {
        repository.save(SomeEntity(1, 2))
        val got = repository.searchFirstLike(mapOf("x" to 10))
        assertEquals(Optional.empty<SomeEntity>(), got)
    }
}