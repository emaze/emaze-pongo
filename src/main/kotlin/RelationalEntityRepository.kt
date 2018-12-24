package net.emaze.pongo

interface RelationalEntityRepository<T : Identifiable> : EntityRepository<T> {

    val tableName: String

    /**
     * Create the table if it doesn't exist.
     */
    fun createTable(): RelationalEntityRepository<T>
}

abstract class BaseRelationalEntityRepository<T : Identifiable>(
    final override val entityClass: Class<T>
) : RelationalEntityRepository<T> {

    override val tableName: String = entityClass.simpleName
        .decapitalize()
        .replace("[A-Z]".toRegex(), { match -> "_${match.value.toLowerCase()}" })
}