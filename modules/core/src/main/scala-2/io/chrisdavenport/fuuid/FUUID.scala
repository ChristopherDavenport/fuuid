package io.chrisdavenport.fuuid

import cats._
import cats.data._
import cats.implicits._
import cats.effect.Sync
import java.util.UUID

import scala.reflect.macros.blackbox

final class FUUID private (private val uuid: UUID) {

  // Direct show method so people do not use toString
  def show: String = uuid.show
  // -1 less than, 0 equal to, 1 greater than
  def compare(that: FUUID): Int = this.uuid.compareTo(that.uuid)

  // Returns 0 when equal
  def eqv(that: FUUID): Boolean = compare(that) == 0

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: FUUID => eqv(that)
    case _ => false
  }
  override def hashCode: Int = uuid.hashCode
  override def toString: String = uuid.toString

}

object FUUID {
  implicit val instancesFUUID: Hash[FUUID] with Order[FUUID] with Show[FUUID] =
    new Hash[FUUID] with Order[FUUID] with Show[FUUID] {
      override def show(t: FUUID): String = t.show
      override def eqv(x: FUUID, y: FUUID): Boolean = x.eqv(y)
      override def hash(x: FUUID): Int = x.hashCode
      override def compare(x: FUUID, y: FUUID): Int = x.compare(y)
    }

  def fromString(s: String): Either[Throwable, FUUID] =
    Either.catchNonFatal(new FUUID(UUID.fromString(s)))

  /**
   * Attempt to parse a UUID from a `String` accumulating errors in a `cats.data.NonEmptyList` on
   * failure.
   *
   * This is useful when you wish to parse more than one UUID at a time and return all the failures,
   * not just the first one. For example,
   *
   * {{{
   * scala> import cats._, cats.data._, cats.implicits._, io.chrisdavenport.fuuid._
   * import cats._
   * import cats.data._
   * import cats.implicits._
   * import io.chrisdavenport.fuuid._
   *
   * scala> (FUUID.fromStringVNel("a"), FUUID.fromStringVNel("b")).mapN((a, b) => println(s"\$a \$b"))
   * res0: cats.data.ValidatedNel[Throwable,Unit] = Invalid(NonEmptyList(java.lang.IllegalArgumentException: Invalid UUID string: a, java.lang.IllegalArgumentException: Invalid UUID string: b))
   * }}}
   */
  def fromStringVNel(s: String): ValidatedNel[Throwable, FUUID] =
    fromStringAccumulating[ValidatedNel[Throwable, *], NonEmptyList](s)

  /**
   * Attempt to parse a UUID from a `String` accumulating errors in a `cats.data.NonEmptyChain` on
   * failure.
   *
   * This is useful when you wish to parse more than one UUID at a time and return all the failures,
   * not just the first one. For example,
   *
   * {{{
   * scala> import cats._, cats.data._, cats.implicits._, io.chrisdavenport.fuuid._
   * import cats._
   * import cats.data._
   * import cats.implicits._
   * import io.chrisdavenport.fuuid._
   *
   * scala> (FUUID.fromStringVNec("a"), FUUID.fromStringVNec("b")).mapN((a, b) => println(s"\$a \$b"))
   * res0: cats.data.ValidatedNec[Throwable,Unit] = Invalid(NonEmptyChain(java.lang.IllegalArgumentException: Invalid UUID string: a, java.lang.IllegalArgumentException: Invalid UUID string: b))
   * }}}
   */
  def fromStringVNec(s: String): ValidatedNec[Throwable, FUUID] =
    fromStringAccumulating[ValidatedNec[Throwable, *], NonEmptyChain](s)

  def fromStringOpt(s: String): Option[FUUID] =
    fromString(s).toOption

  def fromStringF[F[_]](s: String)(implicit AE: ApplicativeError[F, Throwable]): F[FUUID] =
    fromString(s).fold(AE.raiseError, AE.pure)

  /**
   * Like [[#fromStringF]] but using an `Applicative` of `Throwable`.
   *
   * Generally this will be used with something like `cats.data.Validated` to accumulate errors when
   * parsing more than one [[FUUID]].
   *
   * See [[#fromStringVNel]] and [[#fromStringVNec]] for examples.
   */
  def fromStringAccumulating[F[_], E[_]](
      s: String
  )(implicit A: Applicative[E], AE: ApplicativeError[F, E[Throwable]]): F[FUUID] =
    fromString(s).fold(e => AE.raiseError(A.pure(e)), AE.pure)

  def fromUUID(uuid: UUID): FUUID = new FUUID(uuid)

  def randomFUUID[F[_]: Sync]: F[FUUID] = Sync[F].delay(
    new FUUID(UUID.randomUUID)
  )

  def fuuid(s: String): FUUID = macro Macros.fuuidLiteral

  private[FUUID] class Macros(val c: blackbox.Context) {
    import c.universe._
    def fuuidLiteral(s: c.Expr[String]): c.Expr[FUUID] =
      s.tree match {
        case Literal(Constant(s: String)) =>
          fromString(s)
            .fold(
              e => c.abort(c.enclosingPosition, e.getMessage.replace("UUID", "FUUID")),
              _ => c.Expr(q"""
                @SuppressWarnings(Array("org.wartremover.warts.Throw"))
                val fuuid = _root_.io.chrisdavenport.fuuid.FUUID.fromString($s).fold(throw _, _root_.scala.Predef.identity)
                fuuid
              """)
            )
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"This method uses a macro to verify that a FUUID literal is valid. Use FUUID.fromString if you have a dynamic value you want to parse as an FUUID."
          )
      }
  }

  /**
   * Creates a new name-based UUIDv5. NOTE: Not implemented for Scala.js!
   */
  def nameBased[F[_]](namespace: FUUID, name: String)(implicit
      AE: ApplicativeError[F, Throwable]
  ): F[FUUID] =
    PlatformSpecificMethods.nameBased[F](namespace, name, AE)

  /**
   * A Home For functions we don't trust Hopefully making it very clear that this code needs to be
   * dealt with carefully.
   *
   * Likely necessary for some interop
   *
   * Please do not import directly but prefer `FUUID.Unsafe.xxx`
   */
  object Unsafe {
    def toUUID(fuuid: FUUID): UUID = fuuid.uuid
    def withUUID[A](fuuid: FUUID)(f: UUID => A): A = f(fuuid.uuid)
  }

  /**
   * The Nil UUID.
   *
   * This is a constant UUID for which all bits are 0.
   *
   * @see
   *   [[https://tools.ietf.org/html/rfc4122#section-4.1.7]]
   */
  val NilUUID: FUUID =
    FUUID.fromUUID(new UUID(0L, 0L))
}
