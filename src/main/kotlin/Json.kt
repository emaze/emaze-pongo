package net.emaze.pongo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.ColumnMapperFactory
import org.jdbi.v3.core.spi.JdbiPlugin
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.util.*

class JsonJdbiPlugin : JdbiPlugin {
    override fun customizeJdbi(jdbi: Jdbi) {
        jdbi.apply {
            registerArgument(JsonArgumentFactory())
            registerColumnMapper(JsonColumnMapperFactory())
        }
    }
}

data class Json<T>(val value: T) {
    companion object {
        val jackson = jacksonObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

private class JsonArgumentFactory : ArgumentFactory {
    override fun build(type: Type, value: Any?, config: ConfigRegistry) =
        if (value is Json<*>) {
            Optional.of(Argument { position, statement, _ ->
                statement.setObject(position, PGobject().apply {
                    this.type = "jsonb"
                    this.value = Json.jackson.writeValueAsString(value.value)
                })
            })
        } else {
            Optional.empty()
        }
}

private class JsonColumnMapperFactory : ColumnMapperFactory {
    override fun build(type: Type, config: ConfigRegistry): Optional<ColumnMapper<*>> {
        val erasedType = GenericTypes.getErasedType(type)
        if (erasedType != Json::class.java) {
            return Optional.empty()
        }
        val resolvedType = GenericTypes.resolveType(Json::class.java.typeParameters.first(), type)
        return Optional.of(ColumnMapper<Json<*>> { row, columnNumber, _ ->
            val json = row.getString(columnNumber)
            val value = Json.jackson.readValue(json, TypeFactory.rawClass(resolvedType))
            Json(value)
        })
    }
}