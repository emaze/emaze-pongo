package net.emaze.pongo

import net.emaze.pongo.Identifiable.Metadata
import org.junit.Assert.assertEquals
import org.junit.Test

class IdentifiableTest {

    class Sample(metadata: Metadata? = null) : Identifiable(metadata)
    class Other : Identifiable()

    @Test
    fun itIsEqualToSameInstance() {
        val it = Sample()
        assertEquals(true, it.equals(it))
    }

    @Test
    fun itIsNotEqualToNull() {
        assertEquals(false, Sample().equals(null))
    }

    @Test
    fun itIsNotEqualToNonIdentifiableObject() {
        assertEquals(false, Sample().equals(Any()))
    }

    @Test
    fun itIsNotEqualToOtherIdentifiableType() {
        assertEquals(false, Sample().equals(Other()))
    }

    @Test
    fun itIsNotEqualToCompatibleIdentifiableWhenMetadataAreDifferent() {
        assertEquals(false, Sample(Metadata(1, 2)).equals(Sample(Metadata(1, 3))))
    }

    @Test
    fun itIsNotEqualToCompatibleIdentifiableWhenBothMetadataAreNull() {
        assertEquals(false, Sample().equals(Sample()))
    }

    @Test
    fun itIsEqualToCompatibleIdentifiableWhenMetadataAreEqual() {
        assertEquals(true, Sample(Metadata(1, 2)).equals(Sample(Metadata(1, 2))))
    }
}