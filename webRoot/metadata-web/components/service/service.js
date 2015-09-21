
var cmsService= angular.module('cmsService',['ngResource']);
cmsService.factory('instance', function(){
    return {};
})

cmsService.factory('schemaByNameById',['$http','$resource',function($http,$resource){
    return $resource('http://localhost:8080/MDE/DSE/:NAME(\':ID\')',
        {NAME:'@name',ID:'@id'},
        { get:{method:"GET", params:{},isArray: false}},
        { put:{method:"PUT", params:{},isArray: false}},
        { save:{method:"POST", params:{}, isArray: false}},
        { delete:{method:"DELETE", params:{}, isArray: false}}) ;
}]);

cmsService.factory('schemaByNameById2',['$http','$resource',function($http,$resource){
    return $resource('http://localhost:8080/CMS/DSE/:NAME(\':ID\')',
        {NAME:'@name',ID:'@id'},
        { get:{method:"GET", params:{},isArray: false}},
        { put:{method:"PUT", params:{},isArray: false}},
        { save:{method:"POST", params:{}, isArray: false}},
        { delete:{method:"DELETE", params:{}, isArray: false}}) ;
}]);

cmsService.factory('schemaBySpaceByNameById',['$http','$resource',function($http,$resource){
    return $resource('http://localhost:8080/:SPACE/DSE/:NAME(\':ID\')',
        { SPACE:'@space',NAME:'@name',ID:'@id'},
        { put:{method:"PUT", params:{},isArray: false}},
        { post:{method:"POST", params:{}, isArray: false}},
        { delete:{method:"DELETE", params:{}, isArray: false}}) ;

}]);



cmsService.factory('schemaByName',['$http','$resource',function($http,$resource){
    return $resource('/MDE/DSE/:NAME',
        {NAME:'@name'},
        {
            put:{method:"PUT", params:{},isArray: true},
            get:{method:"GET", params:{},isArray: true}
        }) ;
}]);

cmsService.factory('schemaBySpaceByNameByfilter', ['$http', '$resource', function ($http, $resource) {
    return $resource('/:SPACE/DSE/:NAME?$filter=:COLUMN eq :FILTER',
        {SPACE:'@space', NAME: '@name', COLUMN:'@column', FILTER:'@filter'},
        {
            put: {method: "PUT", params: {}, isArray: true},
            get: {method: "GET", params: {}, isArray: true}
        });
}]);
cmsService.factory('schemaByNameWithFilter',['$http','$resource',function($http,$resource){
    return $resource('/MDE/DSE/:NAME?$filter=appSpace eq \':KEYWORD\' ',
        {NAME:'@name', KEYWORD: '@keyword'},
        {
            put:{method:"PUT", params:{},isArray: true},
            get:{method:"GET", params:{},isArray: true}
        }) ;
}]);

cmsService.factory('spaceIdSchema', function($http, $resource) {
    return {
        //get all schema
        schema: function(name){
            var url = "/MDE/DSE/EntitySchema?$filter=appSpaceId eq '"+ name +"'";
            return $http({
                url:url,
                method:"GET"
            }).success(function(data){}).error(function(data){alert("ERROR")});
        },
        //get columns
        columns: function(table){
            var url = "/MDE/DSE/EntitySchema('"+ table +"')";
            return $http({
                url: url,
                method: "GET"
            }).success(function(data){}).error(function(data){alert("ERROR")});
        },
        //get validator
        validator: function(schemaName){
            var url = "/MDE/DSE/"+schemaName;
            return $http({
                url: url,
                method: "GET"
            }).success(function(data){
            }).error(function(data){alert("ERROR")});
        }
    }
});