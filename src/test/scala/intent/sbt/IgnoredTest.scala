package intent.sbt

import intent.{TestSuite, State, Stateless}

// The ignored is used to manually verify that SBT reports ignored tests using correct
// and summary. The actual runner and result are tested with other unit-tests.

class IgnoredTest extends TestSuite with Stateless:
  "ignored test" ignore:
    fail("This should never fail, only be ignored!")
