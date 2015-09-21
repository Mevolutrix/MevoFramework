'use strict';

dashboardApp.directive('validatorDirective', [function () {
    return {
      templateUrl: './views/directive-templates/form/validator.html',
      restrict: 'E',
      scope: false
    };
  }
]);