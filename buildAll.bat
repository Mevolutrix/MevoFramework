call sbt sme/assembly
call sbt sbe/assembly
call sbt sme4CMS/package
copy SME\target\scala-2.11\sme-assembly-0.1.0.jar  mevo.runtime /y
copy SBE\target\scala-2.11\sbe-assembly-0.1.0.jar  mevo.runtime /y
copy target\scala-2.11\*.jar  D:\IdeaProjects\mevo.runtime\mevo-init-version.jar /y