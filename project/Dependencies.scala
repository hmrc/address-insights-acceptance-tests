import sbt.*

object Dependencies {

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "api-test-runner" % "0.10.0",
    "org.wiremock" % "wiremock"        % "3.13.1"
  ).map(_ % Test)

}
