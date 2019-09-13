package intent.suite

import intent.{TestSuite, Stateless}
import intent.testdata._

class TestDiscoveryTest extends TestSuite with Stateless:
  "Test discovery" :
    "empty test suite" :
      val suite = new EmtpyTestSuite()

      "should have 0 tests" in expect(suite.allTestCases).toHaveLength(0)

    "single level test suite" :
      val suite = new SingleLevelTestSuite()

      "should have 1 tests" in expect(suite.allTestCases).toHaveLength(1)

    "nested test suite" :
      val suite = new NestedTestsSuite()

      "should have 3 tests" in expect(suite.allTestCases).toHaveLength(3)
