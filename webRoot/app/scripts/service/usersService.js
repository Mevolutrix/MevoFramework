/**
 * Created by Ming on 2015/2/10.
 */
dashboardApp.factory('usersService', function ($http) {
  return {
    list: function (callback) {
      $http({
        method: 'GET',
        url: 'users.json',
        cache: true
      }).success(callback);
    }
    ,
    find: function (id, callback) {
      //$http({
      //  method: 'GET',
      //  url: 'user_' + id + '.json',
      //  cache: true
      //}).success(callback);

      $http.get('user_' + id + '.json')
    },
    update: function(id,user){
      $http({
        method: 'PUT',
        url: 'users/'+id,
        data: user
      })
    }
  };
});
