Intent is built using Scala 3 (called Dotty) and is an early adopter of both new and
experimental features. So assume that you will need a recent version of Dotty to use
Intent.

We'll try to state minimum requried Dotty version in `README.md` (and it can be found
in  `build.sbt`)


## Our first test

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


## Running tests

Intent only supports SBT for running tests (at least for now) where you run tests
using `sbt test` command.

SBT will identify all your test suites that are stored under the default Scala
test directory: `src/test`


## Test result

The test results are printed to STDOUT via the SBT log:

```
[info] [PASSED] intent.matchers.ToHaveLengthTest >> toHaveLength >> empty list should have length 0 (0 ms)
[info] [PASSED] intent.matchers.ToHaveLengthTest >> toHaveLength >> Seq(1) should have length 1 (0 ms)
[info] [PASSED] intent.matchers.ToHaveLengthTest >> toHaveLength >> Seq() should *not* have length 1 (0 ms)
```

_Currently there are no reports other than the SBT output._


## Stateful test

Not all tests can be implemented without setting the scene, still many test frameworks only focus
on a expressive way to assert expectations. For `Intent` state management is front and center.

Lets go straight to the code:

```scala
class StatefulTest extends TestSuite with State[StatefulState] :
  "root context" using StatefulState() to :
    "transforms a little" using (_.addOne()) to :
      "to transform some more" using (_.addOne()) to :
        "check the stuff" in :
          state => expect(state.stuff).toHaveLength(2)

case class StatefulState(stuff: Seq[String] = Seq.empty):
  def addOne(): StatefulState = copy(stuff = stuff :+ "one more")

```

A test suite that needs state must implement `State[T]`, where `T` is the type carrying
the state you need. There are _no requirements_ on the type `T` or its signature, you are free
to use whatever type you want. We prefere to use `case class` as they are immutable, but any
type will do.

The _root state_ gets created in the `"root context"` and passed downstream to any child context.
Each child context has the possiblity to _transform_ the state before it is passed to the actual
test as a parameter.

```scala
"check the stuff" in :
  state => expect(state.stuff).toHaveLength(2)
```

A suite must _either_ be _stateless_ or _stateful_. There is no support in writing a test that does not
take a state when you derive from `State` and vice versa. While not the same, it resembles how Scala
separate a `class` and an `object`.

There are a few conventions or recommendations on how to use state:

* Put the state implementation below the test.
* Prefer to call methods on the state object over doing it in the test itself
* Keep state focused and create a new suite + state class when needed (cost is low)
