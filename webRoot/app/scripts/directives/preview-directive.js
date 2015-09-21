'use strict';

dashboardApp.directive('previewDirective', [function () {
  return {
    templateUrl: './views/directive-templates/list/preview.html',
    restrict: 'E'
  };
}]);