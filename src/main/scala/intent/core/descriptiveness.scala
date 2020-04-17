package intent.core

import intent.macros.Position

object PositionDescription:
  def (position: Position) contextualize (desc: String) =
    s"${desc} (${position.filePath}:${position.lineNumber0 + 1}:${position.columnNumber0 + 1})"
