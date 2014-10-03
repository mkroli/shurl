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

shurl = angular.module "shurl"

shurl.controller "MainController", ["$scope", "shurlBackend", ($scope, shurlBackend) ->
  $scope.link = ""
  $scope.linkHasError = false

  $scope.name = ""
  $scope.nameHasError = false

  $scope.shortened = null
  $scope.shortenedStats = null

  urlRegex = /^(.*)\/([^/]+)$/

  $scope.shorten = (link, id) ->
    shurlBackend.shorten
      "id" : id
    , link
    , (r, headers) ->
      $scope.shortened = headers("Location")
      [_, prefix, id] = urlRegex.exec($scope.shortened)
      $scope.shortenedStats = "#{prefix}/stats/#{id}"
      $scope.linkHasError = false
      $scope.nameHasError = false
    , (err) ->
      if (err.status == 400)
        $scope.linkHasError = true
      else if (err.status == 409)
        $scope.linkHasError = false
        $scope.nameHasError = true
      $scope.shortened = null
      $scope.shortenedStats = null
]
