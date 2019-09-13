
package intent.matchers

import intent._
import intent.helpers.Meta
import scala.concurrent.Future

class FailureTest extends TestSuite with Stateless with Meta:
  "a toEqual failure" - :
    "is described properly" in :
      runExpectation(expect(1).toEqual(2), "Expected 2 but found 1")

    "is described properly in the negative" in :
      runExpectation(expect(1).not.toEqual(1), "Expected 1 to not equal 1")

    "is described properly with Option" in :
      runExpectation(expect(Some(1)).toEqual(None), "Expected None but found Some(1)")
