---
title: "Table-driven tests"
date: 2020-04-09T13:44:39+02:00
draft: false
---

# Table-driven tests

Table-driven tests allow for a declarative approach to writing tests, and is useful to
test many different variations of some feature with as little boilerplate as possible.

For example, consider the following test suite that tests a [Fibonacci](https://en.wikipedia.org/wiki/Fibonacci_number) function:

```scala
class FibonacciTest extends TestSuite with State[TableState] with
  "The Fibonacci function" usingTable (examples) to :
    "works" in :
      example =>
        expect(F(example.n)).toEqual(example.expected)

  def examples = Seq(
    FibonacciExample(0, 0),
    FibonacciExample(1, 1),
    FibonacciExample(2, 1),
    FibonacciExample(3, 2),
    FibonacciExample(12, 144)
  )

  def F(n: Int): Int = ... // implemented elsewhere

case class FibonacciExample(n: Int, expected: Int) with
  override def toString = s"Fn($n) = $expected"
```

Intent will run the test code for each example returned by the `examples`
method. This makes it easy to adjust examples and add new ones.

> The exact syntax for table-driven tests may change in a future release. The
  current syntax results in test output that is a bit noisy.
