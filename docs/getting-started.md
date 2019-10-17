Intent is built using Scala 3 (called Dotty) and is an early adopter of both new and
experimental features. So assume that you will need a recent version of Dotty to use
Intent.

We'll try to state minimum requried Dotty version in `README.md` (and it can be found
in  `build.sbt`)

## Setting up SBT

The first thing you need to do is to add Intent to your SBT project with the following
lines to your `build.sbt`:

```scala
libraryDependencies += "com.factor10" %% "intent" % "0.1.0",
testFrameworks += new TestFramework("intent.sbt.Framework")
```

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

## Further reading

Consult [Test styles](./test-styles.md) for more information about how to write
tests.

