package net.emaze.pongo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.config.JdbiConfig
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.ColumnMapperFactory
import org.jdbi.v3.core.spi.JdbiPlugin
import java.lang.reflect.Type
import java.util.Optional

class JsonConfig(
    var argumentFactory: ArgumentFactory = DefaultJsonArgumentFactory()
) : JdbiConfig<JsonConfig> {

    override fun createCopy() = JsonConfig(argumentFactory)
}

class JsonJdbiPlugin : JdbiPlugin {
    override fun customizeJdbi(jdbi: Jdbi) {
        jdbi.apply {
            registerArgument(JsonArgumentFactory())
            registerColumnMapper(JsonColumnMapperFactory())
        }
    }
}

data class Json<T>(@JsonValue val value: T) {
    companion object {
        val jackson = ObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

private class JsonArgumentFactory : ArgumentFactory {
    override fun build(type: Type, value: Any?, config: ConfigRegistry): Optional<Argument> =
        if (value is Json<*>) {
            val json = Json.jackson.writeValueAsString(value.value)
            config.get(JsonConfig::class.java)
                .argumentFactory
                .build(type, json, config)
        } else {
            Optional.empty()
        }
}

private class DefaultJsonArgumentFactory : ArgumentFactory {
    override fun build(type: Type, value: Any?, config: ConfigRegistry) =
        Optional.of(Argument { position, statement, _ ->
            statement.setString(position, value as String)
        })
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