package net.emaze.pongo.functional

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.attach
import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionalTest {

    data class Entity(val x: Int) : Identifiable()

    @Test
    fun itShouldSaveAndReturnMappedEntity() {
        val repository = mock<EntityRepository<Entity>> {
            on { save(Entity(1)) } doReturn Entity(2)
        }
        val updater = repository.update { Entity(it.x + 1) }
        val got = updater(Entity(0).attach(Identifiable.Metadata(identity = 1, version = 3)))
        assertEquals(Entity(2), got)
    }
}