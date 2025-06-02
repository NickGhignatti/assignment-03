ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "assignment3"
  )

libraryDependencies += "org.scalafx" %% "scalafx" % "24.0.0-R35"