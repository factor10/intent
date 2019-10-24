package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

class ThrowExpectation[TEx : ClassTag](expect: Expect[_], expectedMessage: Option[String]) extends Expectation
  def evaluate(): Future[ExpectationResult] =
    val expectedClass = implicitly[ClassTag[TEx]].runtimeClass

    def messageMatches(t: Throwable) = expectedMessage match
      case Some(exp) => exp == t.getMessage
      case None      => true // message is irrelevant
    def hasRightType(t: Throwable) = expectedClass.isAssignableFrom(t.getClass)

    def isExpected(t: Throwable) =
      val ok = hasRightType(t) && messageMatches(t)
      if expect.isNegated then !ok else ok

    val r = Try(expect.evaluate()) match
      case Success(_) if expect.isNegated =>
        expect.pass
      case Success(_) =>
        val msg = s"Expected the code to throw ${expectedClass.getName}, but it did not throw anything"
        expect.fail(msg)
      case Failure(t) if isExpected(t) => expect.pass
      case Failure(t) if expect.isNegated =>
        val details = if t.getClass == expectedClass then "did" else s"threw ${t.getClass.getName}"
        val msg = s"Expected the code not to throw ${expectedClass.getName}, but it ${details}"
        expect.fail(msg)
      case Failure(t) =>
        val msg = s"Expected the code to throw ${expectedClass.getName}, but it threw ${t.getClass.getName}"
        expect.fail(msg, t)

    Future.successful(r)
