val dottyVersion = "0.18.1-RC1"

ThisBuild / name := "intent"
ThisBuild / version := "0.0.13"
ThisBuild / scalaVersion := dottyVersion
ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val macros = (project in file("macros"))
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "intent",
    organization := "com.factor10",
    libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0",
    testFrameworks += new TestFramework("intent.sbt.Framework"),

    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
    // include the macro sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value,
  )
  .dependsOn(macros % "compile-internal, test-internal")
