val dottyVersion = "0.18.1-RC1"

ThisBuild / name := "intent"
ThisBuild / version := "0.0.9"
ThisBuild / scalaVersion := dottyVersion

lazy val macros = (project in file("macros"))

lazy val root = project
  .in(file("."))
  .settings(
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",

    testFrameworks += new TestFramework("intent.sbt.Framework")
  )
  .dependsOn(macros)
