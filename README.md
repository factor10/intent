# Intent

[![Actions Status](https://github.com/factor10/intent/workflows/CI/badge.svg)](https://github.com/factor10/intent/actions)

Intent is an opinionated test framework for [Dotty](https://dotty.epfl.ch). It builds on
the following principles:

* Low cermony test code
* Uniform test declaration
* Futures and async testing
* Arranging test state
* Fast to run tests

Here is an example on how the tests look:

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

This readme is focused on building and testing Intent, for documentation on
how to use Intent to write tests, see [User documentation](docs/index.md).


## Development environment

Intent is an early adopter of Dotty features, which means:

* You need a recent Dotty (>= `0.18.1-RC1`) since Intent use the new Scala 3 syntax
 and significant whitespace.

* Visual Studio Code seems to be the best supported editor (although not perfect)


## Testing

Intent is self-hosted and runs all tests using this version of Intent. This is
great and it feels really good running your tests with your _"system under test"_.
However there are a few gotchas related to the same:

* When adding logging, remember that the logs will most likely be printed multiple
  times. Both for the runner invoked by SBT and the runner invoked by your test.

* Tests that are not supposed to be discovered by SBT should not extend `TestSuite`.
  Instead they should be programatically loaded by the running test (e.g. the tests
  under `intent.testdata`).


## Contributing

Until we are satisified with design of Intent and how tests are structured, we are
not accepting any contributions.


## License

Intent is Open Source and released under Apache 2.0. See `LICENSE` for details.
