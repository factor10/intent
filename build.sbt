val dottyVersion = "0.17.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-simple",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",

    testFrameworks += new TestFramework("intent.sbt.Framework")
  )
