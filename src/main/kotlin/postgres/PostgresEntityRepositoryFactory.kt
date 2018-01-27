package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import net.emaze.pongo.EntityRepositoryFactory
import net.emaze.pongo.Identifiable
import javax.sql.DataSource

class PostgresEntityRepositoryFactory(val dataSource: DataSource, val mapper: ObjectMapper) : EntityRepositoryFactory {

    override fun <T : Identifiable> create(entityClass: Class<T>) =
        PostgresEntityRepository(entityClass, dataSource, mapper)
}

inline fun <reified T : Identifiable> PostgresEntityRepositoryFactory.create(): PostgresEntityRepository<T> =
    create(T::class.java)

