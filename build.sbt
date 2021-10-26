organization := "com.wolt.osm"
homepage := Some(url("https://github.com/woltapp/spark-osm-datasource"))
scmInfo := Some(ScmInfo(url("https://github.com/woltapp/spark-osm-datasource"), "git@github.com:woltapp/spark-osm-datasource.git"))
developers := List(Developer("akashihi",
  "Denis Chaplygin",
  "denis.chaplygin@wolt.com",
  url("https://github.com/akashihi")))
licenses += ("GPLv3", url("https://www.gnu.org/licenses/gpl-3.0.txt"))
publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

name := "spark-osm-datasource"
version := "0.3.1"

//scalaVersion := "2.11.12"
scalaVersion := "2.12.10"

val mavenLocal = "Local Maven Repository" at "/opt/maven/repository"
resolvers += mavenLocal

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.1.1",
  "org.apache.spark" %% "spark-sql" % "3.1.1",
  "com.wolt.osm" % "parallelpbf" % "0.3.1",
  "org.scalatest" %% "scalatest" % "3.1.0" % "it,test",
  "org.scalactic" %% "scalactic" % "3.1.0" % "it,test"
)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)