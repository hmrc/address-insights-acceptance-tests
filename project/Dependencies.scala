import sbt.*

object Dependencies {

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "api-test-runner" % "0.5.0" % Test
  )

}
