package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import net.emaze.pongo.*
import net.emaze.pongo.jdbc.execute
import net.emaze.pongo.jdbc.query
import net.emaze.pongo.jdbc.update
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*
import javax.sql.DataSource

open class PostgreSQLEntityRepository<T : Identifiable>(
    entityClass: Class<T>,
    val dataSource: DataSource,
    val mapper: ObjectMapper
) : BaseRelationalEntityRepository<T>(entityClass) {

    companion object {
        private val logger = LoggerFactory.getLogger(PostgreSQLEntityRepository::class.java)
    }

    override fun createTable(): PostgreSQLEntityRepository<T> = also {
        dataSource.execute("""
            CREATE TABLE IF NOT EXISTS $tableName (
              id      BIGSERIAL PRIMARY KEY,
              version BIGINT NOT NULL,
              data    JSONB NOT NULL
            )
        """)
    }

    override fun save(entity: T): T =
        entity.metadata?.let { update(entity) } ?: insert(entity)

    override fun searchAll(query: String, vararg params: Any?): List<T> =
        dataSource.query("select data, id, version from $tableName $query", *params) { result ->
            mapper.readValue(result.getString(1), entityClass).apply {
                metadata = Identifiable.Metadata(identity = result.getLong(2), version = result.getLong(3))
            }
        }

    override fun searchAllLike(example: Any): List<T> = searchAll("where data @> ?", json(example))

    override fun searchFirst(query: String, vararg params: Any?) =
        Optional.ofNullable(searchAll("$query limit 1", *params).getOrElse(0, { null }))

    override fun searchFirstLike(example: Any) = searchFirst("where data @> ?", json(example))

    override fun delete(entity: T) {
        logger.debug("Deleting entity {} with {} from {}", entity, entity.metadata, tableName)
        val identity = entity.metadata?.identity
            ?: throw IllegalArgumentException("Cannot delete the transient object $entity")
        val deleted = dataSource.update("delete from $tableName where id = ?", identity)
        if (deleted == 0) throw IllegalStateException("Cannot delete not existing entity $entity of ID ${entity.metadata}")
    }

    override fun deleteAll() {
        logger.debug("Deleting all entities from {}", tableName)
        dataSource.update("delete from $tableName")
    }

    private fun insert(entity: T): T {
        logger.debug("Inserting entity {} into {}", entity, tableName)
        entity.metadata = dataSource.query("insert into $tableName(version, data) values(0, ?) returning id", json(entity)) {
            Identifiable.Metadata(identity = it.getLong(1), version = 0)
        }.first()
        return entity
    }

    private fun update(entity: T): T {
        logger.debug("Updating entity {} with {} into {}", entity, entity.metadata, tableName)
        val metadata = entity.metadata ?: throw IllegalArgumentException("Cannot update the transient object $entity")
        entity.metadata = dataSource.query("""
                update $tableName
                set data = ?, version = version + 1
                where id = ?
                returning version
            """, json(entity), metadata.identity) { result ->
            val version = result.getLong(1)
            if (version != metadata.version + 1) throw OptimisticLockException("Detected conflict of versions updating entity $entity")
            Identifiable.Metadata(identity = metadata.identity, version = version)
        }.first()
        return entity
    }

    protected fun json(obj: Any) = PGobject().apply {
        type = "jsonb"
        value = mapper.writeValueAsString(obj)
    }
}