package intent.core

import scala.util.{Try, Success, Failure}
import scala.reflect.ClassTag

trait Formatter[T] with
  def format(value: T): String

object IntFmt extends Formatter[Int] with
  def format(i: Int): String = i.toString

object LongFmt extends Formatter[Long] with
  def format(l: Long): String = l.toString

object DoubleFmt extends Formatter[Double] with
  def format(d: Double): String = d.toString // TODO: Consider Locale here, maybe take as given??

object FloatFmt extends Formatter[Float] with
  def format(f: Float): String = f.toString // TODO: Consider Locale here, maybe take as given??

object BooleanFmt extends Formatter[Boolean] with
  def format(b: Boolean): String = b.toString

object StringFmt extends Formatter[String] with
  def format(str: String): String = s""""$str""""

object CharFmt extends Formatter[Char] with
  def format(ch: Char): String = s"'$ch'"

class ThrowableFmt[T <: Throwable] extends Formatter[T] with
  def format(t: T): String =
    // TODO: stack trace??
    s"${t.getClass.getName}: ${t.getMessage}"

class OptionFmt[TInner, T <: Option[TInner]](using innerFmt: Formatter[TInner]) extends Formatter[T] with
  def format(value: T): String = value match
    case Some(x) => s"Some(${innerFmt.format(x)})"
    case None => "None"

class TryFmt[TInner, T <: Try[TInner]](using innerFmt: Formatter[TInner], throwableFmt: Formatter[Throwable]) extends Formatter[T] with
  def format(value: T): String = value match
    case Success(x) => s"Success(${innerFmt.format(x)})"
    case Failure(t) => s"Failure(${throwableFmt.format(t)})"

trait FormatterGivens with
  given Formatter[Int] = IntFmt
  given Formatter[Long] = LongFmt
  given [T <: Throwable]: Formatter[T] = ThrowableFmt[T]
  given Formatter[Boolean] = BooleanFmt
  given Formatter[String] = StringFmt
  given Formatter[Char] = CharFmt
  given Formatter[Double] = DoubleFmt
  given Formatter[Float] = FloatFmt

  given optFmt[TInner, T <: Option[TInner]](using Formatter[TInner]): Formatter[T] = OptionFmt[TInner, T]
  given tryFmt[TInner, T <: Try[TInner]](using Formatter[TInner]): Formatter[T] = TryFmt[TInner, T]
