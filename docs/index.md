Intent is a test framework for [Dotty](https://dotty.epfl.ch) (which is expected to become Scala 3 in year 2020).

Intent is designed to give you clear and concise tests by focusing on:

* Low cermony test code
* Uniform test declaration
* Futures and async testing
* Arranging test state
* Fast to run tests

## Why a new test framework?

The idea of a new test framework was born out of both the frustration and inspiration
of using existing frameworks. Having written tens of thousands of tests using a variety
of languages and frameworks there are a few challenges that keep surfacing.

**Structure** - when you have thousands of tests it is important that the ceremony to
add a new test is as low as possible. If a test belongs to the same functionality as other
tests, these tests should stay together.

> Intent's goal is that it should be possible to express a simple test in a single line
and still have that line clearly express the intention of the test.

**State**, most tests are not stateless, instead they require setup code in order to get
to the state of interest for a particular test. Setting up this state is often verbose,
heavily imperative and worst of all repeated over and over again.

> Intent's goal is to make the dependency on state obvious for each test, and to allow
state transformation in a hierarchical structure.

**Expectation**, when using fluent and nested matchers we feel that it increases the
cognitive load when writing the tests. You need to know each and every one of the
qualified behaviours until you get to the one actually performing the assert. Having too
simple matchers on the other hand results in more test code, and therefore introduce more
noise to achieve the same expectation.

> Intent's goal is to make assertions easy to find and use while also supporting the
most common expectations.

Intent is built, not to circumvent these challenges, but to put them front and center.
As we believe these three attributes are the most significant for achieving good quality
test they should be the foundation of a test framework.

It deserves to be said that Intent pays homage to in particular two test frameworks that
has inspired us greatly:

* Jasmine, supporting nested tests and the format of the expect / matchers
* ScalaTest, FreeSpec style and the use of test fixtures

Let us see how a test suite looks like for intent:

```scala
class StatefulTest extends TestSuite with State[Cart] :
  "an empty cart" using Cart() to :
    "with two items" using (_.add(CartItem("beach-chair", 2))) to :
      "and another three items" using (_.add(CartItem("sunscreen", 3))) to :
        "contains 5 items" in :
          cart => expect(cart.totalQuantity).toEqual(5)

case class CartItem(artNo: String, qty: Int)
case class Cart(items: Seq[CartItem] = Seq.empty):
  def add(item: CartItem): Cart = copy(items = items :+ item)
  def totalQuantity = items.map(_.qty).sum
```

Want to know more? Continue with [getting started](getting-started.md)
