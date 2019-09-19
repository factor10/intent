package intent.matchers

import intent.{Stateless, TestSuite}
import scala.concurrent.Future

class ToCompleteWithTest extends TestSuite with Stateless:
  "toCompleteWith" :
    "for successful Future" :
        "should be completed" in expect(Future.successful("foo")).toCompleteWith("foo")
