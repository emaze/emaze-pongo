package net.emaze.pongo.postgres

import net.emaze.pongo.EntityRepositoryFactory
import net.emaze.pongo.Identifiable
import net.emaze.pongo.JsonJdbiPlugin
import net.emaze.pongo.mysql.MysqlEntityRepository
import org.jdbi.v3.core.Jdbi

class MysqlEntityRepositoryFactory(private val jdbi: Jdbi) : EntityRepositoryFactory {

    override fun <T : Identifiable> create(entityClass: Class<T>): MysqlEntityRepository<T> {
        jdbi.installPlugin(JsonJdbiPlugin())
        return MysqlEntityRepository(entityClass, jdbi)
    }
}

inline fun <reified T : Identifiable> MysqlEntityRepositoryFactory.create(): MysqlEntityRepository<T> =
    create(T::class.java)

