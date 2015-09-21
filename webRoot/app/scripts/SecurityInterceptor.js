/**
 * Created by Ming on 2015/2/15.
 */

dashboardApp.factory('TokenStorage', function() {
  var storageKey = 'auth_token';
  return {
    store : function(token) {
      return localStorage.setItem(storageKey, token);
    },
    retrieve : function() {
      return localStorage.getItem(storageKey);
    },
    clear : function() {
      return localStorage.removeItem(storageKey);
    }
  };
}).factory('TokenAuthInterceptor', function($q, TokenStorage) {
  return {
    request: function(config) {
      var authToken = TokenStorage.retrieve();
      if (authToken) {
        config.headers['X-AUTH-TOKEN'] = authToken;
      }
      return config;
    },
    responseError: function(error) {
      if (error.status === 401 || error.status === 403) {
        TokenStorage.clear();
      }
      return $q.reject(error);
    }
  };
}).config(function($httpProvider) {
  $httpProvider.interceptors.push('TokenAuthInterceptor');

  //fancy random token
  function b(a){return a?(a^Math.random()*16>>a/4).toString(16):([1e16]+1e16).replace(/[01]/g,b)};

  $httpProvider.interceptors.push(function() {
    return {
      'request': function(config) {
        // put a new random secret into our CSRF-TOKEN Cookie after each response
        var token=b();
        document.cookie = 'CSRF-TOKEN=' + token;
        return config;
      }
    };
  });
});
