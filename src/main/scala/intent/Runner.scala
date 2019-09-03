package intent.sbt

import sbt.testing.{
  Framework => SFramework,
  _}

class Framework extends SFramework {
  def name(): String = "intent"
  def fingerprints(): Array[Fingerprint] = Array(IntentFingerprint)

  def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner =
    new SbtRunner(args, remoteArgs, testClassLoader)
}

/**
 * Defines how to find the Intent's test classes
 */
object IntentFingerprint extends SubclassFingerprint {
  // Disable the usage of modules (singleton objects) - this makes discovery faster
  val isModule = false

  // Constructor parameters will not be injected and will only cause confusion - disable.
  def requireNoArgConstructor(): Boolean = true

  // All tests needs to inherit from this type
  def superclassName(): String = "intent.IntentMaker"
}

/**
 * A single run of a single test (controlled by SBT)
 */
class SbtRunner(
  val args: Array[String],
  val remoteArgs: Array[String],
  classLoader: ClassLoader) extends Runner {

  def done(): String = {
    // This is called when test is done, and after that the test task myst not be called
    // Not obvious in the doc how what you're supposed to do here really..
    ""
  }

  def tasks(list: Array[TaskDef]): Array[Task] = {
    list.foreach(td => println(s"About to test: ${td.fullyQualifiedName}"))
    Array[Task]()
  }
}