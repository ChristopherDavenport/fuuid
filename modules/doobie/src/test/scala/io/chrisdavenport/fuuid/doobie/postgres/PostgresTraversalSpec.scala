package io.chrisdavenport.fuuid.doobie.postgres

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import io.chrisdavenport.fuuid.doobie.postgres.rig._
import io.chrisdavenport.fuuid.doobie.implicits._
import io.chrisdavenport.fuuid._
import org.specs2._
import cats.effect._

class PostgresTraversalSpec extends mutable.Specification
  with ScalaCheck with FUUIDArbitraries with CatsResourceIO[Transactor[IO]] {
  // sequential

  override def resource: Resource[IO,_root_.doobie.Transactor[IO]] = 
    TransactorResource.create.evalTap{transactor => 
      sql"""
          CREATE TABLE IF NOT EXISTS PostgresTraversalSpec (
            id   UUID NOT NULL
          )
        """.update.run.transact(transactor).void
    }

  def queryBy(fuuid: FUUID): Query0[FUUID] = {
    sql"""SELECT id from PostgresTraversalSpec where id = ${fuuid}""".query[FUUID]
  }

  def insertId(fuuid: FUUID): Update0 = {
    sql"""INSERT into PostgresTraversalSpec (id) VALUES ($fuuid)""".update
  }

  "Doobie Postgres Meta" should {
    "traverse input and then extraction" in withResource{ transactor => prop { fuuid: FUUID =>

      val action = for {
        _ <- insertId(fuuid).run.transact(transactor)
        fuuid <- queryBy(fuuid).unique.transact(transactor)
      } yield fuuid

      action.unsafeRunSync must_=== fuuid
    }}
    "fail on a non-present value" in withResource{ transactor => prop { fuuid: FUUID =>
      queryBy(fuuid)
        .unique
        .transact(transactor)
        .attempt
        .map(_.isLeft)
        .unsafeRunSync must_=== true
    }}
  }

}
