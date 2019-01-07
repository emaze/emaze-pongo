package net.emaze.pongo.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.emaze.pongo.EntityRepository;
import net.emaze.pongo.Identifiable;
import net.emaze.pongo.Pongo;
import net.emaze.pongo.annotation.Query;
import net.emaze.pongo.Pongoλ;
import net.emaze.pongo.postgres.PostgresContext;
import net.emaze.pongo.postgres.PostgresEntityRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ITJavaUsage {

    public static class JavaEntity extends Identifiable {

        public final int x;
        public final int y;

        @JsonCreator
        public JavaEntity(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface JavaEntityRepository extends EntityRepository<JavaEntity> {
        @Query("(data->>'x')::int < ?")
        List<JavaEntity> searchAllLowerThan(int x);
    }

    final PostgresEntityRepository<JavaEntity> postgresRepository = PostgresContext.INSTANCE.getFactory().create(JavaEntity.class);
    final JavaEntityRepository repository = Pongo.lift(postgresRepository, JavaEntityRepository.class);

    @Before
    public void setUp() {
        postgresRepository.createTable().deleteAll();
    }

    @Test
    public void itCanInsertNewEntity() {
        repository.save(new JavaEntity(1, 2));
        final List<JavaEntity> got = repository.searchAll();
        Assert.assertEquals(1, got.size());
    }

    @Test
    public void itCanUpdateAnExistentEntity() {
        final JavaEntity entity = repository.save(new JavaEntity(1, 2));
        final JavaEntity newEntity = repository.save(Pongo.attach(new JavaEntity(2, 3), entity));
        final List<JavaEntity> got = repository.searchAll();
        Assert.assertEquals(1, got.size());
    }

    @Test
    public void itCanInvokeProxiedMethods() {
        repository.save(new JavaEntity(1, 2));
        final List<JavaEntity> got = repository.searchAllLowerThan(10);
        Assert.assertEquals(true, got.size() > 0);
    }

    @Test
    public void itCanSearchAllEntities() {
        repository.save(new JavaEntity(1, 2));
        final List<JavaEntity> got = repository.searchAll();
        Assert.assertEquals(1, got.size());
    }

    @Test
    public void itCanMapEntities() {
        repository.save(new JavaEntity(1, 2));
        repository.searchFirst().ifPresent(Pongoλ.update(repository, it -> new JavaEntity(it.x + 1, it.y * 2))::invoke);
        final JavaEntity got = repository.searchFirst().get();
        Assert.assertEquals(2, got.x);
        Assert.assertEquals(4, got.y);
    }
}
