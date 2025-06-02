ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "assignment3"
  )

libraryDependencies += "org.scalafx" %% "scalafx" % "24.0.0-R35"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val AkkaVersion = "2.10.6"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion