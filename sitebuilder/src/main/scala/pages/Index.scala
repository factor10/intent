package intent.site.pages

import scala.language.implicitConversions
import scalatags.Text.all._

object Index extends Page with
  override def fileName() = "index.html"

  override def generate() =
    standardLayout(
      Seq(
        p("Intent is an opinionated test framework for Dotty. It builds on the following principles:"),
        ul(
          li("Low ceremony test code"),
          li("Uniform test declaration"),
          li("Futures and async testing"),
          li("Arranging test state"),
          li("Fast to run tests"),
        )
      )
    ).toString
