package net.emaze.pongo.java;

import net.emaze.pongo.EntityRepository;
import net.emaze.pongo.Identifiable;
import net.emaze.pongo.Pongo;
import net.emaze.pongo.postgres.Context;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JavaUsageTest {

    static class JavaEntity extends Identifiable {

        final int x;
        final int y;

        JavaEntity(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    final EntityRepository<JavaEntity> repository = Context.INSTANCE.repository(JavaEntity.class);

    @Test
    public void itCanInsertNewEntity() {
        repository.save(new JavaEntity(1, 2));
        final List<JavaEntity> got = repository.findAll();
        Assert.assertEquals(1, got.size());
    }

    @Test
    public void itCanUpdateAnExistentEntity() {
        final JavaEntity entity = repository.save(new JavaEntity(1, 2));
        final JavaEntity newEntity = repository.save(Pongo.attachTo(new JavaEntity(2, 3), entity));
        final List<JavaEntity> got = repository.findAll();
        Assert.assertEquals(1, got.size());
    }
}
