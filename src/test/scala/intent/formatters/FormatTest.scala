package intent.formatters

import intent._
import scala.util.{Success, Failure, Try}

class FormatTest extends TestSuite with Stateless with
  "Formatting":
    "supports Long" in expect(format(42L)).toEqual("42")
    "surrounds Char in single quotes" in expect(format('a')).toEqual("'a'")
    "surrounds String in double quotes" in expect(format("a")).toEqual("\"a\"")
    "supports Option-Some" in expect(format(Some(42))).toEqual("Some(42)")
    "supports Option-None" in expect(format[Option[String]](None)).toEqual("None")
    "supports Try-Success" in expect(format(Success(42))).toEqual("Success(42)")
    "supports Try-Failure" in expect(format[Try[Int]](Failure(RuntimeException("oops")))).toEqual("Failure(java.lang.RuntimeException: oops)")
    "supports Try as Try" in:
      val t: Try[Int] = Success(42)
      expect(format(t)).toEqual("Success(42)")
    "supports recursive Option" in expect(format(Some("test"))).toEqual("Some(\"test\")")
    "supports Throwable" in:
      val t = RuntimeException("oops")
      expect(format(t)).toEqual("java.lang.RuntimeException: oops")

  def format[T](x: T)(using fmt: core.Formatter[T]): String =
    fmt.format(x)
