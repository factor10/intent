package intent.site.pages

import intent._

class PageTest extends TestSuite with Stateless with
  "Page":
    "section":
      "should use class 'intent-page-section'" in:
        // TODO: Create a toBeginWith matcher for String, or .toContain("")
        val sectionHtml = SomePage.section(Seq.empty).toString
        expect(sectionHtml.startsWith("<section class=\"intent-page-section\">")).toEqual(true)

object SomePage extends Page:
  override def fileName() = ""
  override def generate() =  ""
