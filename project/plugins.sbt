//logLevel := Level.Warn

//addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.4.0")

//resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

//resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")
