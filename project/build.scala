import com.typesafe.sbt.SbtScalariform._
import ohnosequences.sbt.SbtS3Resolver.{S3Resolver, s3, s3resolver}
import sbtrelease.ReleasePlugin._
import sbtbuildinfo.Plugin._
import sbt.Keys._
import sbt._

import scalariform.formatter.preferences._

object AkkaLibprocessBuild extends Build {
  val ProjectName    = "akka-libprocess"

  lazy val root = Project(
    id = ProjectName,
    base = file("."),
    settings = Defaults.defaultSettings ++ baseSettings ++ releaseSettings ++ publishSettings ++  Seq(
      libraryDependencies := Dependencies.core
    )
  )

  lazy val baseSettings = formatSettings ++ buildInfoSettings ++ Seq(
    organization := "akka.libprocess",
    scalaVersion := "2.10.4",

    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Spray Repository" at "http://repo.spray.io/"
    ),

    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    //fork in Test := true,
    parallelExecution in Test := false
  )

  lazy val publishSettings = S3Resolver.defaults ++ Seq(
    publishTo := Some(s3resolver.value(
      "Mesosphere Public Repo (S3)",
      s3("downloads.mesosphere.io/maven")
    ))
  )

  lazy val formatSettings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(IndentWithTabs, false)
      .setPreference(IndentSpaces, 2)
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(FormatXml, true)
  )
}

object Dependencies {
  import Dependency._

  val core = Seq(
    config            % "compile",
    slf4j             % "compile",
    akkaActor         % "compile",
    logback           % "compile",
    sprayCan          % "compile",
    Test.akkaTestKit  % "test",
    Test.scalaTest    % "test"
  )
}

object Dependency {
  object V {
    val Akka            = "2.3.3"
    val Config          = "1.0.2"
    val ScalaTest       = "2.1.3"
    val Slf4j           = "1.7.2"
    val Logback         = "1.0.9"
    val Spray           = "1.3.1"
  }

  val config      = "com.typesafe"        %   "config"          % V.Config
  val slf4j       = "org.slf4j"           %   "slf4j-api"       % V.Slf4j
  val akkaActor   = "com.typesafe.akka"   %%  "akka-actor"      % V.Akka
  val logback     = "ch.qos.logback"      %   "logback-classic" % V.Logback
  val sprayCan    = "io.spray"            %   "spray-can"       % V.Spray

  object Test {
    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit"    % V.Akka
    val scalaTest   = "org.scalatest"     %% "scalatest"       % V.ScalaTest
  }
}
