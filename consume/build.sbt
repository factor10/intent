val dottyVersion = "0.20.0-RC1"

ThisBuild / name := "consume"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := dottyVersion

lazy val root = project
  .in(file("."))
  .settings(
    name := "consume-intent",
    organization := "com.factor10",
    scalacOptions += "-Yindent-colons",
    libraryDependencies += "com.factor10" %% "intent" % "0.2.0",
    testFrameworks += new TestFramework("intent.sbt.Framework")
  )
