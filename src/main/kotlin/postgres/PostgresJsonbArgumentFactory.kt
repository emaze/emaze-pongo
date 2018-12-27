package postgres

import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.util.*

internal class PostgresJsonbArgumentFactory : ArgumentFactory {
    override fun build(type: Type, value: Any?, config: ConfigRegistry) =
        Optional.of(Argument { position, statement, _ ->
            statement.setObject(position, PGobject().apply {
                this.type = "jsonb"
                this.value = value as String
            })
        })
}