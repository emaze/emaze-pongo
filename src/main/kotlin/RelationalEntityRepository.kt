package net.emaze.pongo

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.Query
import org.slf4j.LoggerFactory
import java.util.Optional

interface RelationalEntityRepository<T : Identifiable> : EntityRepository<T> {

    val tableName: String

    /**
     * Create the table if it doesn't exist.
     */
    fun createTable(): RelationalEntityRepository<T>
}

abstract class BaseRelationalEntityRepository<T : Identifiable>(
    final override val entityClass: Class<T>,
    val jdbi: Jdbi
) : RelationalEntityRepository<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val tableName: String = entityClass.simpleName
        .decapitalize()
        .replace("[A-Z]".toRegex()) { match -> "_${match.value.toLowerCase()}" }

    override fun save(entity: T) =
        entity.metadata?.let { update(entity) } ?: insert(entity)

    override fun searchAll(query: String, vararg params: Any?) =
        doSearch(query, params)

    override fun searchFirst(query: String, vararg params: Any?) =
        Optional.ofNullable(doSearch(query, params) { it.setMaxRows(1) }.getOrElse(0) { null })

    private fun doSearch(query: String, params: Array<out Any?>, f: (Query) -> Query = { it }): List<T> =
        jdbi.open().use { handle ->
            val where = if (query.isEmpty()) "" else "where $query"
            handle.createQuery("select this, id, version from $tableName $where")
                .also { query -> params.forEachIndexed { index, value -> query.bind(index, value) } }
                .also { query -> f(query) }
                .map { result, _ ->
                    val entity = Json.jackson.readValue(result.getString(1), entityClass)
                    entity.metadata = Identifiable.Metadata(
                        identity = result.getLong(2),
                        version = result.getLong(3)
                    )
                    entity
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
            handle.createUpdate("insert into $tableName(version, this) values(0, ?)")
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
                set this = :this, version = version + 1
                where id = :identity and version = :version
            """)
                .bindBean(entity.metadata)
                .bind("this", Json(entity))
                .execute()
        }
        if (updated != 1) throw OptimisticLockException("Detected conflict of versions updating entity $entity")
        entity.metadata = Identifiable.Metadata(identity = metadata.identity, version = metadata.version + 1)
        return entity
    }
}