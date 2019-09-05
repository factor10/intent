
package acme

import intent._

class Calculator {
  def add(a: Int, b: Int): Int = a + b
}

case class MyState(calc: Option[Calculator])

class MyTestFixture extends TestSuite with Intent[MyState] {
  "a calculator" via newCalculator - {
    "can add" in { state =>
      val actual = state.calc.map(_.add(1, 2))
      expect(actual) toEqual Some(4)
    }
  }
  "a calculator (no state transform)" - {
    "can add" in { state =>
      val calc = new Calculator // not in state
      expect(calc.add(1, 2)) toEqual 4
    }
  }

  def newCalculator(s: MyState) = s.copy(calc = Some(new Calculator))
  def emptyState = MyState(None)
}
