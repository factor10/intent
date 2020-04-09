---
title: "Customization"
date: 2020-04-09T13:44:39+02:00
draft: false
---
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

## Equality

It is possible to define custom equality for a type. Consider the following example
from Intent's own test suite:

```scala
given customIntEq as intent.core.Eq[Int] :
  def areEqual(a: Int, b: Int) = Math.abs(a - b) == 1
expect(Some(42)).toEqual(Some(43))
```

In this case, a custom equality definition for `Int` says that two values
are equal if they diff by 1. This causes the `toEqual` matcher to succeed.

## Floating-point precision

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

## Formatting

It is possible to customize how a value is printed in a test failure message.
Here's an example from Intent's test suite that shows how:

```scala
given customIntFmt as core.Formatter[Int] :
  def format(a: Int): String = a.toString.replace("4", "forty-")
runExpectation(expect(42).toEqual(43),
  "Expected forty-3 but found forty-2")
```

## Timeout

The default timeout for `whenComplete` and `toCompleteWith` is 5 seconds.
It is possible to use a custom timeout:

```scala
given customTimeout as TestTimeout = TestTimeout(500.millis)
expect(someFuture).toCompleteWith("fast")
```
