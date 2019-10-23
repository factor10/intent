Here are some guidance on how to assert when you have...

## A boolean

The simplest use-case is when you just need to assert if a value is `true` or `false`.

```scala
"using toEqual" in:
  val coll = Seq.empty[Unit]
  expect(coll.isEmpty).toEqual(true)
```

Boolean matcher can also be negated:

```scala
"when negated" in:
  val coll = Seq.empty[Unit]
  expect(coll.isEmpty).not.toEqual(false)
```

**TIP:**
> If possible, avoid evaluating an expression to a Boolean value, since it makes the assertion
> error contain less information upon failure (e.g. expected true to be false):
> ```
> [info] [FAILED] intent.docs.HowToAssert >> How to >> assert a boolean condition (8 ms)
> [info] 	Expected false but found true (intent/src/test/scala/intent/docs/HowToAssert.scala:17:27)
> ```

For this specific example, matching on a empty sequence would have rendered far more detail
in the case of assertion error (e.g. `expect(col).toEqual(Seq.empty)`).


## A number

Numbers (Int, Long, Double, Float) can all be match using `isEqual`:

```scala
"using toEqual" in:
  expect(42).toEqual(42)
```

When it comes to decimal numbers, you most likely want to limit the precision to _N_ number of decimals.

This is achieved by providing a implicit `FloatingPointPrecision` implementation. Since it is an implicit
it is up to you if you want to use a suite wide implementation or inline one in the test. In the example below
we inline the precision to two digits:

```scala
"using custom precision" in:
  given customPrecision: intent.core.FloatingPointPrecision[Double]
    def numberOfDecimals: Int = 2
  expect(1.123456789d).toEqual(1.12d)
```


## A sequence

Asserting some condition for a sequence is a very common scenarion.

When the collection is small, the expected value can be expressed inline such as:

```scala
val coll = (1 to 3).toList
expect(coll).toEqual(Seq(1, 2, 3))
```

If only a single element is of interest, there is a `.toContain` matcher that can be used. Upon failure
the list will be printed together with the failing item.

```scala
val coll = (1 to 3).toList
expect(coll).toContain(4)

[info] [FAILED] intent.docs.HowToAssert >> How to assert >> a sequence >> for a single element (8 ms)
[info] 	Expected List(1, 2, 3) to contain 4 (intent/src/test/scala/intent/docs/HowToAssert.scala:31:21)
```

**TIP:**
> Avoid matching a collection on its length, as a failure will not give any details on the element(s) that were
> missing or extra. An alternative is to filter a larger list and match on a smaller inline list
> ```scala
> Example?
> ```

## General recommendations

Regardless of data-type or matcher, there are a few principles that are valid for most tests:

* Always make the test fail to make sure you are actually testing something.
* Think about the failure, does a failing test give enough details?
