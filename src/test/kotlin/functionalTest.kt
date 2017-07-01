package net.emaze.pongo.functional

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.attach
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class FunctionalTest {

    class Entity(val x: Int) : Identifiable() {
        override fun equals(other: Any?) =
            other is Entity && this.x == other.x && this.metadata == other.metadata

        override fun hashCode() = Objects.hash(x, metadata)
    }

    @Test
    fun updateFunctionSaveAndReturnMappedEntity() {
        val metadata = Identifiable.Metadata(identity = 1, version = 3)
        val repository = mock<EntityRepository<Entity>> {
            on { save(Entity(1).attach(metadata)) } doReturn Entity(2)
        }
        val updater = repository.update { Entity(it.x + 1) }
        val got = updater(Entity(0).attach(metadata))
        assertEquals(Entity(2), got)
    }

    @Test
    fun unitConvertThisFunctionToUnitFunction() {
        var box: Int = 0
        val got: (Int) -> Unit = { n: Int -> box = n }.unit()
        got(3)
        assertEquals(3, box)
    }
}