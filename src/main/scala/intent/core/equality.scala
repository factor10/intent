package intent.core

import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

trait Eq[T] with
  def areEqual(a: T, b: T): Boolean

trait FloatingPointPrecision[T] with
  def numberOfDecimals: Int

object DefaultDoubleFloatingPointPrecision extends FloatingPointPrecision[Double] with
  def numberOfDecimals: Int = 12

object DefaultFloatFloatingPointPrecision extends FloatingPointPrecision[Float] with
  def numberOfDecimals: Int = 6

private def compareFPs[T : Numeric](a: T, b: T)(using prec: FloatingPointPrecision[T]): Boolean =
  if a == b then
    return true
  val num = summon[Numeric[T]]
  val mul = math.pow(10, prec.numberOfDecimals.asInstanceOf[Double])
  val am = math.floor(num.toDouble(a) * mul)
  val bm = math.floor(num.toDouble(b) * mul)
  am == bm

object IntEq extends Eq[Int] with
  def areEqual(a: Int, b: Int): Boolean = a == b

object LongEq extends Eq[Long] with
  def areEqual(a: Long, b: Long): Boolean = a == b

class DoubleEq(using prec: FloatingPointPrecision[Double]) extends Eq[Double] with
  def areEqual(a: Double, b: Double): Boolean = compareFPs(a, b)

class FloatEq(using prec: FloatingPointPrecision[Float]) extends Eq[Float] with
  def areEqual(a: Float, b: Float): Boolean = compareFPs(a, b)

object BooleanEq extends Eq[Boolean] with
  def areEqual(a: Boolean, b: Boolean): Boolean = a == b

object StringEq extends Eq[String] with
  def areEqual(a: String, b: String): Boolean = a == b

object CharEq extends Eq[Char] with
  def areEqual(a: Char, b: Char): Boolean = a == b

class ThrowableEq[T <: Throwable] extends Eq[T] with
  def areEqual(a: T, b: T): Boolean = a == b

object AnyEq extends Eq[Any] with
  def areEqual(a: Any, b: Any): Boolean = a == b

object NothingEq extends Eq[Nothing] with
  def areEqual(a: Nothing, b: Nothing): Boolean = a == b

class OptionEq[TInner, T <: Option[TInner]](using innerEq: Eq[TInner]) extends Eq[T] with
  def areEqual(a: T, b: T): Boolean =
    (a, b) match
      case (Some(aa), Some(bb)) => innerEq.areEqual(aa, bb)
      case (None, None) => true
      case _ => false

class TryEq[TInner, T <: Try[TInner]](using innerEq: Eq[TInner], throwableEq: Eq[Throwable]) extends Eq[T] with
  def areEqual(a: T, b: T): Boolean =
    (a, b) match
      case (Success(aa), Success(bb)) => innerEq.areEqual(aa, bb)
      case (Failure(ta), Failure(tb)) => throwableEq.areEqual(ta, tb)
      case _ => false

trait EqGivens with

  given FloatingPointPrecision[Double] = DefaultDoubleFloatingPointPrecision
  given FloatingPointPrecision[Float] = DefaultFloatFloatingPointPrecision

  given Eq[Int] = IntEq
  given Eq[Long] = LongEq
  given Eq[Boolean] = BooleanEq
  given Eq[String] = StringEq
  given Eq[Char] = CharEq
  given (using FloatingPointPrecision[Double]): Eq[Double] = DoubleEq()
  given (using FloatingPointPrecision[Float]): Eq[Float] = FloatEq()
  given throwableEq[T <: Throwable]: Eq[T] = ThrowableEq[T]
  given optEq[TInner, T <: Option[TInner]](using Eq[TInner]): Eq[T] = OptionEq[TInner, T]
  given tryEq[TInner, T <: Try[TInner]](using Eq[TInner]): Eq[T] = TryEq[TInner, T]
