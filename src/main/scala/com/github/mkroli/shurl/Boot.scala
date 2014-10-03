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

import java.util.Locale
import java.util.TimeZone

import scala.collection.JavaConverters.asScalaBufferConverter

import com.typesafe.scalalogging.StrictLogging

object Boot extends App with StrictLogging {
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  Locale.setDefault(Locale.US)

  val applicationContext = new AnyRef with AkkaComponent with HttpComponent with DatabaseComponent with ConfigurationComponent {
    override def cassandraSeeds = config.getStringList("cassandra.seeds").asScala.toList

    override val httpBindAddress = config.getString("http.bind.address")

    override val httpBindPort = config.getInt("http.bind.port")
  }
  applicationContext.cluster
  applicationContext.httpActor
  logger.info("Started")
}
