package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import net.emaze.pongo.EntityRepositoryFactory
import net.emaze.pongo.Identifiable
import net.emaze.pongo.JsonJdbiPlugin
import org.jdbi.v3.core.Jdbi
import javax.sql.DataSource

class PostgresEntityRepositoryFactory(val dataSource: DataSource, val mapper: ObjectMapper) : EntityRepositoryFactory {

    override fun <T : Identifiable> create(entityClass: Class<T>): PostgreSQLEntityRepository<T> {
        val jdbi = Jdbi.create(dataSource).apply {
            installPlugin(JsonJdbiPlugin())
        }
        return PostgreSQLEntityRepository(entityClass, jdbi, mapper)
    }
}

inline fun <reified T : Identifiable> PostgresEntityRepositoryFactory.create(): PostgreSQLEntityRepository<T> =
    create(T::class.java)

