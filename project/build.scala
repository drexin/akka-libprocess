import sbt._
import Keys._
import sbtprotobuf.{ProtobufPlugin=>PB}

object AkkaLibprocessBuild extends Build {

//////////////////////////////////////////////////////////////////////////////
// PROJECT INFO
//////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION    = "akka.libprocess"
  val PROJECT_NAME    = "akka-libprocess"
  val PROJECT_VERSION = "0.1.0"
  val SCALA_VERSION   = "2.10.4"


//////////////////////////////////////////////////////////////////////////////
// DEPENDENCY VERSIONS
//////////////////////////////////////////////////////////////////////////////

  val MESOS_VERSION           = "0.18.0"
  val AKKA_VERSION            = "2.3.3"
  val TYPESAFE_CONFIG_VERSION = "1.0.2"
  val SCALATEST_VERSION       = "2.1.3"
  val SLF4J_VERSION           = "1.7.2"
  val LOGBACK_VERSION         = "1.0.9"
  val PROTOBUF_VERSION        = "2.5.0"


//////////////////////////////////////////////////////////////////////////////
// NATIVE LIBRARY PATHS
//////////////////////////////////////////////////////////////////////////////

  val pathToMesosLibs = "/usr/local/lib"


//////////////////////////////////////////////////////////////////////////////
// PROJECTS
//////////////////////////////////////////////////////////////////////////////

  lazy val root = Project(
    id = PROJECT_NAME,
    base = file("."),
    settings = commonSettings
  ) dependsOn (
    core, messages
  ) aggregate (
    core, messages
  )

  def subproject(suffix: String) = s"${PROJECT_NAME}-$suffix"

  lazy val core = Project(
    id = subproject("core"),
    base = file("core"),
    settings = commonSettings
  ) dependsOn(messages)

  lazy val messages = Project(
    id = subproject("messages"),
    base = file("messages"),
    settings = commonSettings ++ PB.protobufSettings ++ Seq(
      javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "java")
    )
  )

//////////////////////////////////////////////////////////////////////////////
// SHARED SETTINGS
//////////////////////////////////////////////////////////////////////////////

  lazy val commonSettings = Defaults.defaultSettings ++
                            basicSettings

  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Spray Repository" at "http://repo.spray.io/"
    ),

    libraryDependencies ++= Seq(
      "com.typesafe"       % "config"          % TYPESAFE_CONFIG_VERSION  % "compile",
      "org.slf4j"          % "slf4j-api"       % SLF4J_VERSION            % "compile",
      "com.typesafe.akka" %% "akka-actor"      % AKKA_VERSION             % "compile",
      "com.typesafe.akka" %% "akka-testkit"    % AKKA_VERSION             % "compile",
      "ch.qos.logback"     % "logback-classic" % LOGBACK_VERSION          % "compile",
      "org.scalatest"     %% "scalatest"       % SCALATEST_VERSION        % "compile",
      "com.google.protobuf" % "protobuf-java"  % PROTOBUF_VERSION         % "compile",
      "io.spray"            % "spray-can"      % "1.3.1"                  % "compile"
    ),

    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    javaOptions += "-Djava.library.path=%s:%s".format(
      sys.props("java.library.path"),
      pathToMesosLibs
    ),

    fork in Test := true
  )
}

// vim: set ts=4 sw=4 et:
