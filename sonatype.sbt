sonatypeProfileName := "com.factor10"
publishMavenStyle := true
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("factor10", "intent", "markus.eliasson@factor10.com"))

developers := List(
  Developer(id = "eliasson", name = "Markus Eliasson", email = "markus.eliasson@gmail.com", url = url("http://markuseliasson.se")),
  Developer(id = "provegard", name = "Per Rovegård", email = "per@rovegard.se", url = url("http://www.programmaticallyspeaking.com"))
)
