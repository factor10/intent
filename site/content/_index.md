---
title: "Intent - A test framework for Dotty"
date: 2020-04-09T13:44:39+02:00
draft: false
---

Intent is a test framework for [Dotty](https://dotty.epfl.ch) (which is expected to become Scala 3 in year 2020).

Intent is designed to give you clear and concise tests by focusing on:

* Low ceremony test code
* Uniform test declaration
* Futures and async testing
* Arranging test state
* Fast to run tests


Let us see how a test suite looks like for Intent:

```scala
class StatefulTest extends TestSuite with State[Cart]:
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

## Getting started

Intent is built using Scala 3 (called Dotty) and is an early adopter of both new and
experimental features. So assume that you will need a recent version of Dotty to use
Intent.

We'll try to state minimum required Dotty version in `README.md` (you can also find it
in `build.sbt`)

### Setting up SBT

The first thing you need to do is to add Intent to your SBT project with the following
lines to your `build.sbt`:

```scala
libraryDependencies += "com.factor10" %% "intent" % "0.5.0",
testFrameworks += new TestFramework("intent.sbt.Framework")
```

### Our first test

Let's have a look at how tests should be written.

```scala
import intent.{Stateless, TestSuite}

class ToEqualTest extends TestSuite with Stateless:
  "toEqual" :
    "for Boolean" :
      "true should equal true" in expect(true).toEqual(true)
      "true should *not* equal false" in expect(true).not.toEqual(false)
```

All tests must belong to a test suite. A test suite is a class that extends
`TestSuite` and in this case `Stateless` to indicate the tests do not depend
on any state setup.

_Stateful tests will be described shortly._

Extending `TestSuite` is required, since that is how SBT discovers your tests,
that is its only purpose.


#### Running tests

Intent only supports SBT for running tests (at least for now) where you run tests
using `sbt test` command.

SBT will identify all your test suites that are stored under the default Scala
test directory: `src/test`

The test results are printed to STDOUT via the SBT log:

```log
 [info] [PASSED] ToEqualTest >> toEqual >> for Boolean >> true should *not* equal false (25 ms)
 [info] [PASSED] ToEqualTest >> toEqual >> for Boolean >> true should equal true (30 ms)
```

_Currently there are no reports other than the SBT output._


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


## Contributing to Intent

The design of Intent and the structure of tests are still moving targets.
Therefore, if you wish to contribute, please open an issue or comment on an
existing issue so that we can have a discussion first.

For any contribution, the following applies:

* Tests must be added, if relevant.
* Documentation must be added, if relevant.
* In the absence of style guidelines, please stick to the existing style.
  If unsure what the existing style is, ask! :)
