/*
 * Copyright 2014 Michael Krolikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
name := "shurl"

organization := "com.github.mkroli"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

resolvers += "bintray" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
  "io.spray" %% "spray-can" % "1.3.3",
  "io.spray" %% "spray-routing" % "1.3.3",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9",
  "org.xerial.snappy" % "snappy-java" % "1.1.2",
  "net.jpountz.lz4" % "lz4" % "1.3",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "com.github.mkroli" %% "dns4s-akka" % "0.9"
)

unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
  Seq(
    base / "src" / "main" / "resources",
    base / "src" / "pack" / "etc")
}

unmanagedClasspath in Runtime <+= baseDirectory map { base =>
  Attributed.blank(base / "src" / "pack" / "etc")
}

packSettings

packMain := Map("shurl" -> "com.github.mkroli.shurl.Boot")

packJvmOpts := Map("shurl" -> Seq("-Dlogback.configurationFile=${PROG_HOME}/etc/logback.xml"))

packExtraClasspath := Map("shurl" -> Seq("${PROG_HOME}/etc"))

packGenerateWindowsBatFile := false
