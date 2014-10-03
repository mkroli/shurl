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

shurl.controller "StatsController", ["$scope", "$routeParams", "shurlBackend", ($scope, $routeParams, shurlBackend) ->
  $scope.noGraphs = false

  extractData = (d, dateFormat) ->
    offsets : (Date.create(e.offset).format(dateFormat) for e in d)
    visits : (e.count for e in d)

  createChart = (id, data) ->
    ctx = document.getElementById(id).getContext("2d")
    data =
      labels : data.offsets
      datasets : [
        fillColor : "rgba(151,187,205,0.2)"
        strokeColor : "rgba(151,187,205,1)"
        pointColor : "rgba(151,187,205,1)"
        pointStrokeColor : "#fff"
        pointHighlightFill : "#fff"
        pointHighlightStroke : "rgba(151,187,205,1)"
        data : data.visits
      ]
    new Chart(ctx).Line(data, (responsive : true))

  updateChart = (chart, data) ->
    for i in [0..chart.datasets[0].points.length-1]
      chart.datasets[0].points[i].value = data.visits[i]
    chart.update()

  last24HoursChart = null
  last30DaysChart = null

  reload = ->
    shurlBackend.visits
      id : $routeParams.id
    , (r) ->
      $scope.noGraphs = false
      if(last24HoursChart)
        updateChart(last24HoursChart, extractData(r.last24Hours, "{HH}:00"))
      else
        last24HoursChart = createChart("last24HoursChart", extractData(r.last24Hours, "{HH}:00"))

      if(last30DaysChart)
        updateChart(last30DaysChart, extractData(r.last30Days, "{MM}-{dd}"))
      else
        last30DaysChart = createChart("last30DaysChart", extractData(r.last30Days, "{MM}-{dd}"))
    , (err) ->
      $scope.noGraphs = true

  reloadLoop = ->
    reload()
    setTimeout(reloadLoop, 5000)

  reloadLoop()
]
