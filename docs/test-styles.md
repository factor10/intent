Tests in Intent can be written use a few different styles:

* Stateless
* Stateful
* Async-stateful
* Table-driven

## Stateless test

A stateless test suite extends the `Statesless` suite. _Contexts_ in this style
serve no other purpose than grouping tests into logical units.

Consider the following example:

```scala
import intent.{Stateless, TestSuite}

class CalculatorTest extends TestSuite with Stateless with
  "A calculator" :
    "can add" :
      "plain numbers" in expect(Calculator().add(2, 4)).toEqual(6)
      "complex numbers" in:
        val a = Complex(2, 3)
        val b = Complex(3, 4)
        expect(Calculator().add(a, b)).toEqual(Complex(5, 7))
    "can multiply" :
      "plain numbers" in expect(Calculator().multiply(2, 4)).toEqual(8)
```

Here, contexts serve to group tests based on the arithmetical operation used.

## Stateful test

Not all tests can be implemented without setting the scene, still many test frameworks only focus
on a expressive way to assert expectations. For `Intent` state management is front and center.

Lets go straight to the code:

```scala
class StatefulTest extends TestSuite with State[Cart] with
  "an empty cart" using Cart() to :
    "with two items" using (_.add(CartItem("beach-chair", 2))) to :
      "and another three items" using (_.add(CartItem("sunscreen", 3))) to :
        "contains 5 items" in :
          cart => expect(cart.totalQuantity).toEqual(5)

case class CartItem(artNo: String, qty: Int)
case class Cart(items: Seq[CartItem] = Seq.empty) with
  def add(item: CartItem): Cart = copy(items = items :+ item)
  def totalQuantity = items.map(_.qty).sum
```

A test suite that needs state must implement `State[T]`, where `T` is the type carrying
the state you need. There are _no requirements_ on the type `T` or its signature, you are free
to use whatever type you want. We prefer to use `case class` as they are immutable, but any
type will do.

The _root state_ gets created in the `"root context"` and is passed downstream to any child context.
Each child context has the possiblity to _transform_ the state before it is passed to the actual
test as a parameter.

```scala
"check the stuff" in :
  state => expect(state.stuff).toHaveLength(2)
```

A suite must _either_ be stateless or stateful. There is no support in writing a test that does not
take a state when you derive from `State` and vice versa. While not the same, it resembles how Scala
separate a `class` and an `object`.

There are a few conventions or recommendations on how to use state:

* Put the state implementation below the test
* Prefer to call methods on the state object over doing it in the test itself
* Keep state focused and create a new suite ands state class when needed (cost is low)

## Asynchronous stateful test

Intent supports stateful tests where the state is produced asynchronously. An example:

```scala
class AsyncStatefulTest extends TestSuite with AsyncState[AsyncStatefulState] with
  "an empty cart" using Cart() to :
    "with two items" usingAsync (_.add(CartItem("beach-chair", 2))) to :
      "and another three items" usingAsync (_.add(CartItem("sunscreen", 3))) to :
        "calculates total price" in :
          cart => expect(cart.totalPrice).toEqual(275.0d)

case class CartItem(artNo: String, qty: Int)
case class PricedCartItem(item: CartItem, price: Double) with
  def totalPrice = item.qty * price
case class Cart(items: Seq[PricedCartItem] = Seq.empty) with
  def lookupPrice(artNo: String): Future[Double] = ... // e.g. using a test fake here
  def add(item: CartItem): Future[Cart] =
    lookupPrice(item.artNo).map:
      price =>
        pricedItem = PricedCartItem(item, price)
        copy(items = items :+ pricedItem)

  def totalPrice = items.map(_.totalPrice).sum
```

Some notes here:
* The initial state (`Cart()`) is not produced asynchronously (but could have been).
* Asynchronous state production uses `usingAsync`.
* The test itself is not asynchronous.

The last point is worth expanding on. A test in an async-stateful test suite can be synchronous.
Similarly, a test in a regular (non-async) stateful test suite can be asynchronous. Whether to choose
`State` or `AsyncState` for a test suite depends on how the _state_ is produced.

The [matchers](./matchers.md) documentation describes mechanisms for working with asynchronous
tests.

## Table-driven test

Table-driven tests allow for a declarative approach to writing tests, and is useful to
test many different variations of some feature with as little boilerplate as possible.

For example, consider the following test suite that tests a [Fibonacci](https://en.wikipedia.org/wiki/Fibonacci_number) function:

```scala
class FibonacciTest extends TestSuite with State[TableState] with
  "The Fibonacci function" usingTable (examples) to :
    "works" in :
      example =>
        expect(F(example.n)).toEqual(example.expected)

  def examples = Seq(
    FibonacciExample(0, 0),
    FibonacciExample(1, 1),
    FibonacciExample(2, 1),
    FibonacciExample(3, 2),
    FibonacciExample(12, 144)
  )

  def F(n: Int): Int = ... // implemented elsewhere

case class FibonacciExample(n: Int, expected: Int) with
  override def toString = s"Fn($n) = $expected"
```

Intent will run the test code for each example returned by the `examples`
method. This makes it easy to adjust examples and add new ones.

> The exact syntax for table-driven tests may change in a future release. The
  current syntax results in test output that is a bit noisy.
