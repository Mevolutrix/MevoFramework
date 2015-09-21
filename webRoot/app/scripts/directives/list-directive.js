'use strict';

dashboardApp.directive('listDirective', ['FormService', function (FormService) {
    return {
      controller: function($scope){
        $scope.preAddData = function(){
          if($scope.loadForm.ID ==""){
            alert("还没有选择业务数据诶~~")
          }else{
            console.log("#/"+ $scope.loadForm.ID +"/insert");
            location.href="#/"+ $scope.loadForm.ID +"/insert";
          }
        },
        $scope.cancel = function() {
          location.href="#/"+ $scope.loadForm.ID +"/list"
        }
      },
      templateUrl: './views/directive-templates/list/list.html',
      restrict: 'E'
    };
  }]);

