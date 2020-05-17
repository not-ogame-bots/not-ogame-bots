import sbt.Keys.libraryDependencies

val seleniumVersion = "3.141.59"

lazy val commonSettings = commonSmlBuildSettings ++ acyclicSettings ++ splainSettings ++ Seq(
  scalaVersion := "2.13.1"
)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.3",
    libraryDependencies += "com.beachape" %% "enumeratum" % "1.6.1"
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

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "not-ogame-bots")
  .aggregate(core, selenium)
