

var myAppDirective = angular.module("myAppDirective",['ngRoute', 'ngResource']);



myAppDirective.directive('myCollapse' , function(){
    return{
        scope:{
            collapse : '&',
            collapseName : '&'
         },
        replace:false,
        template:"<div ng-click='collapse()'>{{collapseName}}</div>",
        link:function ($scope, element, attributes) {
            $scope.collapse = function(){
                $("#" + attributes.value).collapse('toggle');
            };
            $scope.collapseName = attributes.name;
         //   element.attr('ng-click' ,'collapse()');
        //    $compile(element.contents())($scope);
        }
    }
});

myAppDirective.directive('spaceSelect' , function(){
    return{
        scope:{
            spaces : '&',
            tempSpace : '&'
        },
        replace:false,
        templateUrl:"components/directive/template/spaceSelect.html",
        link:function (scope, element, attributes) {
            scope.spaces = new Spaces();
            scope.tempSpace="";
            scope.spaceChange = function(){
                scope.$parent[attributes.method](scope.tempSpace);
            }
       //     $compile(element.contents())($scope);
        }
    }
});






