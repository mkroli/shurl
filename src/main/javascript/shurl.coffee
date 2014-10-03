###
Copyright 2014 Michael Krolikowski

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
###

shurl = angular.module "shurl", ["ngRoute", "ngResource"]

shurl.config ["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  $locationProvider.html5Mode(true)

  $routeProvider
    .when "/stats/:id",
      templateUrl : "stats.html"
      controller : "StatsController"
    .otherwise
      templateUrl : "main.html"
      controller : "MainController"
]

shurl.factory "shurlBackend", ["$resource", ($resource) ->
  $resource "/api/:visits/:id", null,
    "shorten":
      "method" : "PUT"
      "headers" :
        "Content-Type" : "text/plain"
    "visits":
      "params":
        "visits" : "visits"
]
