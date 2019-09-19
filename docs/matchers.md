Intents philosophy is to include commonly used matchers for the Scala standard library
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
expect( actual value ).<matcher>( expected value )
```

> Each matcher contains a reference to Intent's own unit-tests for that specific matcher to
> serve as additional examples.


## Types

The following types are currently supported:

- String
- Boolean
- Int
- Option

> TODO: Describe how to provide a custom equality check and formatter for types


## Prefixes

All matchers (unless clearly stated) supports the following matcher prefixes.

### .not

Using the `.not` prefix will cause the negated match to be expected.

```scala
expect(true).not.toEqual(false)
```

## Matchers

### .toEqual

Match the _actual_ value to be equal to the _expected_ value

```scala
expect(true).not.toEqual(false)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToEqualTest.scala)


### .toHaveLength

Match a `Seq` to have the expected length

```scala
expect(Seq("one")).toHaveLength(1)
````

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToHaveLengthTest.scala)


### .toContain

Match if a given sequence contains the expected element.

```scala
expect(Seq(1, 2, 3)).toContain(2)
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToContainTest.scala)

### .toCompleteWith

Match the result of a `Future` to equal the exepected value.

```scala
expect(Future.successful("foo")).toCompleteWith("foo")
```

[Additional examples](https://github.com/factor10/intent/blob/master/src/test/scala/intent/matchers/ToCompleteWithTest.scala)


## Manually fail or succeed a test

Two convenience methods exists where you can manually provide the the test expectation:

* `fail("Reason for failure...")` to fail a test
* `success()` to pass a test
