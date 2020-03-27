package intent.site.pages

import scala.language.implicitConversions
import scalatags.Text.all._

trait Page with
  def fileName(): String
  def generate(): String

  /**
   * Generate a page with the standard layout with a left-hand side menu.
   */
  def standardLayout(content: Seq[Modifier]) =
    html(
      head(
        tag("title")("Intent - A test framework for Dotty"),
        meta(charset := "utf-8"),
        meta(name := "viewport", attr("content") := "width=device-width,initial-scale=1.0"),
        link(href := "assets/intent.css", rel := "stylesheet"),
        link(href := "assets/prism.intent.css", rel := "stylesheet")
      ),
      body(
        div(cls := "intent-page",
          menu(),
          tag("article")(cls := "intent-page-article",
            content),
        ),
        script(src := "assets/prism.js")
      )
    )

  def menu() =
    div(cls := "intent-page-toc",
      header(
        div(cls := "intent-logo",
          img(src := "assets/intent-logo.png"),
          h1("Intent"),
        ),
      ),
      div(cls := "intent-page-toc-chapter",
        h4("Introduction"),
        ul(
          li(a(href := "#")("Getting started")),
          li(a(href := "#")("Why a new framwork")),
          li(a(href := "#")("Contributing")),
          li(a(href := "https://github.com/factor10/intent")("Find us at GitHub")),
        ),
      ),
    )
