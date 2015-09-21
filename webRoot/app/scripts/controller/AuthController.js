/**
 * Created by Ming on 2015/2/15.
 */

dashboardApp.controller('AuthController', function ($scope, $http, TokenStorage,$location) {
  $scope.authenticated = false;
  $scope.token; // For display purposes only

  $scope.init = function () {
    $http.defaults.xsrfHeaderName = 'X-CSRF-TOKEN';
    $http.defaults.xsrfCookieName = 'CSRF-TOKEN';

    $http.get('/rest/user/current').success(function (loginStatus) {
      //alert(loginStatus.status)
      if(loginStatus.status=="SUCCESS"){
          $scope.authenticated = true;
          $scope.username = loginStatus.loginUser.principal;
          // For display purposes only
          $scope.token = JSON.parse(atob(TokenStorage.retrieve().split('.')[0]));
      }
    }).error(function (loginStatus) {
      // console.log(loginStatus);
      if(loginStatus.status=="FAIL"){
        $scope.authenticated = false;
        TokenStorage.clear();
      }
        //for testing purpose set login status to true
        $scope.authenticated = true;
    });
  };

  $scope.login = function () {

    // console.log("start to login.....");

    $http.post('/rest/login', {
      username: $scope.username,
      password: $scope.password
    }).success(function (result, status, headers) {
      $scope.authenticated = true;
      TokenStorage.store(headers('X-AUTH-TOKEN'));

      // For display purposes only
      $scope.token = JSON.parse(atob(TokenStorage.retrieve().split('.')[0]));
    }).error(function (result, status, headers) {
      // console.log(result);
      // console.log(status);
    });
  };

  $scope.logout = function () {
    // Just clear the local storage
    TokenStorage.clear();
    $scope.authenticated = false;

    $location.url("/");
  };
});
