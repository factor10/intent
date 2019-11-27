package intent.docs

import intent.{TestSuite, Stateless}

// Example code used in documentation.
//
// These examples should **not** be used to test intent, only to illustrate
// functions, patterns, etc.
//
// Unfortunately are these not referenced from any documentation tool, but have
// to be copy pasted to documentation for now.

class HowToAssert extends TestSuite with Stateless
  "How to assert":
    "a boolean":
      "using toEqual" in:
        val coll = Seq.empty[Unit]
        expect(coll.isEmpty).toEqual(true)

      "when negated" in:
        val coll = Seq.empty[Unit]
        expect(coll.isEmpty).not.toEqual(false)

    "a number":
      "using toEqual" in:
        expect(42).toEqual(42)

      "using custom precision" in:
        given customPrecision: intent.core.FloatingPointPrecision[Double]
          def numberOfDecimals: Int = 2
        expect(1.123456789d).toEqual(1.12d)

    "a sequence":
      "using a standard way" in:
        val coll = (1 to 3).toList
        expect(coll).toEqual(Seq(1, 2, 3))

      "for a single element" in:
        val coll = (1 to 3).toList
        expect(coll).toContain(2)
