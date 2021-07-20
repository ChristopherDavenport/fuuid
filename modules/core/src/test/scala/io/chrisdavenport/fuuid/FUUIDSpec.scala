package io.chrisdavenport.fuuid

import cats.effect.IO
import java.util.UUID
import munit.CatsEffectSuite

class FUUIDSpec extends CatsEffectSuite {
  test("FUUID.fromString Fail when parsing an invalid string") {
    assertEquals(FUUID.fromString("What up yo!").isLeft, true)
  }
  test("FUUID.fromString Fail when parsing invalid uuid") {
    assert(FUUID.fromString("2630147c-4a18-4866-9bbd-b2d89acef83z").isLeft)
  }
  test("FUUID.fromString Succeed when parsing a valid UUID") {
    FUUID
      .randomFUUID[IO]
      .map(_.toString)
      .map(FUUID.fromString)
      .map(_.isRight)
      .assertEquals(true)
  }

  test("FUUID.hashCode have same hashcode as uuid") {
    val baseString = "00000000-075b-cd15-0000-0000075bcd15"
    // Easy in for testing
    val uuid = UUID.fromString(baseString)
    val fuuid = FUUID.fromUUID(uuid)
    assertEquals(fuuid.hashCode, uuid.hashCode)
  }

  test("FUUID.equalsbe equal for the same FUUID") {
    val baseString = "00000000-075b-cd15-0000-0000075bcd15"
    // Easy in for testing
    val uuid = UUID.fromString(baseString)
    val fuuid = FUUID.fromUUID(uuid)
    assertEquals(fuuid.equals(fuuid), true)
  }

  test("FUUID.eqv be equal for the same FUUID") {
    val baseString = "00000000-075b-cd15-0000-0000075bcd15"
    // Easy in for testing
    val uuid = UUID.fromString(baseString)
    val fuuid = FUUID.fromUUID(uuid)
    assertEquals(fuuid.eqv(fuuid), true)
  }

  test("FUUID.fuuid compile for a literal") {
    FUUID.fuuid("00000000-075b-cd15-0000-0000075bcd15")
  }

  test("FUUID.fuuid fail at compile-time when passed an invalid uuid") {
    assert(
      compileErrors("FUUID.fuuid(\"2630147c-4a18-4866-9bbd-b2d89acef83z\")")
        .contains("error: Error at index 11 in:")
    )
  }
}
