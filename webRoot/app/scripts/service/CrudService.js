/**
 * Created by Ming on 2015/2/10.
 */
dashboardApp.factory('CrudService', function ($http) {
    return {
        list: function (docName, callback) {
            $http({
                method: 'GET',
                //url: 'users.json',
                url: '/rest/document/' + docName,
                cache: false
            }).success(function(data,status,headers,config){
                console.log(data);
                callback(data);
            }).error(function(data,status,headers,config){
                console.log(data);
            });
        },
        find: function (docName, id, callback) {
            $http.get(
                '/rest/document/' + docName + '/' + id
            ).success(function(data,status,headers,config){
                console.log(data);
                callback(data);
            }).error(function(data,status,headers,config){
                console.log(data);
            });
        },

        update: function (docName, id, data, successCallback, errCallback) {
            $http({
                method: 'PUT',
                url: '/rest/document/' + docName + '/' + id,
                data: data
            }).success(function(data,status,headers,config){
                console.log(data);
                successCallback(data);
            }).error(function(data,status,headers,config){
                errCallback(data);
                console.log(data);
            });
        },

        create:function(docName,data, successCallback, errCallback){
            $http({
                method: 'POST',
                url: '/rest/document/' + docName ,
                data: data
            }).success(function(data,status,headers,config){
                console.log(data);
                successCallback(data);
            }).error(function(data,status,headers,config){
                errCallback(data);
                console.log(data);
            });
        }
    };
});
