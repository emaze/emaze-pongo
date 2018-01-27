# Emaze Pongo

Emaze Pongo is a small java library inspired to the Vaughn Vernon article [The Ideal Domain-Driven Design Aggregate Store?](https://vaughnvernon.co/?p=942).

Essentially it allows to manage and query easily a repository of entities stored as JSON objects on PostgreSQL, without the aid of a complex ORM.

Emaze Pongo is written in Kotlin, but it is thought to be Java-friendly.

## Requirements

* JDK 1.8
* PostgreSQL >= 9.5

## Dependencies

* PostgreSQL JDBC driver
* Jackson
* Kotlin runtime

## Features

* Automatic save or update (using an auto-generated surrogate id)
* Detection of optimistic locking conflicts
* Auto-creation of tables and indexes (optional)

## Usage

Check the [API](https://github.com/emaze/emaze-pongo/blob/master/src/main/kotlin/api.kt).

### Defining entities

An entity is simply an object serializable with Jackson derived from `Identifiable`, 
that maintains its surrogate id and its version.

```java
public class User extends Identifiable {

    public final String name;
    /* ... */
    
    @JsonCreator
    public User(@JsonProperty("name") String name /* ... */) {
        this.name = name;
    }
}
```

### Defining repositories

A repository is defined by an interface extending `EntityRepository` bound with the managed entity type. 

```java
public interface UserRepository extends EntityRepository<User> {
    /* ... */
}
```

It can be instanced using an `EntityRepositoryFactory` in the following way:
 
```java
final DataSource dataSource = ...;
final ObjectMapper mapper = ...;
final EntityRepositoryFactory factory = new PostgreSQLEntityRepositoryFactory(dataSource, mapper);
final UserRepository users = Pongo.create(factory, User.class, UserRepository.class);
```

The effective `PostgreSQLEntityRepository` has methods useful to create the related table:
```java
final EntityRepository<User> repository = factory.create(User.class).createTable();
final UserRepository users = Pongo.lift(repository, UserRepository.class);
```

The repository methods can have a default implementation:

```java
public interface UserRepository extends EntityRepository<User> {

   default Optional<User> findByName(String name) {
       return findFirst("where data->>'name' = ?", name);
   }
}
```

Or can be annotated with `@Query`:

```java
public interface UserRepository extends EntityRepository<User> {
    
   @Query("where data->>'name' = ?")
   Optional<User> findByName(String name);
}
```

The semantic of the annotated methods depends on the return type.
The following table summarize the behaviour assuming that `T` is the entity type. 

| Return type | Additional annotations | Behaviour |
| --- | --- | --- |
| `List<T>` | - | Returns all entities matching the criteria as a list |
| `Set<T>` | - | Returns all entities matching the criteria as a `LinkedHashSet` (so the original order is maintained) |
| `T` | - | Returns the first entity matching the criteria or throw `NoSuchElementException` if it's not found |
| `T` | `@Nullable` | Returns the first entity matching the criteria or null |
| `Optional<T>` | - | Returns the first entity matching the criteria or empty

### Executing queries

Currently a query should be a valid `PostgreSQL` query without the prefix `select * from <entity_table>`, 
that is prepended by the library, assuming that the JSON document is available in the column named `data`.

Obviously the `findFirst...` methods doesn't need of the clause `limit 1`.

### A note about immutability

In order to update an entity modelled as an immutable object, you must set the metadata of the original object into the new one.

```java
public class User extends Identifiable {
    /* ... */
    
    public User doSomething() {
        final User user = new User(name.upperCase());
        user.setMetadata(this.getMetadata());
        return user;
    }
    
    public User equivalentDoSomething() {
        return Pongo.attach(new User(name.upperCase()), this);
    }
}

// then, in some service...
final User oldUser = users.findByName("jack");
final User newUser = oldUser.doSomething();
users.save(newUser);
```

## Development

Build with Maven >= 3 and Docker:
```bash
mvn package
```

## Further developments

* Provide a simpler way to query the aggregates using an SQL query translated to a JSON-based query
* Integrate it with the Spring transaction manager in order to support the automatic updates of the changed entities
* Introduce an optional entities cache at transaction level

## Contribute!

Emaze Pongo is under active development, your contribution is welcome!

## License

Attribution-ShareAlike 4.0 International