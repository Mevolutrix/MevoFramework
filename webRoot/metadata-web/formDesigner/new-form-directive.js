'use strict';

app.directive('newForm', ['$http', function($http) {
	var linker = function(scope) {
		if (scope.params != undefined) {
			scope.$on('busData', function (e, data) {
	            $scope.busData = data;
	        });
			$http({
				url: "/MDE/DSE/CForm(\'" + scope.params.formId + "\')",
				method: 'GET'
			}).success(function(data) {
				scope.newForm = data;
			})
		} else {
			scope.newForm = scope.previewForm;
		}
	};
	return {
		link: linker,
		templateUrl: './formDesigner/newForm.html',
		restrict: 'E',
		scope: {
			previewForm: "=form"
		}
	};
}]);