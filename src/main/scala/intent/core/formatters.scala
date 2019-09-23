package intent.core

trait Formatter[T]:
  def format(value: T): String

object IntFmt extends Formatter[Int]:
  def format(i: Int): String = i.toString

object DoubleFmt extends Formatter[Double]:
  def format(d: Double): String = d.toString // TODO: Consider Locale here, maybe take as given??

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

class OptionFmt[TInner, T <: Option[TInner]] given (innerFmt: Formatter[TInner]) extends Formatter[T]:
  def format(value: T): String = value match
    case Some(x) => s"Some(${innerFmt.format(x)})"
    case None => "None"

trait FormatterGivens:
  given as Formatter[Int] = IntFmt
  given [T <: Throwable] as Formatter[T] = ThrowableFmt[T]
  given as Formatter[Boolean] = BooleanFmt
  given as Formatter[String] = StringFmt
  given as Formatter[Char] = CharFmt
  given as Formatter[Double] = DoubleFmt
  given [TInner, T <: Option[TInner]] as Formatter[T] given Formatter[TInner] = OptionFmt[TInner, T]
