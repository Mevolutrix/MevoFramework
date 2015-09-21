import sbt._
import scoverage.ScoverageSbtPlugin._

object SbtMultiBuild extends Build {
  lazy val mevolutrix = Project(
    id = "mevolutrix",
    base = file(".")
    ).aggregate(common,interface,util,entityStore,metadata,serviceconfig,sme,sbe) dependsOn(common,interface,util,entityStore,metadata,serviceconfig,sme,sbe)
 
  lazy val util = Project(
    id = "util",
    base = file("Utils")
  )

  lazy val sme = Project(
    id = "sme",
    base = file("SME")
  ) dependsOn (interface,metadata,entityStore)

  lazy val sbe = Project(
    id = "sbe",
    base = file("SBE")
  ) dependsOn (interface,metadata,serviceconfig,entityStore)

  lazy val serviceconfig = Project(
    id = "serviceconfig",
    base = file("ServiceConfig")
  ) dependsOn interface

  lazy val common = Project(
    id = "common",
    base = file("Common")
    ) dependsOn util

  lazy val interface = Project(
    id = "interface",
    base = file("Interface.EntityStore")
    ) dependsOn common

  lazy val entityStore = Project(
    id = "entityStore",
    base = file("EntityStore")
  ) dependsOn(interface,metadata)

  lazy val metadata = Project(
    id = "metadata",
    base = file("Metadata.EntityStore")
  ) dependsOn (interface,serviceconfig,common,util)

  lazy val sme4CMS = Project(
    id = "sme4CMS",
    base = file("sme4CMS")
  ) dependsOn (interface,metadata,common,entityStore)
}
