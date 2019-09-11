package intent

trait Formatter[T] {
  def format(value: T): String
}

object IntFmt extends Formatter[Int] {
  def format(i: Int): String = i.toString
}

object BooleanFmt extends Formatter[Boolean] {
  def format(b: Boolean): String = b.toString
}

object StringFmt extends Formatter[String] {
  def format(s: String): String = s
}

object ThrowableFmt extends Formatter[Throwable] {
  def format(t: Throwable): String = {
    // TODO: stack trace??
    s"${t.getClass.getName}: ${t.getMessage}"
  }
}

class OptionFmt[T] given (innerFmt: Formatter[T]) extends Formatter[Option[T]] {
  def format(value: Option[T]): String = value match {
    case Some(x) => s"Some(${innerFmt.format(x)})"
    case None => "None"
  }
}

trait FormatterGivens {
  given as Formatter[Int] = IntFmt
  given as Formatter[Throwable] = ThrowableFmt
  given as Formatter[Boolean] = BooleanFmt
  given as Formatter[String] = StringFmt
  given [T] as Formatter[Option[T]] given Formatter[T] = new OptionFmt[T]
}
