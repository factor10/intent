package intent.core

trait Eq[T]:
  def areEqual(a: T, b: T): Boolean

trait FloatingPointPrecision[T]:
  def numberOfDecimals: Int

object DefaultDoubleFloatingPointPrecision extends FloatingPointPrecision[Double]:
  def numberOfDecimals: Int = 12

object DefaultFloatFloatingPointPrecision extends FloatingPointPrecision[Float]:
  def numberOfDecimals: Int = 6

object IntEq extends Eq[Int]:
  def areEqual(a: Int, b: Int): Boolean = a == b

class DoubleEq given (prec: FloatingPointPrecision[Double]) extends Eq[Double]:
  def areEqual(a: Double, b: Double): Boolean =
    if a == b then
      return true
    val mul = math.pow(10, prec.numberOfDecimals.asInstanceOf[Double])
    val am = math.floor(a * mul)
    val bm = math.floor(b * mul)
    am == bm

class FloatEq given (prec: FloatingPointPrecision[Float]) extends Eq[Float]:
  def areEqual(a: Float, b: Float): Boolean =
    if a == b then
      return true
    val mul = math.pow(10, prec.numberOfDecimals.asInstanceOf[Double])
    val am = math.floor(a * mul)
    val bm = math.floor(b * mul)
    am == bm

object BooleanEq extends Eq[Boolean]:
  def areEqual(a: Boolean, b: Boolean): Boolean = a == b

object StringEq extends Eq[String]:
  def areEqual(a: String, b: String): Boolean = a == b

object CharEq extends Eq[Char]:
  def areEqual(a: Char, b: Char): Boolean = a == b

class OptionEq[TInner, T <: Option[TInner]] given (innerEq: Eq[TInner]) extends Eq[T]:
  def areEqual(a: T, b: T): Boolean =
    (a, b) match
      case (Some(aa), Some(bb)) => innerEq.areEqual(aa, bb)
      case (None, None) => true
      case _ => false

trait EqGivens:

  given as FloatingPointPrecision[Double] = DefaultDoubleFloatingPointPrecision
  given as FloatingPointPrecision[Float] = DefaultFloatFloatingPointPrecision

  given as Eq[Int] = IntEq
  given as Eq[Boolean] = BooleanEq
  given as Eq[String] = StringEq
  given as Eq[Char] = CharEq
  given as Eq[Double] given FloatingPointPrecision[Double] = DoubleEq()
  given as Eq[Float] given FloatingPointPrecision[Float] = FloatEq()
  given [TInner, T <: Option[TInner]] as Eq[T] given Eq[TInner] = OptionEq[TInner, T]
