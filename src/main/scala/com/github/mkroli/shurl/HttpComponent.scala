/*
 * Copyright 2014 Michael Krolikowski
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
package com.github.mkroli.shurl

import java.net.URL
import java.util.Date

import scala.language.postfixOps

import org.json4s.DefaultFormats

import com.github.nscala_time.time.Imports.DateTime
import com.github.nscala_time.time.Imports.richAbstractInstant

import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http.HttpEntity
import spray.http.HttpHeaders
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.http.Uri.Path
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling.Unmarshaller
import spray.routing.ExceptionHandler
import spray.routing.HttpServiceActor

case class Visits(last30Days: List[VisitPair], last24Hours: List[VisitPair])
case class VisitPair(offset: Date, count: Long)

trait HttpComponent {
  self: AkkaComponent with DatabaseComponent =>

  val httpBindAddress = "0.0.0.0"

  val httpBindPort = 8080

  lazy val httpActor = actorSystem.actorOf(Props(new HttpActor))

  class HttpActor extends HttpServiceActor {
    object JsonSupport extends Json4sSupport {
      override def json4sFormats = DefaultFormats
    }

    import context.dispatcher

    IO(Http)(actorSystem) ! Http.Bind(
      httpActor,
      interface = httpBindAddress,
      port = httpBindPort)

    implicit def exceptionHandler = ExceptionHandler {
      case UrlNotFoundException => ctx => ctx.complete(StatusCodes.NotFound, "")
      case StoreUrlAlreadyExistsException => ctx => ctx.complete(StatusCodes.Conflict, "")
    }

    val randomIdChars = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toVector
    def generateRandomId() = (1 to 7)
      .map(_ => (math.random * randomIdChars.size).toInt)
      .map(randomIdChars)
      .mkString

    implicit val UrlUnmarshaller = Unmarshaller[URL](MediaTypes.`text/plain`) {
      case HttpEntity.NonEmpty(contentType, data) => new URL(data.asString)
    }

    lazy val redirect = get {
      path(Segment) { id =>
        onSuccess(databaseActor ? GetUrl(id)) {
          case GetUrlResult(_, url) => respondWithHeader(HttpHeaders.Location(url.toString)) {
            databaseActor ! VisitUrl(id)
            complete(StatusCodes.MovedPermanently, "")
          }
        }
      }
    }

    lazy val api = get {
      path("visits" / Segment) { id =>
        onSuccess(databaseActor ? GetVisits(id)) {
          case GetVisitsResult(_, visits) =>
            import JsonSupport._
            val perHour = visits.map {
              case (offset, count) => offset -> count
            }
            val perDay = perHour.groupBy {
              case (offset, _) => new DateTime(offset).withMillisOfDay(0).date
            }.map {
              case (offset, l) => offset -> l.map(_._2).sum
            }

            val last30Days = (29 to 0 by -1).map { day =>
              val offset = DateTime.now.withMillisOfDay(0).minusDays(day).date
              VisitPair(offset, perDay.getOrElse(offset, 0L))
            }
            val last24Hours = (23 to 0 by -1).map { hour =>
              val offset = DateTime.now.withMillisOfHour(0).minusHours(hour).date
              VisitPair(offset, perHour.getOrElse(offset, 0L))
            }

            complete(Visits(last30Days.toList, last24Hours.toList))
        }
      }
    } ~ put {
      requestUri { reqUri =>
        entity(as[URL]) { url: URL =>
          def shorten(id: String) = onSuccess(databaseActor ? StoreUrl(id, url)) { _ =>
            respondWithHeader(HttpHeaders.Location(reqUri.withPath(Path / id).toString)) {
              complete(StatusCodes.Created, "")
            }
          }
          pathEnd {
            shorten(generateRandomId)
          } ~ path(Segment) { id =>
            shorten(id)
          }
        }
      }
    }

    override def receive = runRoute {
      pathPrefix("api") {
        api
      } ~ pathSingleSlash {
        getFromResource("com/github/mkroli/shurl/index.html")
      } ~ path("stats" / Segment) { _ =>
        getFromResource("com/github/mkroli/shurl/index.html")
      } ~ getFromResourceDirectory("com/github/mkroli/shurl") ~ redirect
    }
  }
}
