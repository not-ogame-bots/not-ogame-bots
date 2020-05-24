import sbt.Keys.libraryDependencies

val seleniumVersion = "3.141.59"
val refinedVersion = "0.9.14"
val pureConfigVersion = "0.12.3"
val enumeratumVersion = "1.6.1"

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
      "com.beachape" %% "enumeratum" % enumeratumVersion,
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
    libraryDependencies ++= seleniumDeps,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.7" % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(core)

lazy val facts: Project = (project in file("facts"))
  .settings(commonSettings)
  .settings(name := "facts")
  .dependsOn(core)

lazy val ghostbuster: Project = (project in file("ghostbuster"))
  .settings(commonSettings)
  .settings(
    name := "ghostbuster",
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion,
      "eu.timepit" %% "refined-pureconfig" % refinedVersion,
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0",
      "com.lihaoyi" %% "pprint" % "0.5.6",
      "io.monix" %% "monix" % "3.2.1",
      "org.scalameta" %% "munit" % "0.7.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(selenium, facts)

lazy val ordon: Project = (project in file("ordon"))
  .settings(commonSettings)
  .settings(
    name := "ordon",
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-enumeratum" % pureConfigVersion,
      "eu.timepit" %% "refined-pureconfig" % refinedVersion,
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
