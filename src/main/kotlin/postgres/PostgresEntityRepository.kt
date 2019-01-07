package net.emaze.pongo.postgres

import net.emaze.pongo.BaseRelationalEntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.Json
import org.jdbi.v3.core.Jdbi

open class PostgresEntityRepository<T : Identifiable>(
    entityClass: Class<T>,
    jdbi: Jdbi
) : BaseRelationalEntityRepository<T>(entityClass, jdbi) {

    override fun createTable(): PostgresEntityRepository<T> = also {
        jdbi.open().use { handle ->
            handle.execute("""
                CREATE TABLE IF NOT EXISTS $tableName (
                  id      BIGSERIAL PRIMARY KEY,
                  version BIGINT NOT NULL,
                  this    JSONB NOT NULL
                )
            """)
        }
    }

    override fun searchAllLike(example: Any) =
        searchAll("this @> ?", Json(example))

    override fun searchFirstLike(example: Any) =
        searchFirst("this @> ?", Json(example))
}