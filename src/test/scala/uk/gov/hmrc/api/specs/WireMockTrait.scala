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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.{interval, timeout}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.concurrent.duration.FiniteDuration

trait WireMockTrait extends BeforeAndAfterEach with BeforeAndAfterAll {

  this: Suite =>

  val wireMockServerConfig: Config = ConfigFactory.load()
  private val wireMockServerPort   = wireMockServerConfig.getInt("mock.server.port")
  lazy val wireMockServer          = new WireMockServer(WireMockConfiguration.wireMockConfig().port(wireMockServerPort))

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor("127.0.0.1", wireMockServerPort)
  }

  override def beforeEach(): Unit = {

    stubFor(
      post("/write/audit")
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

    stubFor(
      post("/write/audit/merged")
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )
  }

  override def afterEach(): Unit =
    wireMockServer.resetAll()

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  def delayedFunction[T](duration: FiniteDuration)(f: => T): T =
    eventually(timeout(Span(duration.toSeconds, Seconds)), interval(Span(100, Millis)))(f)
}
