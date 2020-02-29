Intent's philosophy is to include commonly used matchers for the Scala standard library
(including `Future`, `Option` and `Either`) but not to go overboard. Instead Intent makes is
quite easy to defined your own matchers where needed.

In our experience, deeply nested or fluent matchers are often hard to discover and the
tooling support (code completion) is limited by having to know each of the namespace
identifiers until you get to the actual match.

Due to this, Intent strives to keep the matcher namespace as flat as possible altough
some prefixes exists.

The convention used in the documentation is that the _actual value_ is what is used as
parameter to expect and the _expected value_ value is what is used in the matcher
paramter.

```scala
expect( <actual value> ).<matcher>( <expected value> )
```

> The documentation for each matcher contains a reference to Intent's own unit-tests for that specific matcher to
> serve as additional examples.

If you are new to Intent or even new to testing there is also a use-case oriented document,
see [How to assert when I have a ...](how-to-assert.md).

## Types

The following types are currently supported:

- String
- Boolean
- Int
- Long
- Float
- Double
- Char
- Option

> TODO: Consider how this should be described; test with case classes; perhaps this should be a
  "what's not supported" list?

## Prefixes

All matchers (unless clearly stated) supports the following matcher prefixes.

### .not

Using the `.not` prefix will cause the negated match to be expected.

```scala
expect(true).not.toEqual(false)
```

## Matchers

### .toEqual

Match the _actual_ value to be equal to the _expected_ value.

```scala
expect(true).not.toEqual(false)
```

To compare the values, the `==` operator is used behind the scenes.

The values can also be sequences (`IterableOnce`/`Array`), in which case they must
have the same length and elements on the same position must be equal.

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToEqualTest.scala)

### .toHaveLength

Match a `Seq`/`List` (in fact, any `IterableOnce`) to have the expected length.

```scala
expect(Seq("one")).toHaveLength(1)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToHaveLengthTest.scala)

### .toContain

Match if a given sequence (either `IterableOnce` or `Array`) contains the expected element.

```scala
expect(Seq(1, 2, 3)).toContain(2)
```

It can also be used with a `Map` to match if the given Map contains the expected key-value pair.

```scala
expect(Map("one" -> 1, "two" -> 2, "three" -> 3)).toContain("two" -> 2)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToContainTest.scala)

### .toContainAllOf

Match if a given `Map` contains *all* of the expected key-value pairs.

```scala
expect(Map("one" -> 1, "two" -> 2, "three" -> 3)).toContainAllOf("two" -> 2, "one" -> 1)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToContainAllOfTest.scala)

### .toCompleteWith

Match the result of a `Future` to equal the expected value.

```scala
expect(Future.successful("foo")).toCompleteWith("foo")
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToCompleteWithTest.scala)

### .toMatch

Match the result of a String using a regular expression.

```scala
expect(someString).toMatch("^a".r)
```

Note that the regular expression only needs to partially match the actual string,
since we reckon that is the most common use case. Thus, the following test will pass:

```scala
expect("foo").toMatch("o+".r)
```

If a complete match is desired, use `^` as prefix and `$` as suffix. For example,
this will fail:

```scala
expect("foo").toMatch("^o+$".r)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToMatchTest.scala)

### .toThrow

Test that a piece of code throws an exception, optionally with a particular message.

```scala
def div(n: Int, d: Int) =
  require(d != 0, "Division by zero")
  n / d
expect(div(5, 0)).toThrow[IllegalArgumentException]("requirement failed: Division by zero")
```

This matcher can be used in three different ways:

1. With no expected message:

```scala
expect(div(5, 0)).toThrow[IllegalArgumentException]()
```

2. With an exact expected message:

```scala
expect(div(5, 0)).toThrow[IllegalArgumentException]("requirement failed: Division by zero")
```

3. With a regular expression, which is applied as a partial match:

```scala
expect(div(5, 0)).toThrow[IllegalArgumentException]("failed.*zero".r)
```

Note that since the argument to `expect` is a block, testing of a more complex piece of 
potentially-throwing code can be written as follows:

```scala
expect:
  val numerator = 5
  val denominator = 0
  div(numerator, denominator)
.toThrow[IllegalArgumentException]()
```

`toThrow` is satisified when the actual exception is of the same type as or a sub type of the
expected exception. Thus, the following test passes:

```scala
expect(div(5, 0)).toThrow[RuntimeException]()
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToThrowTest.scala)

## Manually fail or succeed a test

Two convenience methods exists where you can manually provide the the test expectation:

* `fail("Reason for failure...")` to fail a test
* `success()` to pass a test

## Asyncness

If you need to await the result of a `Future` before using a matcher, you can use
`whenComplete`:

```scala
whenComplete(Future.successful(Seq("foo", "bar"))):
  actual => expect(actual).toContain("foo")
```

This allows for more complex testing compared to when using `toCompleteWith`.

> `whenComplete` can be used regardless of the suite type, i.e. it doesn't need to
  be in an `AsyncState` suite. The async part of `AsyncState` allows for building the
  test state asynchronously, but has nothing to do with the expectations used.

## Customization

### Equality

It is possible to define custom equality for a type. Consider the following example
from Intent's own test suite:

```scala
given customIntEq as intent.core.Eq[Int] :
  def areEqual(a: Int, b: Int) = Math.abs(a - b) == 1
expect(Some(42)).toEqual(Some(43))
```

In this case, a custom equality definition for `Int` says that two values
are equal if they diff by 1. This causes the `toEqual` matcher to succeed.

### Floating-point precision

When floating-point values (`Float` and `Double`) are compared, Intent compares up
to a certain precision, defined as the number of decimals that must match.

Here's an example where a custom precision is used:

```scala
given customPrecision as intent.core.FloatingPointPrecision[Float] :
  def numberOfDecimals: Int = 2
expect(1.234f).toEqual(1.235f)
```

The test passes because we say that two `Float`s are equal to the precision of
2 decimals. In other words, the equality check actually compares 1.23 and 1.23.

The default precision is 12 decimals for `Double` and 6 decimals for `Float`.

### Formatting

It is possible to customize how a value is printed in a test failure message.
Here's an example from Intent's test suite that shows how:

```scala
given customIntFmt as core.Formatter[Int] :
  def format(a: Int): String = a.toString.replace("4", "forty-")
runExpectation(expect(42).toEqual(43),
  "Expected forty-3 but found forty-2")
```

### Timeout

The default timeout for `whenComplete` and `toCompleteWith` is 5 seconds.
It is possible to use a custom timeout:

```scala
given customTimeout as TestTimeout = TestTimeout(500.millis)
expect(someFuture).toCompleteWith("fast")
```

### List cutoff limit

TBD