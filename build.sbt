val dottyVersion = "0.20.0-RC1"

ThisBuild / name := "intent"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := dottyVersion
ThisBuild / publishTo := sonatypePublishToBundle.value

// Let 'sbt clean' remove files that may cause the editor to get out-of-sync
// with the compiler.
cleanFiles += new java.io.File(".dotty-ide.json")
cleanFiles += new java.io.File(".dotty-ide-artifact")

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

    // Without -Yindent-colons, the editor and compiler get out of sync for me - Per
    scalacOptions += "-Yindent-colons",

    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
    // include the macro sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value,
  )
  .dependsOn(macros % "compile-internal, test-internal")
