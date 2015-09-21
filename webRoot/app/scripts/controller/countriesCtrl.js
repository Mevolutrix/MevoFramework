/**
 * Created by Ming on 2015/2/10.
 */

dashboardApp.controller("CountryController", function ($scope, countriesService) {
    //$scope.model={
    //    message: "This is the test Message!!"
    //}
    countriesService.list(function (countries) {
      $scope.countries = countries;
    })
  })
.controller('CountryDetailCtrl', function ($scope, $routeParams, countriesService) {
    countriesService.find($routeParams.countryId, function (country) {
      $scope.country = country;
    });
  });
