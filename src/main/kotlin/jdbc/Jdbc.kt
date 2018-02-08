@file:JvmName("Jdbc")

package net.emaze.pongo.jdbc

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.*
import java.util.regex.Pattern
import javax.sql.DataSource
import kotlin.collections.ArrayList

typealias IndexedParams = Array<out Any?>
typealias NamedParams = Map<String, Any?>

fun <T> DataSource.withStatement(sql: String, params: IndexedParams, f: PreparedStatement.() -> T): T =
    connection.use { conn ->
        conn.prepareStatement(sql).use { stat ->
            with(stat) {
                setParams(params)
                f()
            }
        }
    }

fun <T> DataSource.withNamedStatement(sql: String, params: NamedParams, f: PreparedStatement.() -> T): T {
    val regex = ":(${params.keys.joinToString("|", transform = Pattern::quote)})".toRegex()
    val newParams = ArrayList<Any?>()
    val newSql = sql.replace(regex) { newParams.add(params[it.groupValues[1]]); "?" }
    return withStatement(newSql, newParams.toTypedArray(), f)
}

fun DataSource.execute(sql: String, vararg params: Any?): Unit =
    withStatement(sql, params) {
        execute()
    }

fun DataSource.update(sql: String, params: NamedParams): Int =
    withNamedStatement(sql, params) {
        executeUpdate()
    }

fun DataSource.update(sql: String, vararg params: Any?): Int =
    withStatement(sql, params) {
        executeUpdate()
    }

fun <T> DataSource.query(sql: String, params: NamedParams, mapper: (ResultSet) -> T): List<T> =
    withNamedStatement(sql, params) {
        queryAll(mapper)
    }

fun <T> DataSource.query(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> =
    withStatement(sql, params) {
        queryAll(mapper)
    }

private fun <T> PreparedStatement.queryAll(mapper: (ResultSet) -> T): List<T> {
    val result = executeQuery()
    val results = ArrayList<T>()
    while (result.next()) results.add(mapper(result))
    return results
}

private fun PreparedStatement.setParams(values: IndexedParams) =
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