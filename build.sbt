import org.scalajs.linker.interface.Report

import scala.sys.process._

ThisBuild / scalaVersion         := "2.13.3"
ThisBuild / description          := "Salesforce Apex static analysis toolkit"
ThisBuild / organization         := "io.github.apex-dev-tools"
ThisBuild / organizationHomepage := Some(url("https://github.com/apex-dev-tools/apex-ls"))
ThisBuild / homepage := Some(url("https://github.com/apex-dev-tools/apex-ls"))
ThisBuild / licenses := List(
  "BSD-3-Clause" -> new URL("https://opensource.org/licenses/BSD-3-Clause")
)
ThisBuild / developers := List(
  Developer(
    "apexdevtools",
    "Apex Dev Tools Team",
    "apexdevtools@gmail.com",
    url("https://github.com/apex-dev-tools")
  )
)
ThisBuild / versionScheme := Some("strict")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val build = taskKey[File]("Build artifacts")
lazy val Dev   = config("dev") extend Compile

// Don't publish root
publish / skip := true

lazy val apexls = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .configs(Dev)
  .settings(
    name := "apex-ls",
    scalacOptions += "-deprecation",
    libraryDependencies ++= Seq(
      "io.github.apex-dev-tools" %%% "outline-parser"                         % "1.0.0",
      "com.github.nawforce"      %%% "scala-json-rpc"                         % "1.0.1",
      "com.github.nawforce"      %%% "scala-json-rpc-upickle-json-serializer" % "1.0.1",
      "com.lihaoyi"              %%% "upickle"                                % "1.2.0",
      "org.scalatest"            %%% "scalatest"                              % "3.2.0" % Test
    )
  )
  .jvmSettings(
    build       := buildJVM.value,
    Test / fork := true,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules"  %% "scala-xml"                  % "1.3.0",
      "org.scala-lang.modules"  %% "scala-parallel-collections" % "1.0.0",
      "org.scala-js"            %% "scalajs-stubs"              % "1.0.0",
      "io.github.apex-dev-tools" % "apex-parser"                % "3.0.0",
      "io.github.apex-dev-tools" % "vf-parser"                  % "1.0.0",
      "org.antlr"                % "antlr4-runtime"             % "4.8-1",
      "io.github.apex-dev-tools" % "sobject-types"              % "55.0.0",
      "io.github.apex-dev-tools" % "standard-types"             % "55.0.0",
      "com.github.nawforce"      % "uber-apex-jorje"            % "1.0.0" % Test,
      "com.google.jimfs"         % "jimfs"                      % "1.1"   % Test
    )
  )
  .jsSettings(
    build       := buildJs(Compile / fullLinkJS).value,
    Dev / build := buildJs(Compile / fastLinkJS).value,
    libraryDependencies ++= Seq("net.exoego" %%% "scala-js-nodejs-v14" % "0.12.0"),
    scalaJSUseMainModuleInitializer := false,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val buildJVM = Def.task {
  // Copy jar deps to target for easier testing
  val targetDir = crossTarget.value
  val files = (Compile / dependencyClasspath).value.files map { f =>
    f -> targetDir / f.getName
  }
  IO.copy(files, CopyOptions().withOverwrite(true))

  (Compile / Keys.`package`).value
}

def buildJs(jsTask: TaskKey[Attributed[Report]]): Def.Initialize[Task[File]] = Def.task {
  def exec: (String, File) => Unit = run(streams.value.log)(_, _)

  // Depends on scalaJS fast/full linker output
  jsTask.value

  val targetDir  = crossTarget.value
  val targetFile = (jsTask / scalaJSLinkerOutputDirectory).value / "main.js"
  val npmDir     = baseDirectory.value / "npm"

  val files: Map[File, File] = Map(
    // Update target with NPM modules (for testing)
    npmDir / "package.json" -> targetDir / "package.json",
    // Add source to NPM
    targetFile -> npmDir / "src/apexls.js"
  )

  IO.copy(files, CopyOptions().withOverwrite(true))

  // Install modules in NPM
  exec("npm i", npmDir)

  // Update target with NPM modules (for testing)
  IO.copyDirectory(
    npmDir / "node_modules",
    targetDir / "node_modules",
    CopyOptions().withOverwrite(true)
  )

  targetFile
}

// Run a command and log to provided logger
def run(log: ProcessLogger)(cmd: String, cwd: File): Unit = {
  val shell: Seq[String] =
    if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
  val exitCode = Process(shell :+ cmd, cwd) ! log
  if (exitCode > 0) {
    log.err(s"Process exited with non-zero exit code: $exitCode")
    sys.exit(exitCode)
  }
}