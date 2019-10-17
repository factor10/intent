
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


## Ignoring tests

If you want to ignore a test you can substitute the `in` keyword with `ignore`.

```scala
"ignored test" ignore:
  fail("This should never fail, only be ignored!")
```

Ignored tests will still be logged by SBT, but classified as `IGNORED` and printed
in yellow.

```
[info] [IGNORED] intent.sbt.IgnoredTest >> ignored test (0 ms)
```

> Note: A ignored test will not be evaluated, but the signature must be valid so
> it needs to return an expectation.


## Focusing tests

If you only want to run a single (or a few) tests, you can substitute the `in`
keyword with `focus`.

```scala
"test work in progress" focus:
  expect(30 + 3).toEqual(33)
```

This will result in that _only_ focused tests are run, and all other tests are
marked interpreted as ignored.

> When in focued mode, SBT reporting will only print success or failure for tests
> no ignored tests will be logged.
