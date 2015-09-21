var cmsApp=angular.module('cmsApp',['ngResource', 'ngRoute']).config(function ($routeProvider) {
    $routeProvider
        .when('/layout', {
            templateUrl: "/page/index.html",
            controller: ''
        })
        .when('/savePage', {
            templateUrl:function(){
                return '/page/load.html';
            },
            controller: ''
        })
        .when('/storeinfo', {
            templateUrl: 'page/storeinfo.html',
            controller: ''
        })
});

cmsApp.controller('cmsController' ,['$scope','schemaByName','schemaByNameById', function($scope) {
    $scope.id =3;
}])

