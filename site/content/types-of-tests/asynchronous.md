---
title: "Asynchronous tests"
date: 2020-04-09T13:44:39+02:00
draft: false
---

#  Asynchronous tests

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
