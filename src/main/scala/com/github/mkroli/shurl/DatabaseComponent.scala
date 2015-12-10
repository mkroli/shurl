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

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.breakOut

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.querybuilder.{ QueryBuilder => q }
import com.github.nscala_time.time.Imports.DateTime
import com.github.nscala_time.time.Imports.richAbstractInstant

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe

trait DatabaseComponent {
  self: AkkaComponent =>

  def cassandraSeeds: List[String]

  case class GetUrl(id: String)
  case class GetUrlResult(id: String, url: URL)
  case object UrlNotFoundException extends RuntimeException

  case class VisitUrl(id: String)

  case class GetVisits(id: String)
  case class GetVisitsResult(id: String, visits: Map[Date, Long])

  case class StoreUrl(id: String, url: URL)
  case object StoreUrlResult
  case object StoreUrlAlreadyExistsException extends RuntimeException

  lazy val databaseActor = actorSystem.actorOf(Props(new DatabaseActor))

  lazy val cluster = {
    val cluster = (Cluster.builder /: cassandraSeeds)(_ addContactPoint _).build.connect
    cluster.execute("""
      CREATE KEYSPACE IF NOT EXISTS shurl WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
      }
    """)

    cluster.execute("""
      CREATE COLUMNFAMILY IF NOT EXISTS shurl.urls (
        id text,
        url text,
        PRIMARY KEY (id)
      )
    """)

    cluster.execute("""
      CREATE COLUMNFAMILY IF NOT EXISTS shurl.visits (
        id text,
        time timestamp,
        visits counter,
        PRIMARY KEY (id, time)
      )
    """)

    cluster
  }

  class DatabaseActor extends Actor {
    import context.dispatcher

    val fetchQuery = cluster.prepare(
      q.select("url")
        .from("shurl", "urls")
        .where(q.eq("id", q.bindMarker)))

    val visitQuery = cluster.prepare(
      q.update("shurl", "visits")
        .`with`(q.incr("visits"))
        .where(q.eq("id", q.bindMarker))
        .and(q.eq("time", q.bindMarker)))

    val fetchVisitsQuery = cluster.prepare(
      q.select("time", "visits")
        .from("shurl", "visits")
        .where(q.eq("id", q.bindMarker))
        .and(q.gt("time", DateTime.now.withMillisOfHour(0).minusDays(30).date)))

    val storeQuery = cluster.prepare(
      q.insertInto("shurl", "urls")
        .ifNotExists
        .value("id", q.bindMarker)
        .value("url", q.bindMarker))

    override def receive = {
      case GetUrl(id) =>
        cluster.executeAsync(fetchQuery.bind(id)).map { row =>
          if (row.isExhausted) throw UrlNotFoundException
          else GetUrlResult(id, new URL(row.one.getString("url")))
        }.pipeTo(sender)
      case VisitUrl(id) =>
        cluster.executeAsync(visitQuery.bind(id, DateTime.now.withMillisOfHour(0).date))
      case GetVisits(id) =>
        cluster.executeAsync(fetchVisitsQuery.bind(id)).map { row =>
          if (row.isExhausted) throw UrlNotFoundException
          else {
            val visits: Map[Date, Long] = row.all.toList.map { row =>
              row.getDate("time") -> row.getLong("visits")
            }(breakOut)
            GetVisitsResult(id, visits)
          }
        }.pipeTo(sender)
      case StoreUrl(id, url) =>
        cluster.executeAsync(storeQuery.bind(id, url.toString)).map(_.one.getBool("[applied]")).map {
          case true => StoreUrlResult
          case _ => throw StoreUrlAlreadyExistsException
        }.pipeTo(sender)
    }
  }
}
