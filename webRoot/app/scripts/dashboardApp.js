/**
 * Created by Ming on 2015/1/24.
 */

var dashboardApp = angular.module("dashboardApp", ['ui.bootstrap',"ngRoute",'validator', 'validator.rules', 'ngResource']);

dashboardApp.config(function ($routeProvider) {
    $routeProvider
        .when('/countries', {
            templateUrl: 'views/templates/countries/country-list.html',
            controller: 'CountryController'
        })

        .when('/users', {
          templateUrl: 'views/templates/users/userList.html',
          controller: 'UserListController'
        })
        .when('/countries/:countryId', {
          templateUrl: "views/templates/countries/country-detail.html",
          controller: "CountryDetailCtrl"
        })
        .when('/users/:userId', {
          templateUrl: "views/templates/users/userDetail.html",
          controller: "UserDetailController"
        })

        //=========================================================
        .when('/forms/create', {
            templateUrl: 'views/create.html',
            controller: 'CreateCtrl'
        })
        .when('/forms/:id/view', {
            templateUrl: 'views/view.html',
            controller: 'ViewCtrl'
        })
        .when('/list', {
            templateUrl: 'views/list.html',
            controller: 'ViewCtrl'
        })
        .when('/:id/list', {
            templateUrl: 'views/list.html',
            controller: 'ViewCtrl'
        })
        .when('/:id/insert', {
            templateUrl: 'views/insert.html',
            controller: 'InsertCtrl'
        })
        .when('/:id/:dataID', {
            templateUrl: 'views/insert.html',
            controller: 'UpdateCtrl'
        })
        .when('/forms/:id', {
            templateUrl: 'views/view.html',
            controller: 'DetailCtrl'
        })

        .when('/tabDemo', {
            templateUrl: 'views/tabdemo.html',
            controller: 'TabsDemoCtrl'
        })

        .when('/designer', {
            templateUrl: 'views/designer.html',
            controller: 'DesignerCtrl'
        })

        .when('/preCreate', {
            templateUrl: 'views/preCreate.html',
            controller: 'PreCreateCtrl'
        })

        //======================================================
        .otherwise({
            redirectTo: '/'
        });
});



dashboardApp.controller("AppController", function ($scope) {
    $scope.model = {
        message: "This is the test Message!!"
    }
})

dashboardApp.controller("sideBarController", function ($scope) {
  $scope.selectedItem=null;
  //$scope.varActive=selectedItem==
})

dashboardApp.filter('encodeURI', function () {
    return window.encodeURI;
})