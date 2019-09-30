
package intent.matchers

import intent._
import intent.helpers.Meta
import intent.core.TestTimeout
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class FailureTest extends TestSuite with Stateless with Meta:
  "a toEqual failure" :
    "is described properly" in :
      runExpectation(expect(1).toEqual(2), "Expected 2 but found 1")

    "is described properly in the negative" in :
      runExpectation(expect(1).not.toEqual(1), "Expected 1 not to equal 1")

    "is described properly with Option" in :
      runExpectation(expect(Some(1)).toEqual(None), "Expected None but found Some(1)")

  "a toMatch failure" :
    "is described properly" in :
      runExpectation(expect("foobar").toMatch("^bar".r), "Expected \"foobar\" to match /^bar/")

    "is described properly in the negative" in :
      runExpectation(expect("foobar").not.toMatch("^foo".r), "Expected \"foobar\" not to match /^foo/")

  "a toContain failure" :
    "is described properly" in :
      runExpectation(expect(Seq(1, 2)).toContain(3), "Expected List(1, 2) to contain 3")

    "is described properly in the negative" in :
      runExpectation(expect(Seq(1, 2)).not.toContain(1), "Expected List(1, 2) not to contain 1")

    "is described properly for Array" in :
      runExpectation(expect(Array(1, 2)).toContain(3), "Expected Array(1, 2) to contain 3")

  "using fail()" :
    "should fail with the given description" in runExpectation(fail("Manually failed"), "Manually failed")

  "Future timeout" :
    "should abort a long-running test" in :
      given customTimeout as TestTimeout = TestTimeout(50.millis)
      val p = Promise[Int]()
      runExpectation({
        whenComplete(p.future) :
          result => expect(result).toEqual(42)
      }, "Test timed out")