(function() {
  angular.module('validator.rules', ['validator']).config([
    '$validatorProvider', function($validatorProvider) {
      $validatorProvider.register('required', {
        invoke: 'watch',
        validator: /.+/,
        error: '请输入数据！'
      });
      $validatorProvider.register('number', {
        invoke: 'watch',
        validator: /^[-+]?[0-9]*[\.]?[0-9]*$/,
        error: '请输入数字.'
      });
      $validatorProvider.register('email', {
        invoke: 'blur',
        validator: /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
        error: '请输入有效电子邮箱.'
      });


      $validatorProvider.register('url', {
        invoke: 'blur',
        validator: /((([A-Za-z]{3,9}:(?:\/\/)?)(?:[-;:&=\+\$,\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\+\$,\w]+@)[A-Za-z0-9.-]+)((?:\/[\+~%\/.\w-_]*)?\??(?:[-\+=&;%@.\w_]*)#?(?:[\w]*))?)/,
        error: '请输入有效超链接地址.'
      });

      $validatorProvider.register('backendWatch', {
        invoke: 'watch',
        validator: function(value, scope, element, attrs, $injector) {
          var $http;
          $http = $injector.get('$http');
          return $http.get('example/data.json').then(function(response) {
            var x;
            if (response && response.data) {
              return !(__indexOf.call((function() {
                var _i, _len, _ref, _results;
                _ref = response.data;
                _results = [];
                for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                  x = _ref[_i];
                  _results.push(x.name);
                }
                return _results;
              })(), value) >= 0);
            } else {
              return false;
            }
          });
        },
        error: "do not use 'Kelp' or 'x'"
      });
      $validatorProvider.register('backendSubmit', {
        validator: function(value, scope, element, attrs, $injector) {
          var $http;
          $http = $injector.get('$http');
          return $http.get('example/data.json').then(function(response) {
            var x;
            if (response && response.data) {
              return !(__indexOf.call((function() {
                var _i, _len, _ref, _results;
                _ref = response.data;
                _results = [];
                for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                  x = _ref[_i];
                  _results.push(x.name);
                }
                return _results;
              })(), value) >= 0);
            } else {
              return false;
            }
          });
        },
        error: "do not use 'Kelp' or 'x'"
      });
      $validatorProvider.register('backendBlur', {
        invoke: 'blur',
        validator: function(value, scope, element, attrs, $injector) {
          var $http;
          $http = $injector.get('$http');
          return $http.get('example/data.json').then(function(response) {
            var x;
            if (response && response.data) {
              return !(__indexOf.call((function() {
                var _i, _len, _ref, _results;
                _ref = response.data;
                _results = [];
                for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                  x = _ref[_i];
                  _results.push(x.name);
                }
                return _results;
              })(), value) >= 0);
            } else {
              return false;
            }
          });
        },
        error: "do not use 'Kelp' or 'x'"
      });
      $validatorProvider.register('requiredSubmit', {
        validator: /^.+$/,
        error: '请输入数据！'
      });
      $validatorProvider.register('requiredBlur', {
        invoke: 'blur',
        validator: /^.+$/,
        error: '请输入数据！'
      });
      $validatorProvider.register('numberSubmit', {
        validator: /^[-+]?[0-9]*[\.]?[0-9]*$/,
        error: 'This field should be number.'
      });
      $validatorProvider.register('cellPhone', {
        invoke: 'blur',
        validator: /^1[0-9]{10}$/,
        error: '请输入正确手机号码.'
      });
      $validatorProvider.register('customLess', {
        invoke: 'watch',
        validator: function(value, scope) {
          return value < scope.formWatch.number;
        },
        error: 'It should less than number 1.'
      });
    }
  ]);

}).call(this);
