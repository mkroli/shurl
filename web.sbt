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
jsSettings

sourceDirectory in (Compile, JsKeys.js) <<= (sourceDirectory in Compile)(_ / "javascript")

includeFilter in (Compile, JsKeys.js) := "*.jsm"

resourceManaged in (Compile, JsKeys.js) <<= (resourceManaged in Compile)(_ / "com" / "github" / "mkroli" / "shurl")

resourceGenerators in Compile <+= (JsKeys.js in Compile)

compile in Compile <<= compile in Compile dependsOn (JsKeys.js in Compile)

webResourceSettings

webResources ++= Map(
  "bootstrap.min.css" -> "http://maxcdn.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css",
  "bootstrap-theme.min.css" -> "http://maxcdn.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css")

resourceGenerators in Compile <+= resolveWebResources

managedResourceDirectories in Compile <+= webResourcesBase

lessSettings

sourceDirectories in (Compile, LessKeys.less) <<= (sourceDirectory in Compile, webResourcesBase) { (srcDir, wrb) =>
  Seq(srcDir / "less", wrb)
}

resourceManaged in (Compile, LessKeys.less) <<= (resourceManaged in Compile)(_ / "com" / "github" / "mkroli" / "shurl")

resourceGenerators in Compile <+= (LessKeys.less in Compile)

LessKeys.less in Compile <<= LessKeys.less in Compile dependsOn (resolveWebResources in Compile)

compile in Compile <<= compile in Compile dependsOn (LessKeys.less in Compile)

unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
  Seq(
    base / "src" / "main" / "javascript",
    base / "src" / "main" / "less")
}
