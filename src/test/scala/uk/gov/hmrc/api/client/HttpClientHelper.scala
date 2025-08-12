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

package uk.gov.hmrc.api.client

import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.StandaloneWSResponse
import uk.gov.hmrc.apitestrunner.http.HttpClient

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

trait HttpClientHelper extends HttpClient {

  private def await[T](f: => Future[T]): T = Await.result(f, 10.seconds)

  def post[T](url: String, body: T, headers: (String, String)*)(implicit writes: Writes[T]): StandaloneWSResponse =
    await(
      mkRequest(url)
        .withHttpHeaders(headers: _*)
        .post(Json.toJson(body))
    )
}
