package intent.core.expectations

private[expectations] def listTypeName[T](actual: IterableOnce[T]): String =
  actual.getClass match
    case c if classOf[List[_]].isAssignableFrom(c) => "List"
    case c if classOf[scala.collection.mutable.ArraySeq[_]].isAssignableFrom(c)  => "Array"
    case c => c.getSimpleName
