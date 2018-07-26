---
layout: home

---

# fuuid - Functional UUID [![Build Status](https://travis-ci.com/ChristopherDavenport/fuuid.svg?branch=master)](https://travis-ci.com/ChristopherDavenport/fuuid) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/fuuid_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/fuuid_2.12)

Java UUID's aren't "exceptionally" safe. Operations throw and are not
referentially transparent. We can fix that.

Currently this is the minimal implementation that keeps me content. This aims to offer a baseline
set of tools for interacting with FUUID's. Modules add functionality by bringing in additional
dependencies to support additional interactions.

## Quick Start

To use fuuid in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "fuuid" % "<version>"
```

## Examples

```tut:book
import io.chrisdavenport.fuuid.FUUID
import cats.effect.IO

// We place it in IO because this requires a Sync constraint
val create: IO[FUUID] = FUUID.randomFUUID[IO]

val fromString : Either[IllegalArgumentException, FUUID] = FUUID.fromString("d6faceab-4193-4508-86ca-e1561d38fea6")

val failsReferentiallyTransparently : Either[IllegalArgumentException, FUUID] = FUUID.fromString("Not a UUID")

// For some syntax improvements
import cats.implicits._

// Uses cats Eq
val equalToItself : IO[Boolean] = for {
  fuuid <- FUUID.randomFUUID[IO]
} yield fuuid === fuuid

equalToItself.unsafeRunSync

// Uses cats Order
val laterGreaterThanEarlier : IO[Boolean] = for {
  fuuid1 <- FUUID.randomFUUID[IO]
  fuuid2 <- FUUID.randomFUUID[IO]
} yield fuuid2 > fuuid1

laterGreaterThanEarlier.unsafeRunSync
```

## Circe Integration

To use fuuid directly in you circe Json handling, add to your `build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "fuuid-circe" % "<version>"
```

An example using this integration:

```tut:book
import io.chrisdavenport.fuuid.circe._
import io.circe.syntax._

// Running UnsafeRunSync For Tut Purposes - Do Not Do this in your code please.
val circeFUUID = FUUID.randomFUUID[IO].unsafeRunSync

val circeFUUIDJson = circeFUUID.asJson

val reparsing = circeFUUIDJson.as[FUUID]
```

## Http4s Integration

To use fuuid to define http4s paths, add to your `build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "fuuid-http4s" % "<version>"
```

An example using this integration:

```tut:book
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import org.http4s._, org.http4s.dsl.io._

def getEntityByUuid(id: FUUID): IO[String] = ???

val service: HttpService[IO] =
  HttpService[IO] {
    case GET -> Root / "uuid" / FUUIDVar(id) =>
      for {
        entity <- getEntityByUuid(id)
        response <- Ok(entity)
      } yield response
  }
```

## Doobie Integration

### Postgres

To use fuuid to store UUID's in Postgres, add to your `build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "fuuid-doobie-postgres" % "<version>"
```

An example of a query and an insert using this integration.

```tut:book
import io.chrisdavenport.fuuid.doobie.postgres._
import doobie._, doobie.implicits._

// This is the table we'll use for the insert and update below
def createdTable: Update0 = {
  sql"""
    CREATE TABLE tablewithUUIDid (
      id   UUID NOT NULL
    )""".update
}

def queryBy(fuuid: FUUID): Query0[FUUID] = {
    sql"""SELECT id from tablewithUUIDid where id = ${fuuid}""".query[FUUID]
  }

def insertId(fuuid: FUUID): Update0 = {
  sql"""INSERT into tablewithUUIDid (id) VALUES ($fuuid)""".update
}
```

### H2

To use fuuid to store UUID's in H2, add to your `build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "fuuid-doobie-h2" % "<version>"
```

An example of a query and an insert using this integration.

```tut:book
import io.chrisdavenport.fuuid.doobie.h2._

// This is the table we'll use for the insert and update below
def createdTable: Update0 = {
  sql"""
    CREATE TABLE tablewithUUIDid (
      id   UUID NOT NULL
    )""".update
}

def queryBy(fuuid: FUUID): Query0[FUUID] = {
    sql"""SELECT id from tablewithUUIDid where id = ${fuuid}""".query[FUUID]
  }

def insertId(fuuid: FUUID): Update0 = {
  sql"""INSERT into tablewithUUIDid (id) VALUES ($fuuid)""".update
}