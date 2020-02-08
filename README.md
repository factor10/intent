# Intent

[![Actions Status](https://github.com/factor10/intent/workflows/CI/badge.svg)](https://github.com/factor10/intent/actions)

Intent is an opinionated test framework for [Dotty](https://dotty.epfl.ch). It builds on
the following principles:

* Low ceremony test code
* Uniform test declaration
* Futures and async testing
* Arranging test state
* Fast to run tests

Here is an example on how the tests look:

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

This readme is focused on building and testing Intent, for documentation on
how to use Intent to write tests, see [User documentation](docs/index.md).

## Getting started

Add Intent to your SBT project with the following lines to your `build.sbt`:

```scala
libraryDependencies += "com.factor10" %% "intent" % "0.5.0",
testFrameworks += new TestFramework("intent.sbt.Framework")
```

## Development environment

Intent is an early adopter of Dotty features, which means:

* You need a recent Dotty (>= `0.22.0-RC1`) since Intent use the latest Scala 3 syntax.

* Visual Studio Code seems to be the best supported editor (although not perfect)


## Contributing

See [Contributing to Intent](./CONTRIBUTING.md)

## License

Intent is Open Source and released under Apache 2.0. See `LICENSE` for details.
