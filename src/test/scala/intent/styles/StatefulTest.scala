package intent.styles

import intent._

class StatefulTest extends TestSuite with State[StatefulState]:
  "root context" using StatefulState() to:
    "transforms a little" using (_.addOne()) to:
      "to transform some more" using (_.addOne()) to:
        "check the stuff" in:
          state => expect(state.stuff).toHaveLength(2)

case class StatefulState(stuff: Seq[String] = Seq.empty):
  def addOne(): StatefulState = copy(stuff = stuff :+ "one more")
