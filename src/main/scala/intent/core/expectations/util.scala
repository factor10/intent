package intent.core.expectations

private[expectations] def listTypeName[T](actual: IterableOnce[T]): String =
  Option(actual).map(_.getClass) match
    case Some(c) if classOf[List[_]].isAssignableFrom(c) => "List"
    case Some(c) if classOf[scala.collection.mutable.ArraySeq[_]].isAssignableFrom(c)  => "Array"
    case Some(c) => c.getSimpleName
    // Null is an edge case, I think. If it turns out not to be, then we could take
    // the list type with ClassTag, possibly. 
    case None    => "<null>"
