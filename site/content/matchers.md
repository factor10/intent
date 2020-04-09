---
title: "Matchers"
date: 2020-04-09T13:44:39+02:00
draft: false
---

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

{{< intent "ToEqualTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToEqualTest.scala" >}}


### .toHaveLength

Match a `Seq`/`List` (in fact, any `IterableOnce`) to have the expected length.

```scala
expect(Seq("one")).toHaveLength(1)
```

{{< intent "ToHaveLengthTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToHaveLengthTest.scala" >}}


### .toContain

Match if a given sequence (either `IterableOnce` or `Array`) contains the expected element.

```scala
expect(Seq(1, 2, 3)).toContain(2)
```

It can also be used with a `Map` to match if the given Map contains the expected key-value pair.

```scala
expect(Map("one" -> 1, "two" -> 2, "three" -> 3)).toContain("two" -> 2)
```

{{< intent "ToContainTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToContainTest.scala" >}}


### .toContainAllOf

Match if a given `Map` contains *all* of the expected key-value pairs.

```scala
expect(Map("one" -> 1, "two" -> 2, "three" -> 3)).toContainAllOf("two" -> 2, "one" -> 1)
```

{{< intent "ToContainAllOfTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToContainAllOfTest.scala" >}}


### .toCompleteWith

Match the result of a `Future` to equal the expected value.

```scala
expect(Future.successful("foo")).toCompleteWith("foo")
```

{{< intent "ToCompleteWithTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToCompleteWithTest.scala" >}}


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

{{< intent "ToMatchTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToMatchTest.scala" >}}


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

{{< intent "ToThrowTest.scala" "https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToThrowTest.scala" >}}

