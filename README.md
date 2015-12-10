ShUrl
=====
An URL shortener which uses Cassnadra.

Installation (Docker Compose)
-----------------------------
```
docker-compose up -d
```

Installation (Docker)
---------------------

Cassandra
```
docker run -d --name cassandra cassandra
```

ShUrl
```
docker run --name shurl -d --link cassandra:cassandra -p 80:8080 mkroli/shurl
```

Cassandra
---------
ShUrl will use the Keyspace with the name "shurl".
If it doesn't exist yet it will be created with the following settings:
```
CREATE KEYSPACE IF NOT EXISTS shurl WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
}
```
In any non-testing environment the Keyspace should probably be pre-created.
