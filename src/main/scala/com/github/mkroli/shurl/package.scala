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
package com.github.mkroli

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Promise
import scala.language.implicitConversions
import scala.util.Try

import com.github.nscala_time.time.Imports.DateTime
import com.google.common.util.concurrent.ListenableFuture
import com.typesafe.config.Config

import spray.http.Uri

package object shurl {
  implicit def functionToRunnable[T](f: () => T) = new Runnable {
    override def run = f()
  }

  implicit def listenableFutureToFuture[T](lf: ListenableFuture[T])(implicit c: ExecutionContextExecutor) = {
    val promise = Promise[T]
    lf.addListener(() => promise.success(lf.get), c)
    promise.future
  }

  implicit class RichConfig(conf: Config) {
    def getOptionalString(path: String) = Try(conf.getString(path)).toOption
  }

  implicit class RichString(uri: Uri) {
    def /(segment: String) = uri.withPath(uri.path / segment)
  }

  implicit class RichDateTime(d: DateTime) {
    def withMillisOfHour(millis: Int) = d
      .withMinuteOfHour((millis / 1000 / 60) % 60)
      .withSecondOfMinute((millis / 1000) % 60)
      .withMillisOfSecond(millis % 1000)
  }
}
