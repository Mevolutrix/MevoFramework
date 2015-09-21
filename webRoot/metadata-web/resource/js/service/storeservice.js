
app.factory('schemaInstance',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/EntitySchema(\':schemaName\')',
					  {schemaName:'@schemaName'},
                      { put:{method:"PUT"}, params:{},isArray: true}) ;
}]);

app.factory('storeInstance',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/EntitySet(\':setName\')',
					  {setName:'@setName'},
					  {create:{method:'PUT'},params:{},isArray:false},
                      {get:{method:'GET'},params:{},isArray:false}
					) ;
}]);

app.factory('schemaByNameById',['$http','$resource',function($http,$resource){
    return $resource('/MDE/DSE/:NAME(\':ID\')',
        {NAME:'@name',ID:'@id'},
        { put:{method:"PUT"}, params:{},isArray: false}) ;
}]);

app.factory('schemaByName',['$http','$resource',function($http,$resource){
	return $resource('/MDE/DSE/:NAME',
		{NAME:'@name'},
		{
			put:{method:"PUT", params:{},isArray: true},
		 	get:{method:"GET", params:{},isArray: true}
		}) ;
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



app.factory('setMetaInstance',function($http,$resource){
	return $resource(
					  '/MDE/DSE/SetMetadata(\':setMetaName\')',
					  {setMetaName:'@setMetaName'},
	  				  { put:{method:"PUT"}, params:{},isArray: false}
	  				 )
});

app.factory('smeDeploySet',function($http,$resource){
	return $resource( 
		 			 "/MDE/SME/deploySets?appSpace=\':spaceName\'",
					  {spaceName:'@spaceName'},
					  {deploy:{method:"GET"},params:{},isArray: true}
					) ;
});


app.factory('smeCreateSet',function($http,$resource){
	return $resource( 
		 			 "/MDE/SME/createSet?setName=\':setName\'",
					  {setName:'@setName'}	  
					) ;
});


app.factory('spaceSchema',function($http,$resource){
	return $resource( 
		 			 "/MDE/DSE/EntitySchema?$filter=appSpace eq \':spaceName\'",
					  {spaceName:'@spaceName'},
					  {get:{method:"GET"},params:{},isArray: true}	  
					) ;
});

app.factory('dataTable',['$http','$resource',function($http,$resource){
	return $resource('/XEDU/DSE/:NAME(\':ID\')',
		{NAME:'@name',ID:'@id'},
		{ put:{method:"PUT"}, params:{},isArray: false}
	) ;
}
]);





