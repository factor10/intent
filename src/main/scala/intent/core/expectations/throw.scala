package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

class ThrowExpectation[TEx : ClassTag](expect: Expect[_]) extends Expectation
  def evaluate(): Future[ExpectationResult] =
    val expectedClass = implicitly[ClassTag[TEx]].runtimeClass
    def isExpected(clazz: Class[_]) = if expect.isNegated then expectedClass ne clazz else expectedClass eq clazz
    val r = Try(expect.evaluate()) match
      case Success(_) => ???
      case Failure(t) if isExpected(t.getClass) => expect.pass
      case Failure(t) if expect.isNegated =>
        val msg = s"Expected the code not to throw ${expectedClass.getName}, but it did"
        expect.fail(msg)
      case Failure(t) =>
        val msg = s"Expected the code to throw ${expectedClass.getName}, but it threw ${t.getClass.getName}"
        expect.fail(msg)

    Future.successful(r)
