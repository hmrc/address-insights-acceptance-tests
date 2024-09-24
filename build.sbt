lazy val root = (project in file("."))
  .settings(
    name := "address-insights-acceptance-tests",
    version := "0.1.0",
    scalaVersion := "2.13.13",
    libraryDependencies ++= Dependencies.test,
    (Compile / compile) := ((Compile / compile) dependsOn (Compile / scalafmtSbtCheck, Compile / scalafmtCheckAll)).value
  )
