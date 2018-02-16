import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.typesafe.akka" %% "akka-stream" % "2.5.9",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.9" % Test,
      "com.typesafe.akka" %% "akka-http"   % "10.1.0-RC1"
    )
  )
