import sbt.*

object Dependencies {

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "api-test-runner" % "0.7.0" % Test,
    "com.github.tomakehurst" % "wiremock"        % "3.0.1" % Test
  )

}
