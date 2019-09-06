package intent

trait Eq[T] {
  def areEqual(a: T, b: T): Boolean
}

object IntEq extends Eq[Int] {
  def areEqual(a: Int, b: Int): Boolean = a == b
}

object BooleanEq extends Eq[Boolean] {
  def areEqual(a: Boolean, b: Boolean): Boolean = a == b
}


class OptionEq[T] given (innerEq: Eq[T]) extends Eq[Option[T]] {
  def areEqual(a: Option[T], b: Option[T]): Boolean = {
    (a, b) match {
      case (Some(aa), Some(bb)) => innerEq.areEqual(aa, bb)
      case (None, None) => true
      case _ => false
    }
  }
}

trait EqGivens {
  given as Eq[Int] = IntEq
  given as Eq[Boolean] = BooleanEq
  given [T] as Eq[Option[T]] given Eq[T] = new OptionEq[T]
}