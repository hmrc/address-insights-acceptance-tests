/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.api.specs

import play.api.libs.json.Json
import play.shaded.ahc.io.netty.handler.codec.http.{HttpHeaderNames, HttpResponseStatus}
import uk.gov.hmrc.api.client.HttpClient

import scala.concurrent.Await
import scala.concurrent.duration.*
import uk.gov.hmrc.api.conf.TestEnvironment

class AddressInsightsGatewaySpec extends BaseSpec with HttpClient with WireMockTrait {
  private val addressGatewayUrl = TestEnvironment.url("address-gateway")

  val addressGatewayUserAgent = "address-gateway"

  val requestedAddress: String =
    """{ "address": {
      | "addressLine1": "30-31",
      | "postcode": "BN2 1QB",
      | "country": "GB"
      |}
      |}""".stripMargin

  Feature("Check the Address insights API") {

    Scenario("Get insights for an address that is in the database") {
      Given("I want to see insights for the given address")

      When("I use the address insights api")
      val actualResponseMaybe =
        Await.result(
          post(
            addressGatewayUrl + "/reputation/sa-reg",
            requestedAddress,
            HttpHeaderNames.CONTENT_TYPE.toString -> "application/json",
            HttpHeaderNames.USER_AGENT.toString   -> addressGatewayUserAgent
          ),
          5.seconds
        )

      Then("I am given the insights information for that address")
      actualResponseMaybe.status shouldBe HttpResponseStatus.OK.code()
      val bodyString = actualResponseMaybe.body
      val bodyJson   = Json.parse(bodyString)

      (bodyJson \ "lookbackDays").as[Int] shouldBe 120

      val assessmentNode = bodyJson \ "reputation" \ "assessment"
      (assessmentNode \ "action").as[String]        shouldBe "CHECK"
      (assessmentNode \ "reasons").as[List[String]] shouldBe List(
        "LONGER_TERM_RISK_120DAYS_POSTCODE_THRESHOLD_BREACHED"
      )

      val insightsNode = bodyJson \ "insights" \ "relationships" \ "occurrences"
      (insightsNode \ "byUprn" \ "count").as[Int]        shouldBe 0
      (insightsNode \ "byLocationRef" \ "count").as[Int] shouldBe 1
      (insightsNode \ "byPostCode" \ "count").as[Int]    shouldBe 9
    }
  }

  Feature("Check the Address cache API") {
    Scenario("Add an address to the cache") {
      Given("I want to add an address to the short term cache")

      When("I use the address insights cache api")
      val cacheAddress =
        """{ "address": {
          |     "addressLine1": "30-31",
          |     "postcode": "BN2 1QB",
          |     "country": "GB"
          |  },
          |  "caseId": "1234567890"
          |}""".stripMargin

      val actualResponse =
        Await.result(
          post(
            addressGatewayUrl + "/cache",
            cacheAddress,
            HttpHeaderNames.CONTENT_TYPE.toString -> "application/json",
            HttpHeaderNames.USER_AGENT.toString   -> addressGatewayUserAgent
          ),
          5.seconds
        )

      actualResponse.status shouldBe HttpResponseStatus.NO_CONTENT.code()
    }

    Scenario("Get insights for an address that is also in the cache") {
      When("I use the address insights api to get insights for a matching address")
      val actualResponseMaybe =
        Await.result(
          post(
            addressGatewayUrl + "/reputation/sa-reg",
            requestedAddress,
            HttpHeaderNames.CONTENT_TYPE.toString -> "application/json",
            HttpHeaderNames.USER_AGENT.toString   -> addressGatewayUserAgent
          ),
          5.seconds
        )

      Then("I am given the insights information for that address, including the cached address")
      actualResponseMaybe.status shouldBe HttpResponseStatus.OK.code()
      val bodyString = actualResponseMaybe.body
      val bodyJson   = Json.parse(bodyString)

      (bodyJson \ "lookbackDays").as[Int] shouldBe 120

      val assessmentNode = bodyJson \ "reputation" \ "assessment"
      (assessmentNode \ "action").as[String]      shouldBe "CHECK"
      (assessmentNode \ "reasons").as[List[String]] should contain theSameElementsAs List(
        "LONGER_TERM_RISK_120DAYS_POSTCODE_THRESHOLD_BREACHED"
      )

      val insightsNode = bodyJson \ "insights" \ "relationships" \ "occurrences"
      (insightsNode \ "byUprn" \ "count").as[Int]        shouldBe 0
      (insightsNode \ "byLocationRef" \ "count").as[Int] shouldBe 2
      (insightsNode \ "byPostCode" \ "count").as[Int]    shouldBe 10
    }
  }
}
