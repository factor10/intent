val dottyVersion = "0.18.1-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "intent",
    version := "0.0.9",

    scalaVersion := dottyVersion,

    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",

    testFrameworks += new TestFramework("intent.sbt.Framework")
  )
