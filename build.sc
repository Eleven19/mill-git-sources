// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
// Run integration tests with mill
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.6.0`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`
import mill._
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Command, Target, Task, TaskModule}
import mill.scalalib._
import mill.scalalib.publish._
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._
import os.Path

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String
  def testWithMill: Seq[String]

  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  val millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.10"
  val scoverageVersion = "1.4.11"
  val scoveragePlugin = ivy"org.scoverage:::scalac-scoverage-plugin:${scoverageVersion}"
  val scoverageRuntime = ivy"org.scoverage::scalac-scoverage-runtime:${scoverageVersion}"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}

object Deps_0_10_0 extends Deps {
  override def millPlatform = "0.10"
  override def millVersion = "0.10.0" // scala-steward:off
  override def scalaVersion = "2.13.8"
  override def testWithMill = Seq(millVersion)
}

object Deps_0_9 extends Deps {
  override def millPlatform = "0.9"
  override def millVersion = "0.9.3" // scala-steward:off
  override def scalaVersion = "2.13.8"
  override def testWithMill =
    Seq("0.9.12", "0.9.11", "0.9.10", "0.9.9", "0.9.8", "0.9.7", "0.9.6", "0.9.5", "0.9.4", millVersion)
}

val crossDeps = Seq(Deps_0_10_0, Deps_0_9)
val millApiVersions = crossDeps.map(x => x.millPlatform -> x)
val millItestVersions = crossDeps.flatMap(x => x.testWithMill.map(_ -> x))

/** Shared configuration. */
trait BaseModule extends CrossScalaModule with PublishModule with ScoverageModule {
  def sonatypeUri: String = "https://s01.oss.sonatype.org/service/local"

  def sonatypeSnapshotUri: String = "https://s01.oss.sonatype.org/content/repositories/snapshots"

  def millApiVersion: String
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)
  def crossScalaVersion = deps.scalaVersion
  override def artifactSuffix: T[String] = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"

  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }

  def publishVersion = VcsVersion.vcsState().format()

  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8")

  def pomSettings = T {
    PomSettings(
      description = "Mill plugin to derive a version from (last) git tag and edit state",
      organization = "io.github.eleven19",
      url = "https://github.com/eleven19/mill-git-sources",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("eleven19", "mill-git-sources"),
      developers = Seq(Developer("DamianReeves", "Damian Reeves", "https.//github.com/DamianReeves"))
    )
  }

  override def scoverageVersion = deps.scoverageVersion
  // we need to adapt to changed publishing policy - patch-level
  override def scoveragePluginDep = T {
    deps.scoveragePlugin
  }

  trait Tests extends ScoverageTests

}

/* The actual mill plugin compilied against different mill APIs. */
object core extends Cross[CoreCross](millApiVersions.map(_._1): _*)
class CoreCross(override val millApiVersion: String) extends BaseModule {

  override def artifactName = "mill-git-sources"

  override def skipIdea: Boolean = deps != crossDeps.head

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  object test extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(deps.scalaTest)
  }
}

/** Integration tests. */
object itest extends Cross[ItestCross](millItestVersions.map(_._1): _*) with TaskModule {
  override def defaultCommandName(): String = "test"
  def testCached: T[Seq[TestCase]] = itest(millItestVersions.map(_._1).head).testCached
  def test(args: String*): Command[Seq[TestCase]] = itest(millItestVersions.map(_._1).head).test()
}
class ItestCross(millItestVersion: String) extends MillIntegrationTestModule {

  val millApiVersion = millItestVersions.toMap.apply(millItestVersion).millPlatform
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)

  override def millSourcePath: Path = super.millSourcePath / os.up
  override def millTestVersion = millItestVersion
  override def pluginsUnderTest = Seq(core(millApiVersion))

  /** Replaces the plugin jar with a scoverage-enhanced version of it. */
  override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p                  => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    super.testInvocations().map { case (pr, _) =>
      pr -> Seq(TestInvocation.Targets(Seq("-d", "verify")))
    }
  }

  override def perTestResources = T.sources { Seq(generatedSharedSrc()) }
  def generatedSharedSrc = T {
    os.write(
      T.dest / "shared.sc",
      s"""import $$ivy.`${deps.scoverageRuntime.dep.module.organization.value}::${deps.scoverageRuntime.dep.module.name.value}:${deps.scoverageRuntime.dep.version}`
         |import $$ivy.`com.eed3si9n.expecty::expecty:0.15.4`
         |""".stripMargin
    )
    PathRef(T.dest)
  }

}
