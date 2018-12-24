package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import net.emaze.pongo.BaseRelationalEntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.Json
import net.emaze.pongo.OptimisticLockException
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.Query
import org.slf4j.LoggerFactory
import java.util.*

open class PostgreSQLEntityRepository<T : Identifiable>(
    entityClass: Class<T>,
    val jdbi: Jdbi,
    val mapper: ObjectMapper
) : BaseRelationalEntityRepository<T>(entityClass) {

    companion object {
        private val logger = LoggerFactory.getLogger(PostgreSQLEntityRepository::class.java)
    }

    override fun createTable(): PostgreSQLEntityRepository<T> = also {
        jdbi.open().use { handle ->
            handle.execute("""
                CREATE TABLE IF NOT EXISTS $tableName (
                  id      BIGSERIAL PRIMARY KEY,
                  version BIGINT NOT NULL,
                  data    JSONB NOT NULL
                )
            """)
        }
    }

    override fun save(entity: T): T =
        entity.metadata?.let { update(entity) } ?: insert(entity)

    override fun searchAll(query: String, vararg params: Any?): List<T> =
        doSearch(query, params)

    override fun searchAllLike(example: Any): List<T> =
        doSearch("where data @> ?", arrayOf(Json(example)))

    override fun searchFirst(query: String, vararg params: Any?) =
        Optional.ofNullable(doSearch(query, params) { it.setMaxRows(1) }.getOrElse(0) { null })

    override fun searchFirstLike(example: Any) =
        searchFirst("where data @> ?", Json(example))

    private fun doSearch(query: String, params: Array<out Any?>, f: (Query) -> Query = { it }): List<T> =
        jdbi.open().use { handle ->
            handle.createQuery("select data, id, version from $tableName $query")
                .also { query -> params.forEachIndexed { index, value -> query.bind(index, value) } }
                .also { query -> f(query) }
                .map { result, _ ->
                    mapper.readValue(result.getString(1), entityClass).apply {
                        metadata = Identifiable.Metadata(identity = result.getLong(2), version = result.getLong(3))
                    }
                }
                .list()
        }

    override fun delete(entity: T) {
        logger.debug("Deleting entity {} with {} from {}", entity, entity.metadata, tableName)
        val identity = entity.metadata?.identity
            ?: throw IllegalArgumentException("Cannot delete the transient object $entity")
        val deleted = jdbi.open().use { it.execute("delete from $tableName where id = ?", identity) }
        if (deleted == 0) throw IllegalStateException("Cannot delete not existing entity $entity of ID ${entity.metadata}")
    }

    override fun deleteAll() {
        logger.debug("Deleting all entities from {}", tableName)
        jdbi.open().use { it.execute("delete from $tableName") }
    }

    private fun insert(entity: T): T {
        logger.debug("Inserting entity {} into {}", entity, tableName)
        entity.metadata = jdbi.open().use { handle ->
            handle.createUpdate("insert into $tableName(version, data) values(0, ?)")
                .bind(0, Json(entity))
                .executeAndReturnGeneratedKeys()
                .map { result, _ -> Identifiable.Metadata(identity = result.getLong(1), version = 0) }
                .findOnly()
        }
        return entity
    }

    private fun update(entity: T): T {
        logger.debug("Updating entity {} with {} into {}", entity, entity.metadata, tableName)
        val metadata = entity.metadata ?: throw IllegalArgumentException("Cannot update the transient object $entity")
        val updated = jdbi.open().use { handle ->
            handle.createUpdate("""
                update $tableName
                set data = :data, version = version + 1
                where id = :identity and version = :version
            """)
                .bindBean(entity.metadata)
                .bind("data", Json(entity))
                .execute()
        }
        if (updated != 1) throw OptimisticLockException("Detected conflict of versions updating entity $entity")
        entity.metadata = Identifiable.Metadata(identity = metadata.identity, version = metadata.version + 1)
        return entity
    }
}