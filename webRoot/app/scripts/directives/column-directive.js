'use strict';

dashboardApp.directive('columnDirective', function () {
    return {
        templateUrl: './views/directive-templates/list/column.html',
        restrict: 'E',
        scope: {
            field:'=',
            data:'='
        }
    };
  });
