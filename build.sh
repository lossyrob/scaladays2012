#!/bin/sh
#

rm -rf classes && mkdir classes
$SCALA_HOME/bin/scalac -d classes $(find src -name '*.scala')
