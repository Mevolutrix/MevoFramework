
app.controller('storeInfoController', function ($scope,$http,schemaInstance) {
     $scope.storeShow = true;
     var flag = false;
     var createFlag = false;
     var name ="";
     var col ={};

   $scope.$on("storeChangeFromParent",function (event, treeNode) {
        $scope.storeShow = true;


  	     if(treeNode.isParent == true){

          $scope.title = "查看业务数据";
          $scope.storeShow = true;
          $scope.store ={};
          $scope.storeInfo = [];
           console.log("storeInfo:",$scope.storeInfo);

          $scope.$apply(
              schemaInstance.get({schemaName:treeNode.name}).$promise.then(
                  function(resolve){

                      var properties = resolve.properties;
                      var colName = [];
                      for(var i=0;i<properties.length;i++){
                        col[properties[i].name]="";
                      }
                      colName.push(col);

                      name = treeNode.name.substring(treeNode.name.lastIndexOf(".")+1);
                      var URL = "/MDE/DSE/"+ name;

                      $http({
                             url:URL,
                             method:"GET"
                         }).success(function(data){
                             console.log("data",data);
                             //no default value in set
                             if(data.length == 0){
                                $scope.storeInfo = colName;
                                flag = true;
                             //exist default value in set
                             }else{
                                $scope.storeInfo = data;
                                flag = false;
                             }
                                 console.log("final storeINfo",$scope.storeInfo);


                         }).error(function(){
                            console.log("get property error");
                         }).finally(function(){
                            console.log("final get data");
                         });
//                    $scope.storeInfo = colName;



                  },
                  function(reject){console.log("get schema failed")}
              )
            );
  		  }
   });

   $scope.testGrid = function(){
     console.log("begin test");
     $scope.storeInfo = [{test1:111,test2:222}];
   }

    $scope.gridOptions = {
       data: 'storeInfo',
       enableCellEdit: true
    }

    $scope.createStoreInfo = function(){
       $scope.storeInfo.unshift(col);
       createFlag = true;
       if(flag == true){  //业务表为空，删除临时记录
         $scope.storeInfo.pop();
       }
    }

    $scope.testComplex = function(){
      var url = "/MED/DSE/DefaultValueTest('4')";
    }

    $scope.saveStoreInfo = function(){

           var URL = "/MDE/DSE/"+ name +"(\'"+ $scope.storeInfo[0].id+"\')";
           if(createFlag == true){

                  $http({
                      url:URL,
                      method:"PUT",
                      data: $scope.storeInfo[0]
                  }).success(function(data){
                     console.log("data:",data);
                  });
           }
    }

})






	
