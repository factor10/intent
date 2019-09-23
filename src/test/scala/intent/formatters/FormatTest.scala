package intent.formatters

import intent._

class FormatTest extends TestSuite with Stateless:
  "Formatting" :
    "surrounds Char in single quotes" in expect(format('a')).toEqual("'a'")
    "surrounds String in double quotes" in expect(format("a")).toEqual("\"a\"")

  def format[T](x: T) given (fmt: core.Formatter[T]): String =
    fmt.format(x)