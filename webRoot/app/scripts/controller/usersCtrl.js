/**
 * Created by Ming on 2015/2/10.
 */

dashboardApp.controller("UserListController", function ($scope, CrudService) {
    //$scope.model={
    //    message: "This is the test Message!!"
    //}
    CrudService.list('UserInfo',function (users) {
      $scope.users = users;
      $scope.sortField = 'fullname';
      $scope.reverse = true;
    })
})
.controller('UserDetailController', function ($scope, $routeParams, CrudService) {
    if($routeParams.userId!='null' && !isNaN($routeParams.userId)){
        CrudService.find('UserInfo', $routeParams.userId, function (user) {
            $scope.user = user;
        });
    }
})

.controller('UserUpdateController', function($scope, $validator, CrudService, $location) {

  $scope.formSubmit = {

    submit: function() {
      return $validator.validate($scope, 'user').success(function() {
        if ($scope.user.userId == undefined) {
          CrudService.create('UserInfo', $scope.user, function() {}, function() {});
        } else {
          CrudService.update('UserInfo', $scope.user.userId, $scope.user, function() {}, function() {});
        }

        return console.log('success');
      }).error(function() {
        return console.log('error');
      }).then(function() {
        $location.url("/users");
        return console.log('then');
      });
    },
    reset: function() {
      console.log("testste");
      $location.url("/users");

      return $validator.reset($scope, 'formSubmit');
    }
  };
});

//dashboardApp.run(function($validator) {
//  return $validator.register('requiredRun', {
//    invoke: 'watch',
//    validator: /^.+$/,
//    error: 'This field is requrired.'
//  });
//})

