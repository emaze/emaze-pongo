package net.emaze.pongo.java;

import java.util.List;
import net.emaze.pongo.EntityRepository;
import net.emaze.pongo.Identifiable;
import net.emaze.pongo.postgres.Context;
import org.junit.Assert;
import org.junit.Test;

public class JavaUsageTest {

    static class JavaEntity extends Identifiable {

        public final int x;
        public final int y;

        public JavaEntity(int x, int y) {
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
}
