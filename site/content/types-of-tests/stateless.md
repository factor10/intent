---
title: "Table-driven tests"
date: 2020-04-09T13:44:39+02:00
draft: false
---

# Table-driven tests

A stateless test suite extends the `Statesless` suite. _Contexts_ in this style
serve no other purpose than grouping tests into logical units.

Consider the following example:

```scala
import intent.{Stateless, TestSuite}

class CalculatorTest extends TestSuite with Stateless with
  "A calculator" :
    "can add" :
      "plain numbers" in expect(Calculator().add(2, 4)).toEqual(6)
      "complex numbers" in:
        val a = Complex(2, 3)
        val b = Complex(3, 4)
        expect(Calculator().add(a, b)).toEqual(Complex(5, 7))
    "can multiply" :
      "plain numbers" in expect(Calculator().multiply(2, 4)).toEqual(8)
```

Here, contexts serve to group tests based on the arithmetical operation used.
