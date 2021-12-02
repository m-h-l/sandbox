ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.17"
)

lazy val root = (project in file("."))
  .settings(
    name := "sandbox",
    idePackagePrefix := Some("org.mhackl.sandbox")
  )
