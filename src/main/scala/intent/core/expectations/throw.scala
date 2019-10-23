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
      case Failure(t) => ???

    Future.successful(r)
