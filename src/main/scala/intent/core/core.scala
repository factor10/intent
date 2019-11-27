package intent.core

import scala.collection.mutable.{ Map => MMap }
import scala.collection.immutable.{ Map => IMap }
import scala.collection.Map

type MapLike[K, V] = Map[K, V] | IMap[K, V] | MMap[K, V]