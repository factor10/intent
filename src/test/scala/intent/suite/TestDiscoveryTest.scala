package intent.suite

import intent.{TestSuite, State, Stateless}
import intent.testdata._

class TestDiscoveryTest extends TestSuite with State[TestDiscoveryTestState]:
  "Test discovery" using TestDiscoveryTestState() to:
    "empty test suite" using (_.withSuite(EmtpyTestSuite())) to:

      "should have 0 tests" in:
        st => expect(st.suite.allTestCases).toHaveLength(0)

    "single level test suite" using (_.withSuite(SingleLevelTestSuite())) to:

      "should have 1 tests" in:
        st => expect(st.suite.allTestCases).toHaveLength(1)

    "nested test suite" using (_.withSuite(NestedTestsSuite())) to:

      "should have 3 tests" in:
        st => expect(st.suite.allTestCases).toHaveLength(3)

case class TestDiscoveryTestState(suite: Stateless = null):
  def withSuite(s: Stateless) = copy(suite = s)
