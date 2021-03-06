package net.emaze.pongo.postgres

import net.emaze.pongo.EntityRepositoryFactory
import net.emaze.pongo.Identifiable
import net.emaze.pongo.JsonConfig
import net.emaze.pongo.JsonJdbiPlugin
import org.jdbi.v3.core.Jdbi
import postgres.PostgresJsonbArgumentFactory

class PostgresEntityRepositoryFactory(private val jdbi: Jdbi) : EntityRepositoryFactory {

    override fun <T : Identifiable> create(entityClass: Class<T>): PostgresEntityRepository<T> {
        val config = jdbi.getConfig(JsonConfig::class.java)
        config.argumentFactory = PostgresJsonbArgumentFactory()
        jdbi.installPlugin(JsonJdbiPlugin())
        return PostgresEntityRepository(entityClass, jdbi)
    }
}

inline fun <reified T : Identifiable> PostgresEntityRepositoryFactory.create(): PostgresEntityRepository<T> =
    create(T::class.java)

