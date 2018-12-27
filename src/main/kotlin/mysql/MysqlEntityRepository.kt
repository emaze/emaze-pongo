package net.emaze.pongo.mysql

import net.emaze.pongo.BaseRelationalEntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.Json
import org.jdbi.v3.core.Jdbi

open class MysqlEntityRepository<T : Identifiable>(
    entityClass: Class<T>,
    jdbi: Jdbi
) : BaseRelationalEntityRepository<T>(entityClass, jdbi) {

    override fun createTable(): MysqlEntityRepository<T> = also {
        jdbi.open().use { handle ->
            handle.execute("""
                CREATE TABLE IF NOT EXISTS $tableName (
                  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                  version BIGINT NOT NULL,
                  data    JSON NOT NULL
                )
            """)
        }
    }

    override fun searchAllLike(example: Any) =
        searchAll("JSON_CONTAINS(data, ?, '$')", Json(example))

    override fun searchFirstLike(example: Any) =
        searchFirst("JSON_CONTAINS(data, ?, '$')", Json(example))
}