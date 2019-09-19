
package intent.matchers

import intent._
import intent.helpers.Meta
import scala.concurrent.Future

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