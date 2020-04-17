package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex

sealed trait ExpectedMessage:
  def matches(msg: String): Boolean
  def describe(): String

object AnyExpectedMessage extends ExpectedMessage:
  def matches(msg: String) = true
  def describe() = ""

class ExactExpectedMessage(expected: String)(using stringFmt: Formatter[String]) extends ExpectedMessage:
  def matches(msg: String) = msg == expected
  def describe() = s" with message ${stringFmt.format(expected)}"

class RegexExpectedMessage(re: Regex) extends ExpectedMessage:
  def matches(msg: String) = re.findFirstMatchIn(msg).isDefined
  def describe() = s" with message matching /${re}/"

private case class ExceptionMatch(typeOk: Boolean, messageOk: Boolean):
  def successful(isNegated: Boolean) =
    val ok = typeOk && messageOk
    if isNegated then !ok else ok

class ThrowExpectation[TEx : ClassTag](expect: Expect[_], expectedMessage: ExpectedMessage)(using stringFmt: Formatter[String]) extends Expectation:
  def evaluate(): Future[ExpectationResult] =
    val expectedClass = implicitly[ClassTag[TEx]].runtimeClass

    def messageMatches(t: Throwable)          = expectedMessage.matches(t.getMessage)
    def hasRightType(t: Throwable)            = expectedClass.isAssignableFrom(t.getClass)
    def matchOn(t: Throwable): ExceptionMatch = ExceptionMatch(hasRightType(t), messageMatches(t))

    val r = Try(expect.evaluate()) match
      case Success(_) if expect.isNegated =>
        expect.pass
      case Success(_) =>
        val msg = s"Expected the code to throw ${expectedClass.getName}, but it did not throw anything"
        expect.fail(msg)
      case Failure(t) =>
        val exceptionMatch = matchOn(t)
        if exceptionMatch.successful(expect.isNegated) then
          expect.pass
        else if expect.isNegated then
          val details = if t.getClass == expectedClass then "did" else s"threw ${t.getClass.getName}"
          val msg = s"Expected the code not to throw ${expectedClass.getName}${expectedMessage.describe()}, but it ${details}"
          expect.fail(msg)
        else
          val msg = if exceptionMatch.typeOk then
            s"Expected the code to throw ${expectedClass.getName}${expectedMessage.describe()}, but the message was ${stringFmt.format(t.getMessage)}"
          else
            s"Expected the code to throw ${expectedClass.getName}, but it threw ${t.getClass.getName}"
          expect.fail(msg, t)

    Future.successful(r)
