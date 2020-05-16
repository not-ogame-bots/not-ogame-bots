lazy val commonSettings = commonSmlBuildSettings ++ acyclicSettings ++ splainSettings ++ Seq(
  scalaVersion := "2.13.1"
)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.3"
  )

lazy val selenium: Project = (project in file("selenium"))
  .settings(commonSettings)
  .settings(
    name := "selenium"
  )
  .dependsOn(core)

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "not-ogame-bots")
  .aggregate(core, selenium)
