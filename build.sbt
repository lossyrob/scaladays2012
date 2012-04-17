resolvers += ScalaToolsSnapshots

name := "scaladays2012"

organization := "org.improving"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.0-SNAPSHOT"

libraryDependencies <<= (scalaVersion)(sv => Seq("org.scala-lang" % "scala-compiler" % sv))
