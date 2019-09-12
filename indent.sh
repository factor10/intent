#!/bin/bash

ti=~/.ivy2/cache/org.scala-sbt/test-interface/jars/test-interface-1.0.jar
find . -name '*.scala' | xargs dotc -classpath $ti -rewrite -new-syntax
find . -name '*.scala' | xargs dotc -classpath $ti -rewrite -indent
