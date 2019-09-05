import intent.{TestSuite, Intent}

class TestDiscoveryTest extends TestSuite with Intent[Unit] {
  "Test discovery" - {
    "empty test suite" - {
      val suite = new EmtpyTestSuite()

      "should have 0 tests" in { _ =>
        expect(suite.allTestCases.length ).toEqual(0)
      }
    }

    "single level test suite" - {
      val suite = new SingleLevelTestSuite()

      "should have 1 tests" in { _ =>
        expect(suite.allTestCases.length ).toEqual(1)
      }
    }

    "nested test suite" - {
      val suite = new NestedTestsSuite()

      "should have 3 tests" in { _ =>
        expect(suite.allTestCases.length ).toEqual(3)
      }
    }
  }

   def emptyState = ()
}

class EmtpyTestSuite extends TestSuite with Intent[Unit] {
  def emptyState = ()
}

class SingleLevelTestSuite extends TestSuite with Intent[Unit] {
  "root suite" - {
    "root test" in { state =>
      expect( 1 + 1 ) toEqual 2
    }
  }

  def emptyState = ()
}

class NestedTestsSuite extends Intent[Unit] {
  "root suite" - {
    "child suite" - {
      "grand child suite" - {
        "grand child test" in { state =>
          expect( 1 + 1 ) toEqual 2
        }
      }

      "child test" in { state =>
        expect( 1 + 1 ) toEqual 2
      }
    }

    "root test" in { state =>
      expect( 1 + 1 ) toEqual 2
    }
  }

  def emptyState = ()
}
