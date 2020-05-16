lazy val commonSettings = commonSmlBuildSettings ++ acyclicSettings ++ splainSettings ++ Seq(
  scalaVersion := "2.13.1"
)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
  )


lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "not-ogame-bots")
  .aggregate(core)

