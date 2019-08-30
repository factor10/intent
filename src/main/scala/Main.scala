import java.net.URLClassLoader
import scala.util.control.NonFatal

object Main {

  def (full: String) beginsWith (head: String): Boolean = full.startsWith(head)

  def main(args: Array[String]): Unit = {
    val classes = Seq(classOf[MyTestFixture])
    val testCases = classes.flatMap(findTestCases)

    var errorCount = 0
    testCases.foreach { tc =>
      val fullName = tc.nameParts.mkString(" > ")
      println(s"$fullName")
      try {
        tc.run()
        println("PASS")
      } catch {
        case ex: AssertionError =>
          System.err.println("FAIL")
          System.err.println(ex.getMessage)
          errorCount += 1
        case NonFatal(t) =>
          t.printStackTrace()
          errorCount += 1
      }
    }

    System.exit(errorCount)


  }

  private def findTestCases(cl: Class[_]): Seq[ITestCase] = {
    val instance = cl.newInstance.asInstanceOf[Intent[_]]
    instance.allTestCases
  }

  def msg = "I was compiled by dotty :)"

}

trait ITestCase {
  def nameParts: Seq[String]
  def run(): Unit
}

trait Intent[TState] {
  type Transform = TState => TState
  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Unit) extends ITestCase {
    def nameParts: Seq[String] = setup.map(_.name)
    def run(): Unit = {
      val state = setup.foldLeft(emptyState)((st, part) => part.transform(st))
      impl(state)
    }
  }

  def allTestCases: Seq[ITestCase] = testCases

  private var testCases: Seq[TestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty

  // behövs för att extension-metoder appliceras höger till vänster??
  def (blockName: String) via (transformAndBlock: TransformAndBlock): Unit = {
    SetupPart(blockName, transformAndBlock.transform) - transformAndBlock.blk()
  }

  def (setupPart: SetupPart) - (block: => Unit): Unit = {
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail
  }

  def (blockName: String) - (block: => Unit): Unit = {
    val setupPart = SetupPart(blockName, s => s)
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail
  }

  // hack för att "XX via YY - BLK" evalueras som "XX via (YY - BLK)"
  def (transform: Transform) - (block: => Unit): TransformAndBlock = {
    TransformAndBlock(transform, () => block)
  }

  def (testName: String) in (testImpl: TState => Unit): Unit = {
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, testImpl)
  }

  def expect[T](expr: => T): Expect[T] = new Expect[T](expr)

  def emptyState: TState

  given as Eq[Int] = IntEq
  given as Formatter[Int] = IntFmt
}

object IntFmt extends Formatter[Int] {
  def format(i: Int): String = i.toString
}

object IntEq extends Eq[Int] {
  def areEqual(a: Int, b: Int): Boolean = a == b
}

trait Ord[T] {
  def compare(a: T, b: T): Int
}

trait Eq[T] {
  def areEqual(a: T, b: T): Boolean
}

trait Formatter[T] {
  def format(value: T): String
}

class AssertionError(msg: String) extends RuntimeException(msg)

class Expect[T](blk: => T) {
  def toEqual(other: T) given (eq: Eq[T], fmt: Formatter[T]): Unit = {
    val value = blk
    if (!eq.areEqual(value, other)) {
      val actualStr = fmt.format(value)
      val expectedStr = fmt.format(other)
      throw new AssertionError(s"Expected $expectedStr but got $actualStr")
    }
  }
}

class Calculator {
  def add(a: Int, b: Int): Int = a + b
}

case class MyState(calc: Calculator)

class MyTestFixture extends Intent[MyState] {
  "a calculator" via newCalculator - {
    "can add" in { state =>
      expect(state.calc.add(1, 2)) toEqual 3
    }
  }
  "a calculator (no state transform)" - {
    "can add" in { state =>
      val calc = new Calculator // not in state
      expect(calc.add(1, 2)) toEqual 4
    }
  }

  def newCalculator(s: MyState) = s.copy(calc = new Calculator) 
  def emptyState = MyState(null)
}