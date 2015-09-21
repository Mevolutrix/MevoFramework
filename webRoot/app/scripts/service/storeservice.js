
dashboardApp.factory('schemaInstance',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/EntitySchema(\':schemaName\')',
		{schemaName:'@schemaName'},
		{put:{method:"PUT"}, params:{},isArray: true}) ;
}]);

dashboardApp.factory('storeInstance',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/EntitySet(\':setName\')',
		{setName:'@setName'},
		{create:{method:'PUT'},params:{},isArray:false},
		{get:{method:'GET'},params:{},isArray:false}
	) ;
}]);

dashboardApp.factory('schemaByNameById',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/:NAME(\':ID\')',
		{NAME:'@name',ID:'@id'},
		{ put:{method:"PUT"}, params:{},isArray: false}) ;
}]);

dashboardApp.factory('schemaByName',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/:NAME',
		{NAME:'@name'},
		{
			put:{method:"PUT", params:{},isArray: true},
			get:{method:"GET", params:{},isArray: true}
		}) ;
}]);

dashboardApp.factory('dataTable',['$http','$resource',function($http,$resource){
	return $resource('http://localhost:8080/XEDU/DSE/:NAME(\':ID\')',
		{NAME:'@name',ID:'@id'},
		{ put:{method:"PUT"}, params:{},isArray: false});
}]);



/**
 * Created by user on 2015/4/22.
 */
/*
app.factory('storeInstanceFilterAppID',['$http','$resource',function($http,$resource){
    return $resource('/MDE/DSE/EntitySet?$filter=appSpaceId eq \':setName\' &$select=entitySetName',
        {setName:'@setName'},
        {get:{method:'GET'},params:{},isArray:false}
    ) ;
}]);

app.factory('schemaInstanceFilterAppID',['$http','$resource',function($http,$resource){
    return $resource('/MDE/DSE/EntitySchema?$filter=appSpace eq \':setName\')',
        {setName:'@setName'},
        {get:{method:'GET'},params:{},isArray:false}
    ) ;
}]); */



dashboardApp.factory('setMetaInstance',function($http,$resource){
	return $resource(
					  '/MDE/DSE/SetMetadata(\':setMetaName\')',
					  {setMetaName:'@setMetaName'},
	  				  { put:{method:"PUT"}, params:{},isArray: false}
	  				 )
});

dashboardApp.factory('smeDeploySet',function($http,$resource){
	return $resource( 
		 			 "/MDE/SME/deploySets?appSpace=\':spaceName\'",
					  {spaceName:'@spaceName'},
					  {deploy:{method:"GET"},params:{},isArray: true}
					) ;
});


dashboardApp.factory('smeCreateSet',function($http,$resource){
	return $resource( 
		 			 "/MDE/SME/createSet?setName=\':setName\'",
					  {setName:'@setName'}	  
					) ;
});


dashboardApp.factory('spaceSchema', function($http, $resource) {
	return $resource(
		"/MDE/DSE/EntitySchema?$filter=appSpace eq \':spaceName\'", {
			spaceName: '@spaceName'
		}, {
			get: {
				method: "GET"
			},
			params: {},
			isArray: true
		}
	);
});

//20150429 Mark
dashboardApp.factory('spaceIdSchema', function($http, $resource) {
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

dashboardApp.factory('getAllId',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/Form?$select=id',{},
		{
			get:{method:"GET", params:{},isArray: true}
		}) ;
}]);

