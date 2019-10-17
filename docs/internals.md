Intent is self-hosted and runs all tests using this version of Intent. This is
great and it feels really good running your tests with your _"system under test"_.
However there are a few gotchas related to the same:

* When adding logging, remember that the logs will most likely be printed multiple
  times. Both for the runner invoked by SBT and the runner invoked by your test.

* Tests that are not supposed to be discovered by SBT should not extend `TestSuite`.
  Instead they should be programatically loaded by the running test (e.g. the tests
  under `intent.testdata`).
