import sbt.Keys.libraryDependencies

val seleniumVersion = "3.141.59"
val refinedVersion = "0.9.14"

lazy val commonSettings = commonSmlBuildSettings ++ acyclicSettings ++ splainSettings ++ Seq(
  scalaVersion := "2.13.1"
)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      compilerPlugin("com.softwaremill.neme" %% "neme-plugin" % "0.0.5"),
      "org.typelevel" %% "cats-effect" % "2.1.3",
      "com.beachape" %% "enumeratum" % "1.6.1",
      "eu.timepit" %% "refined" % refinedVersion,
      "eu.timepit" %% "refined-cats" % refinedVersion
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
    libraryDependencies ++= seleniumDeps
  )
  .dependsOn(core)

lazy val facts: Project = (project in file("facts"))
  .settings(commonSettings)
  .settings(name := "facts")
  .dependsOn(core)

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "not-ogame-bots")
  .aggregate(core, selenium, facts)
