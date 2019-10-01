package intent.core

/**
  * Used as callback when running a test suite, for reporting test events.
  * Originally part of a simple Observable pattern implementation, but this
  * is all we need right now.
  */
trait Subscriber[T]:
  def onNext(event: T): Unit
