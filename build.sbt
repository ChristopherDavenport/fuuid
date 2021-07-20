import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala213 = "2.13.6"
val Scala212 = "2.12.14"
val Scala3 = "3.0.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)

ThisBuild / licenses := Seq("MIT" -> new java.net.URL("http://opensource.org/licenses/MIT"))

ThisBuild / startYear := Some(2018)
ThisBuild / developers := List(
  Developer(
    "christopherdavenport",
    "Christopher Davenport",
    "chris@christopherdavenport.tech",
    url("https://christopherdavenport.github.io/")
  ),
  Developer(
    "JesusMtnez",
    "Jesús Martínez-B. H.",
    "jesusmartinez93@gmail.com",
    url("https://jesusmtnez.es/")
  )
)

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "scalafmt",
    "Scalafmt",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Sbt(List("scalafmtCheckAll", "scalafmtSbtCheck"), name = Some("Scalafmt"))
    ),
    // Awaiting release of https://github.com/scalameta/scalafmt/pull/2324/files
    scalas = List(Scala3)
  )
)

def crossCompileDirs(scalaVersion: String, baseDirectory: File) = {
  val major = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => "-2"
    case _ => "-3"
  }
  List(CrossType.Pure, CrossType.Full).flatMap(
    _.sharedSrcDir(baseDirectory, "main").toList.map(f => file(f.getPath + major))
  )
}

lazy val fuuid = project
  .in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .aggregate(coreJS, coreJVM, doobie, http4s, circeJS, circeJVM)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(commonSettings)
  .settings(
    Compile / unmanagedSourceDirectories ++= crossCompileDirs(
      scalaVersion.value,
      baseDirectory.value
    )
  )
  .settings(
    name := "fuuid"
  )

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val doobie = project
  .in(file("modules/doobie"))
  .settings(commonSettings)
  .settings(
    name := "fuuid-doobie",
    libraryDependencies ++= Seq(
      "org.tpolecat"                           %% "doobie-core"      % doobieV,
      "org.tpolecat"                           %% "doobie-postgres"  % doobieV         % Test,
      "org.tpolecat"                           %% "doobie-h2"        % doobieV         % Test,
      "org.tpolecat"                           %% "doobie-munit"     % doobieV         % Test,
      "org.typelevel"                          %% "discipline-munit" % disciplineMunit % Test,
      "org.scalameta"                          %% "munit"            % munitV          % Test,
      "org.scalameta"                          %% "munit-scalacheck" % munitV          % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCE3V          % Test,
      ("com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersV % Test)
        .cross(CrossVersion.for3Use2_13)
    ),
    Test / parallelExecution := false // Needed due to a driver initialization deadlock between Postgres and H2
  )
  .dependsOn(coreJVM % "compile->compile;test->test")

lazy val circe = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/circe"))
  .settings(commonSettings)
  .settings(
    name := "fuuid-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeV
    )
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeV % Test
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val circeJS = circe.js
lazy val circeJVM = circe.jvm

lazy val http4s = project
  .in(file("modules/http4s"))
  .settings(commonSettings)
  .settings(
    name := "fuuid-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % http4sV,
      "org.http4s" %% "http4s-dsl"  % http4sV % Test
    )
  )
  .dependsOn(coreJVM % "compile->compile;test->test")

lazy val site = project
  .in(file("modules/site"))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .enablePlugins(DavenverseMicrositePlugin)
  .dependsOn(coreJVM, http4s, doobie, circeJVM)
  .settings(
    micrositeDescription := "Functional UUID's"
  )

val catsV = "2.6.1" //https://github.com/typelevel/cats/releases
val catsEffectV = "3.1.1" //https://github.com/typelevel/cats-effect/releases
val circeV = "0.14.1" //https://github.com/circe/circe/releases
val http4sV = "1.0.0-M23" //https://github.com/http4s/http4s/releases
val doobieV = "1.0.0-M5" //https://github.com/tpolecat/doobie/releases
val scalaJavaTimeV = "2.3.0" // https://github.com/cquiroz/scala-java-time/releases
val testcontainersV = "0.39.5"
val munitV = "0.7.26"
val munitCE3V = "1.0.5"
val disciplineMunit = "1.0.9"

// General Settings
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "cats-effect"         % catsEffectV,
    "org.typelevel" %%% "cats-laws"           % catsV           % Test,
    "org.typelevel" %%% "discipline-munit"    % disciplineMunit % Test,
    "org.scalameta" %%% "munit"               % munitV          % Test,
    "org.scalameta" %%% "munit-scalacheck"    % munitV          % Test,
    "org.typelevel" %%% "munit-cats-effect-3" % munitCE3V       % Test
  ),
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => Nil
    case _ =>
      Seq(
        scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
        scalaOrganization.value % "scala-reflect"  % scalaVersion.value % Provided,
        compilerPlugin("org.typelevel" % "kind-projector"     % "0.13.0" cross CrossVersion.full),
        compilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1")
      )
  }),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _ => Nil
  }),
  Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
)
