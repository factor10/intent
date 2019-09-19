package intent.core

import intent.core.{Observable, WarmObservable, Subscriber}
import intent.{TestSuite, State}

class ObservableTestSuite extends TestSuite with State[PublisherAndSubscribers]:
  "Observable" using PublisherAndSubscribers() to :
    "when there is no subscriber" using (_.copy()) to : // TODO: can we use identity here?
      "it should still be possible to publish event" in:
        ps =>
          ps.publish()
          expect(true) toEqual(true)  // TODO: Not to throw (i.e. reached this far)

    "when there is a single subscriber" using (_.withSingleConsumer) to :
      "published event should be received by subscriber" in:
        ps =>
          ps.publish()
          expect(ps.received()).toEqual("0:0")

      "events should be received in order" in:
        ps =>
          (0 until 3).foreach(_ => ps.publish())
          expect(ps.received()).toEqual("0:0_0:1_0:2")

    "when there is a multiple subscriber" using (_.withThreeConsumers) to :
      "published event should be received by subscriber" in:
        ps =>
          ps.publish()
          expect(ps.received()).toEqual("0:0_1:0_2:0")

      "events should be received in order" in:
        ps =>
          (0 until 3).foreach(_ => ps.publish())
          expect(ps.received()).toEqual("0:0_1:0_2:0_0:1_1:1_2:1_0:2_1:2_2:2")

    "when attaching a late subscriber" using (_.withLateJoiner) to :
      "it should only receive events from when subscribing" in:
        ps =>
          ps.publish()        // 0
          ps.publish()        // 1
          ps.addSubscriber()
          ps.publish()        // 2
          expect(ps.subscribers.last.received.head).toEqual(2) // TODO: Add matcher on lists (or on matrix)

    "with a misbehaving subscriber" using (_.withMisbehavingSubscriber) to :
      "errors should be caught by observable" in:
        ps =>
          ps.publish()        // 0
          ps.publish()        // 1
          expect(true) toEqual(true)  // TODO: Not to throw (i.e. reached this far)

      "misbehaving subscriber should not store the received events" in:
        ps =>
          // Error is thrown early for misbehaving in this test. The assertion is only to make
          // sure that the throwing works.
          ps.publish()        // 0
          ps.publish()        // 1
          expect(ps.subscribers.head.received.length).toEqual(0) // Misbehaving is first

      "wellbehaved subscriber should continue to receive events" in:
        ps =>
          ps.publish()        // 0
          ps.publish()        // 1
          expect(ps.subscribers.last.received.length).toEqual(2) // Misbehaving is first

/**
 * Wraps a combination of a single publisher and a variable number of consumers
 */
case class PublisherAndSubscribers(initialSubscribers: Int = 0):

  def withSingleConsumer = PublisherAndSubscribers(1)
  def withThreeConsumers = PublisherAndSubscribers(3)
  def withLateJoiner = PublisherAndSubscribers(1)
  def withMisbehavingSubscriber =
    copy()
      .addMisbehavingSubscriber()
      .addSubscriber()

  /**
   * Record used to keep track on received events for each subscriber.
   */
  case class ReceivedEvent(subscriberId: Int, event: Int)

  val publisher = SomethingObservable()
  var subscribers: Seq[ASubscriber] = (0 until initialSubscribers).map(i => ASubscriber(i))

  // Start subscribing!!
  subscribers.map(s => publisher.subscribe(s))

  def publish() =
    publisher.publishNextValue()

  def addSubscriber(): PublisherAndSubscribers =
    addSubscriber(ASubscriber(subscribers.length))

  def addMisbehavingSubscriber(): PublisherAndSubscribers =
    addSubscriber(ASubscriber(subscribers.length, wellbehaved = false))

  private def addSubscriber(sub: ASubscriber): PublisherAndSubscribers =
    subscribers :+= sub
    publisher.subscribe(sub)
    this

  //
  // Return all received events as a string chaining each subscriber with the received value
  // such as "0:0_0:1_0:2" for a single consumer and "0:0_0:1_1:0_1:1" for two consumers.
  //
  // These methods will throw if called for unbalanced subscribers!
  //
  def receivedEvents(): Seq[ReceivedEvent] =
    subscribers.map(s => s.received.map(v => ReceivedEvent(s.id, v))).transpose.flatten

  def received(): String =
    receivedEvents().map(e => s"${e.subscriberId}:${e.event}").mkString("_")

/**
 * The producer of events that can be subscribed to.
 */
class SomethingObservable() extends WarmObservable[Int]:
  private var current = 0

  def publishNextValue(): Unit =
    publish(current)
    current += 1

/**
 * A subscriber only keeps a list of received values
 */
class ASubscriber(val id: Int, wellbehaved: Boolean = true) extends Subscriber[Int]:
  var received: Seq[Int] = List()

  override def onNext(event: Int): Unit =
    wellbehaved match
      case true => received :+= event
      case false => throw RuntimeException("Misbehaving subscriber")
