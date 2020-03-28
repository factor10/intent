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
        ),
        gettingStarted(),
        why(),
        contribute())).toString

  private def gettingStarted() =
    section(
      Seq(
        h2(id := "getting-started", "Getting started"),
        p("""TODO""")))

  private def why() =
    section(
      Seq(
        h2(id := "why", "Why a new test framework?"),
        p("""The idea of a new test framework was born out of both the frustration and inspiration
        of using existing frameworks. Having written tens of thousands of tests using a variety
        of languages and frameworks there are a few challenges that keep surfacing."""),
        p(strong("Structure"),
          """, when you have thousands of tests it is important that the ceremony to
          add a new test is as low as possible. If a test belongs to the same functionality as other
          tests, these tests should stay together."""),
        p("""Intent's goal is that it should be possible to express a simple test in a single line
        and still have that line clearly express the intention of the test."""),

        p(strong("State"),
          """, most tests are not stateless, instead they require setup code in order to get
          to the state of interest for a particular test. Setting up this state is often verbose,
          heavily imperative and worst of all repeated over and over again."""),
        p("""Intent's goal is to make the dependency on state obvious for each test, and to allow
        state transformation in a hierarchical structure."""),

        p(strong("Expectation"),
          """, when using fluent and nested matchers we feel that it increases the
          cognitive load when writing the tests. You need to know each and every one of the
          qualified behaviours until you get to the one actually performing the assert. Having too
          simple matchers on the other hand results in more test code, and therefore introduce more
          noise to achieve the same expectation."""),
        p("""Intent's goal is to make assertions easy to find and use while also supporting the
        most common expectations."""),

        p("""Intent is built, not to circumvent these challenges, but to put them front and center.
        As we believe these three attributes are the most significant for achieving good quality
        test they should be the foundation of a test framework."""),

        p("""It deserves to be said that Intent pays homage to in particular two test frameworks that
        has inspired us greatly:"""),

        ul(
          li("Jasmine, supporting nested tests and the format of the expect / matchers"),
          li("ScalaTest, FreeSpec style and the use of test fixtures"))))

  private def contribute() =
    section(
      Seq(
        h2(id := "contributing", "Contributing to Intent"),
        p("""The design of Intent and the structure of tests are still moving targets. Therefore, if you wish to contribute,
             please open an issue or comment on an existing issue so that we can have a discussion first."""),
        p("For any contribution, the following applies:"),
        ul(
          li("Tests must be added, if relevant."),
          li("Documentation must be added, if relevant."),
          li("In the absence of style guidelines, please stick to the existing style. If unsure what the existing style is, ask! :)"))))
