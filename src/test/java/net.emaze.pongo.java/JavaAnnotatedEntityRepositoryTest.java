package net.emaze.pongo.java;

import net.emaze.pongo.EntityRepository;
import net.emaze.pongo.Identifiable;
import net.emaze.pongo.Pongo;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JavaAnnotatedEntityRepositoryTest {

    public static class Entity extends Identifiable {
    }

    public interface Entities extends EntityRepository<Entity> {
        default void store(Entity entity) {
            save(entity);
        }
    }

    @Test
    public void proxyRepositoryIsAbleToInvokeDefaultMethods() {
        final EntityRepository repository = mock(EntityRepository.class);
        final Entities proxy = Pongo.lift(repository, Entities.class);
        final Entity entity = new Entity();
        proxy.store(entity);
        verify(repository).save(entity);
    }
}
