
Tests are run by issuing the `sbt test` command.

Each test will result in one of four states:

* **Successful** - A test is successful if the expectation is successful, this is what it is all about.
* **Failed** - A failed test is when the expectation was not fulfilled, or if an error occured during
the execution of the test or the exeuction of the chained test-setup.
* **Error** - Errors should be rare and typically occurs if a TestSuite could not be loaded or
instantiated.
* **Ignored** - Test was explicit set to not run.

SBT prints a summary once all discovered tests are run

```
[info] Passed: Total 65, Failed 0, Errors 0, Passed 65, Ignored 1
```