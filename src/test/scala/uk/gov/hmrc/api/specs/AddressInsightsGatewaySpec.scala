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

import play.api.libs.json.{JsValue, Json}
import play.shaded.ahc.io.netty.handler.codec.http.{HttpHeaderNames, HttpResponseStatus}
import uk.gov.hmrc.api.client.HttpClientHelper
import uk.gov.hmrc.api.conf.TestEnvironment

class AddressInsightsGatewaySpec extends BaseSpec with HttpClientHelper {

  private val addressGatewayUrl              = TestEnvironment.url("address-gateway")
  private val addressGatewayUserAgent        = "address-gateway"
  private val headers: Seq[(String, String)] = Seq(
    HttpHeaderNames.CONTENT_TYPE.toString -> "application/json",
    HttpHeaderNames.USER_AGENT.toString   -> addressGatewayUserAgent
  )

  val requestedAddress: JsValue =
    Json.obj(
      "address" -> Json.obj(
        "addressLine1" -> "30-31",
        "postcode"     -> "BN2 1QB",
        "country"      -> "GB"
      )
    )

  Feature("Check the Address insights API") {

    Scenario("Get insights for an address that is in the database") {
      Given("I want to see insights for the given address")

      When("I use the address insights api")
      val actualResponseMaybe =
        post(
          addressGatewayUrl + "/reputation/sa-reg",
          requestedAddress,
          headers: _*
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
      (insightsNode \ "byLocationRef" \ "count").as[Int] shouldBe 0
      (insightsNode \ "byPostCode" \ "count").as[Int]    shouldBe 6
    }
  }

  Feature("Check the Address cache API") {
    Scenario("Add an address to the cache") {
      Given("I want to add an address to the short term cache")

      When("I use the address insights cache api")
      val cacheAddress =
        Json.obj(
          "address" -> Json.obj(
            "addressLine1" -> "30-31",
            "postcode"     -> "BN2 1QB",
            "country"      -> "GB"
          ),
          "caseId"  -> "1234567890"
        )

      val actualResponse =
        post(
          addressGatewayUrl + "/cache",
          cacheAddress,
          headers: _*
        )

      actualResponse.status shouldBe HttpResponseStatus.NO_CONTENT.code()
    }

    Scenario("Get insights for an address that is also in the cache") {
      When("I use the address insights api to get insights for a matching address")
      val actualResponseMaybe =
        post(
          addressGatewayUrl + "/reputation/sa-reg",
          requestedAddress,
          headers: _*
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
      (insightsNode \ "byLocationRef" \ "count").as[Int] shouldBe 0
      (insightsNode \ "byPostCode" \ "count").as[Int]    shouldBe 6
    }
  }
}
