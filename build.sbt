name := "jvm-matrix"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "0.11.2",
  "org.scalanlp" %% "breeze-natives" % "0.11.2",
  "com.github.fommil.netlib" % "all" % "1.1.2" pomOnly(),
  "com.googlecode.matrix-toolkits-java" % "mtj" % "1.0.5-SNAPSHOT",
  "org.ojalgo" % "ojalgo" % "38.2",
  ("org.apache.hadoop" % "hadoop-common" % "2.2.0").
    exclude("org.mortbay.jetty", "servlet-api").
    exclude("commons-beanutils", "commons-beanutils-core").
    exclude("commons-collections", "commons-collections").
    exclude("commons-logging", "commons-logging").
    exclude("com.esotericsoftware.minlog", "minlog").
    exclude("com.google.guava", "guava")
)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

test in assembly := {}

