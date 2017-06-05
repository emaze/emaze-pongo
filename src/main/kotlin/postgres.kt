package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import net.emaze.pongo.EntityRepository
import net.emaze.pongo.Identifiable
import net.emaze.pongo.OptimisticLockException
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.sql.PreparedStatement
import java.util.*
import javax.sql.DataSource

class Jsonb(val obj: Any)

open class PostgresJsonRepository<T : Identifiable>(
    val cls: Class<T>,
    val dataSource: DataSource,
    val mapper: ObjectMapper
) : EntityRepository<T> {

    companion object {
        private val logger = LoggerFactory.getLogger(PostgresJsonRepository::class.java)
    }

    val tableName: String by lazy {
        cls.simpleName!!
            .decapitalize()
            .replace("[A-Z]".toRegex(), { match -> "_${match.value.toLowerCase()}" })
    }

    fun createTable(): PostgresJsonRepository<T> =
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                CREATE TABLE IF NOT EXISTS $tableName (
                  id      BIGSERIAL PRIMARY KEY,
                  version BIGINT NOT NULL,
                  data    JSONB NOT NULL
                )
                """)
                .execute()
            return this
        }

    fun createIndex(): PostgresJsonRepository<T> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("CREATE INDEX IF NOT EXISTS ${tableName}_data_idx ON $tableName USING GIN (data)")
                .execute()
            return this
        }

    override fun save(entity: T): T =
        entity.metadata?.let { update(entity) } ?: insert(entity)

    override fun findAll(query: String, vararg params: Any): List<T> =
        dataSource.connection.use { conn ->
            val stat = conn.prepareStatement("select data, id, version from $tableName $query")
            fillArguments(stat, params)
            val result = stat.executeQuery()
            val results = ArrayList<T>()
            while (result.next()) {
                val entity = mapper.readValue(result.getString(1), cls)
                entity.metadata = Identifiable.Metadata(
                    identity = result.getLong(2),
                    version = result.getLong(3))
                results.add(entity)
            }
            return results
        }

    override fun findAllLike(example: Any): List<T> = findAll("where data @> ?", Jsonb(example))

    override fun findFirst(query: String, vararg params: Any) =
        Optional.ofNullable(findAll("$query limit 1", *params).getOrElse(0, { null }))

    override fun findFirstLike(example: Any) = findFirst("where data @> ?", Jsonb(example))

    override fun delete(entity: T) {
        logger.debug("Deleting entity {} of ID {} from {}", entity, entity.metadata, tableName)
        val identity = entity.metadata?.identity ?: throw IllegalArgumentException("Cannot delete the transient object $entity")
        dataSource.connection.use { conn ->
            val stat = conn.prepareStatement("delete from $tableName where id = ?")
            stat.setLong(1, identity)
            val deleted = stat.executeUpdate()
            if (deleted == 0) throw IllegalStateException("Cannot delete not existing entity $entity of ID ${entity.metadata}")
        }
    }

    override fun deleteAll() {
        logger.debug("Deleting all entities from {}", tableName)
        dataSource.connection.use { conn -> conn.prepareStatement("delete from $tableName").executeUpdate() }
    }

    private fun insert(entity: T): T {
        logger.debug("Inserting entity {} into {}", entity, tableName)
        entity.metadata = dataSource.connection.use { conn ->
            val stat = conn.prepareStatement("insert into $tableName(version, data) values(0, ?) returning id")
            stat.setObject(1, asJsonb(entity))
            val result = stat.executeQuery().apply { next() }
            Identifiable.Metadata(identity = result.getLong(1), version = 0)
        }
        return entity
    }

    private fun update(entity: T): T {
        logger.debug("Updating entity {} of ID {} into {}", entity.metadata, tableName)
        val metadata = entity.metadata ?: throw IllegalArgumentException("Cannot update the transient object $entity")
        entity.metadata = dataSource.connection.use { conn ->
            val stat = conn.prepareStatement("""
                update $tableName
                set data = ?, version = version + 1
                where id = ?
                returning version
            """).apply {
                setObject(1, asJsonb(entity))
                setLong(2, metadata.identity)
            }
            val result = stat.executeQuery().apply { next() }
            val version = result.getLong(1)
            if (version != metadata.version + 1) throw OptimisticLockException("Detected conflict of versions updating entity $entity")
            Identifiable.Metadata(identity = metadata.identity, version = version)
        }
        return entity
    }

    private fun fillArguments(statement: PreparedStatement, arguments: Array<out Any>) {
        (0 until arguments.size).forEach { index -> fillArgument(statement, index + 1, arguments[index]) }
    }

    private fun fillArgument(statement: PreparedStatement, index: Int, argument: Any) {
        when (argument) {
            is String -> statement.setString(index, argument)
            is Int -> statement.setInt(index, argument)
            is Long -> statement.setLong(index, argument)
            is Boolean -> statement.setBoolean(index, argument)
            is Date -> statement.setDate(index, java.sql.Date(argument.time))
            is Double -> statement.setDouble(index, argument)
            is Float -> statement.setFloat(index, argument)
            is Jsonb -> statement.setObject(index, asJsonb(argument.obj))
            else -> throw IllegalArgumentException("Cannot prepare statement with $argument at index $index")
        }
    }

    private fun asJsonb(obj: Any) = PGobject().apply {
        type = "jsonb"
        value = mapper.writeValueAsString(obj)
    }
}