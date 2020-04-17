package intent.core

import scala.util.{Try, Success, Failure}
import scala.reflect.ClassTag

trait Formatter[T]:
  def format(value: T): String

object IntFmt extends Formatter[Int]:
  def format(i: Int): String = i.toString

object LongFmt extends Formatter[Long]:
  def format(l: Long): String = l.toString

object DoubleFmt extends Formatter[Double]:
  def format(d: Double): String = d.toString // TODO: Consider Locale here, maybe take as given??

object FloatFmt extends Formatter[Float]:
  def format(f: Float): String = f.toString // TODO: Consider Locale here, maybe take as given??

object BooleanFmt extends Formatter[Boolean]:
  def format(b: Boolean): String = b.toString

object StringFmt extends Formatter[String]:
  def format(str: String): String = s""""$str""""

object CharFmt extends Formatter[Char]:
  def format(ch: Char): String = s"'$ch'"

class ThrowableFmt[T <: Throwable] extends Formatter[T]:
  def format(t: T): String =
    // TODO: stack trace??
    s"${t.getClass.getName}: ${t.getMessage}"

class OptionFmt[TInner, T <: Option[TInner]](using innerFmt: Formatter[TInner]) extends Formatter[T]:
  def format(value: T): String = value match
    case Some(x) => s"Some(${innerFmt.format(x)})"
    case None => "None"

class TryFmt[TInner, T <: Try[TInner]](using innerFmt: Formatter[TInner], throwableFmt: Formatter[Throwable]) extends Formatter[T]:
  def format(value: T): String = value match
    case Success(x) => s"Success(${innerFmt.format(x)})"
    case Failure(t) => s"Failure(${throwableFmt.format(t)})"

trait FormatterGivens:
  given Formatter[Int] = IntFmt
  given Formatter[Long] = LongFmt
  given [T <: Throwable] as Formatter[T] = ThrowableFmt[T]
  given Formatter[Boolean] = BooleanFmt
  given Formatter[String] = StringFmt
  given Formatter[Char] = CharFmt
  given Formatter[Double] = DoubleFmt
  given Formatter[Float] = FloatFmt

  given optFmt[TInner, T <: Option[TInner]](using Formatter[TInner]) as Formatter[T] = OptionFmt[TInner, T]
  given tryFmt[TInner, T <: Try[TInner]](using Formatter[TInner]) as Formatter[T] = TryFmt[TInner, T]
