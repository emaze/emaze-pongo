package net.emaze.pongo.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.emaze.pongo.Identifiable;
import net.emaze.pongo.Pongo;
import net.emaze.pongo.postgres.Context;
import net.emaze.pongo.postgres.PostgresEntityRepository;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ITJavaUsage {

    static class JavaEntity extends Identifiable {

        final int x;
        final int y;

        @JsonCreator
        JavaEntity(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }
    }

    final PostgresEntityRepository<JavaEntity> repository = Context.INSTANCE.getFactory().create(JavaEntity.class);

    {
        repository.createTable().deleteAll();
    }

    @Test
    public void itCanInsertNewEntity() {
        repository.save(new JavaEntity(1, 2));
        final List<JavaEntity> got = repository.findAll();
        Assert.assertEquals(1, got.size());
    }

    @Test
    public void itCanUpdateAnExistentEntity() {
        final JavaEntity entity = repository.save(new JavaEntity(1, 2));
        final JavaEntity newEntity = repository.save(Pongo.attach(new JavaEntity(2, 3), entity));
        final List<JavaEntity> got = repository.findAll();
        Assert.assertEquals(1, got.size());
    }
}
