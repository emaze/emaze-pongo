# Emaze Pongo

Emaze Pongo allows to manage and query a repository of entities stored as JSON objects on a relational database in an easy way, without the aid of a complex ORM.
It is designed to support the Domain Driven Design and CQRS programming paradigms.

The library is written in Kotlin, but it is thought to be Java-friendly, and currently supports PostgreSQL, MySQL and MariaDB.

## Requirements

* JDK 1.8
* PostgreSQL >= 9.5
* MySQL >= 5.7
* MariaDB >= 10.2.3

## Dependencies

* PostgreSQL/MySQL/MariaDB JDBC driver
* Jackson
* Kotlin runtime

## Features

* Automatic save or update (using auto-generated surrogate IDs)
* Conflicts detection (optimistic locking)
* Dynamic creation of repository instances
* Auto-creation of tables (optional)

## Usage

### Connect to the database

Pongo is based on [JDBI](http://jdbi.org), so it must be initialized with a proper driver, i.e.:
```java
final Jdbi jdbi = Jdbi.create("jdbc:postgresql://localhost/pongo", "root", "password");
```

### Defining entities

An entity is simply an object serializable with Jackson derived from 
[Identifiable](https://github.com/emaze/emaze-pongo/blob/master/src/main/kotlin/Identifiable.kt), 
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

A repository is defined by an interface extending [EntityRepository](https://github.com/emaze/emaze-pongo/blob/master/src/main/kotlin/EntityRepository.kt) bound with the managed entity type. 

```java
public interface UserRepository extends EntityRepository<User> {
}
```

It can be instanced using an `EntityRepositoryFactory` in the following way:
 
```java
final EntityRepositoryFactory factory = new PostgreSQLEntityRepositoryFactory(jdbi);
final UserRepository users = Pongo.create(factory, User.class, UserRepository.class);
```

The effective `PostgreSQLEntityRepository` has methods useful to create the related table:
```java
final EntityRepository<User> repository = factory.create(User.class).createTable();
final UserRepository users = Pongo.lift(repository, UserRepository.class);
```

The additional repository methods can have a default implementation...

```java
public interface UserRepository extends EntityRepository<User> {

   default Optional<User> findByName(String name) {
       return findFirst("this->>'name' = ?", name);
   }
}
```

...or can be annotated with `@Where`:

```java
public interface UserRepository extends EntityRepository<User> {
    
   @Where("this->>'name' = ?")
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

Currently a query must be a valid "where" predicate, assuming that the JSON document is available in the column named `this`.

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

### Integration with Spring Framework

In order to synchronize the operations with the Spring transaction manager you can use the `TransactionAwareDataSourceProxy` decorator, i.e.:
```java
@Configuration
public class MyConfig {
    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(new TransactionAwareDataSourceProxy(dataSource));
    }
}
```

## Database scripts

Is it possible to create the tables manually as follows.

### PostgreSQL
```sql
CREATE TABLE user (
  id      BIGSERIAL PRIMARY KEY,
  version BIGINT NOT NULL,
  this    JSONB NOT NULL
);
CREATE UNIQUE INDEX user_email_ukey ON users ((this->>'email'));
```

### MySQL
```sql
CREATE TABLE user (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  version BIGINT NOT NULL,
  this    JSON NOT NULL,
  email   VARCHAR(255) GENERATED ALWAYS AS (this->'$.email'),
  UNIQUE INDEX user_email_ukey (email)
);
```

### MariaDB
```sql
CREATE TABLE user (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  version BIGINT NOT NULL,
  this    JSON NOT NULL,
  email   VARCHAR(255) GENERATED ALWAYS AS (JSON_EXTRACT(this, '$.email')) PERSISTENT UNIQUE
);
```

## Development

Build with Maven >= 3 and Docker:
```bash
mvn -Pdocker package
```

## Contribute!

Emaze Pongo is under active development, your contribution is welcome!

## Credits

Emaze Pongo is inspired to the Vaughn Vernon article [The Ideal Domain-Driven Design Aggregate Store?](https://kalele.io/blog-posts/the-ideal-domain-driven-design-aggregate-store/).

## License

Attribution-ShareAlike 4.0 International
