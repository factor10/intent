package intent.matchers

import intent.{Stateless, TestSuite}

class ToMatchTest extends TestSuite with Stateless:
  "toMatch":
    "for String":
      "should find match" in expect("foobar").toMatch("^foo".r)

      "should find non-match" in expect("foobar").not.toMatch("foo$".r)
