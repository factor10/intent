package intent.styles

import intent._

class TableDrivenTest extends TestSuite with State[TableState]:
  "Table-driven test" usingTable (myTable) to:
    "uses table data" in:
      s =>
        expect(s.a + s.b).toEqual(s.sum)

  def myTable: Seq[TableState] = Seq(
    TableState(1, 2, 3),
    TableState(2, 3, 5),
    TableState(-1, -2, -3)
  )

case class TableState(a: Int, b: Int, sum: Int):
  override def toString = s"$a + $b should be $sum"
