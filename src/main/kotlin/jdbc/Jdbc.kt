@file:JvmName("Jdbc")

package net.emaze.pongo.jdbc

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.*
import javax.sql.DataSource

fun <T> DataSource.withStatement(sql: String, f: PreparedStatement.() -> T): T =
    connection.use { conn -> conn.prepareStatement(sql).use(f) }

fun DataSource.execute(sql: String, vararg params: Any?): Unit =
    withStatement(sql) {
        setParams(*params)
        execute()
    }

fun DataSource.update(sql: String, vararg params: Any?): Int =
    withStatement(sql) {
        setParams(*params)
        executeUpdate()
    }

fun <T> DataSource.query(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> =
    withStatement(sql) {
        setParams(*params)
        val result = executeQuery()
        val results = ArrayList<T>()
        while (result.next()) results.add(mapper(result))
        results
    }

private fun PreparedStatement.setParams(vararg values: Any?) =
    (0 until values.size).forEach { index -> setParam(index + 1, values[index]) }

private fun PreparedStatement.setParam(index: Int, value: Any?) =
    when (value) {
        null -> setNull(index, Types.NULL)
        is String -> setString(index, value)
        is Int -> setInt(index, value)
        is Long -> setLong(index, value)
        is Boolean -> setBoolean(index, value)
        is Date -> setTimestamp(index, Timestamp(value.time))
        is Double -> setDouble(index, value)
        is Float -> setFloat(index, value)
        else -> setObject(index, value)
    }