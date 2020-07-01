import com.typesafe.sbt.packager.docker.ExecCmd
import sbt.Keys.libraryDependencies

val seleniumVersion = "3.141.59"
val pureConfigVersion = "0.12.3"
val enumeratumVersion = "1.6.1"
val http4sVersion = "0.21.4"
val circeVersion = "0.13.0"
val tapirVersion = "0.14.4"

val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
)

lazy val commonSettings = commonSmlBuildSettings ++ acyclicSettings ++ splainSettings ++ Seq(
  scalaVersion := "2.13.1",
  libraryDependencies += compilerPlugin("com.softwaremill.neme" %% "neme-plugin" % "0.0.5")
)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.1.3",
      "co.fs2" %% "fs2-core" % "2.2.1",
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.softwaremill.common" %% "tagging" % "2.2.1"
    )
  )

val seleniumDeps = Seq(
  "org.seleniumhq.selenium" % "selenium-java",
  "org.seleniumhq.selenium" % "selenium-support",
  "org.seleniumhq.selenium" % "selenium-firefox-driver"
).map(_ % seleniumVersion)

lazy val selenium: Project = (project in file("selenium"))
  .settings(commonSettings)
  .settings(
    name := "selenium",
    libraryDependencies ++= seleniumDeps,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.7" % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(core)

lazy val facts: Project = (project in file("facts"))
  .settings(commonSettings)
  .settings(name := "facts")
  .dependsOn(core)

lazy val dockerSettings = Seq(
  dockerExposedPorts := Seq(8080),
  dockerBaseImage := "selenium/standalone-firefox",
  dockerUsername := Some("notogamebots"),
  packageName in Docker := "ghostbot",
  dockerCommands := {
    dockerCommands.value.flatMap {
      case ep @ ExecCmd("ENTRYPOINT", _*) =>
        Seq(
          ExecCmd("ENTRYPOINT", "/opt/docker/docker-entrypoint.sh" :: ep.args.toList: _*)
        )
      case other => Seq(other)
    }
  },
  mappings in Docker ++= {
    val scriptDir = baseDirectory.value / "scripts"
    val entrypointScript = scriptDir / "docker-entrypoint.sh"
    val entrypointScriptTargetPath = "/opt/docker/docker-entrypoint.sh"
    Seq(
      entrypointScript -> entrypointScriptTargetPath,
      baseDirectory.value / "../selenium/geckodriver" -> "/opt/docker/selenium/geckodriver"
    )
  },
  dockerUpdateLatest := true
)

lazy val ghostbuster: Project = (project in file("ghostbuster"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "ghostbuster",
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion,
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0",
      "com.lihaoyi" %% "pprint" % "0.5.6",
      "io.monix" %% "monix" % "3.2.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.google.firebase" % "firebase-admin" % "6.8.1",
      "org.typelevel" %% "cats-free" % "2.1.1",
      "com.github.cb372" %% "cats-retry" % "1.1.1",
      "org.scalameta" %% "munit" % "0.7.7" % Test
    ) ++ jsonDependencies,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(selenium, facts)

lazy val ordon: Project = (project in file("ordon"))
  .settings(commonSettings)
  .settings(
    name := "ordon",
    libraryDependencies ++= Seq(
      "com.squareup.retrofit2" % "converter-gson" % "2.1.0",
      "com.squareup.retrofit2" % "retrofit" % "2.9.0",
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion,
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0",
      "org.scalameta" %% "munit" % "0.7.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(selenium, facts)

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "not-ogame-bots")
  .aggregate(core, selenium, facts, ghostbuster, ordon)
