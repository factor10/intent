---
title: "Stateful tests"
date: 2020-04-09T13:44:39+02:00
draft: false
---

# Stateful tests

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
