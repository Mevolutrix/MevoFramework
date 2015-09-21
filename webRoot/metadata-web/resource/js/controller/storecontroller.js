var app = angular.module('MyApp', ['ngResource','ngRoute','ngGrid']).config(function ($routeProvider) {
    $routeProvider
      .when('/store', {
        templateUrl: 'page/store.html',
        controller: ''
      })
      .when('/schema', {
        templateUrl: 'page/schema.html',
        controller: ''
      })
      .when('/storeinfo', {
        templateUrl: 'page/storeinfo.html',
        controller: ''
      })
  });

var spacename ='';

app.controller('storeManage', function ($scope) {  

  $scope.storeView = true;
  $scope.schemaView = false;    
  $scope.space = {name:"spaces",realname:""};
  $scope.spaces = spaceModel;

  $scope.$on("storeChange",
    function (event, treeNode) {
       $scope.storeView = true;
       $scope.schemaView = false;   
       $scope.$broadcast("storeChangeFromParent", treeNode);
    });

  $scope.$on("schemaChange",
    function(event,treeNode){
        $scope.storeView = false;
        $scope.schemaView = true;
        $scope.$broadcast("schemaChangeFromParent",treeNode);
    });

  $scope.selectSpace=function(){
    $("#spacemodal").modal();
  }

  $scope.saveSpace = function(){
    $scope.space = $scope.tempSpace; 
    spacename = $scope.space.name;
  }

  $scope.cancelSpace = function(){
    $scope.tempSpace = {};
  }

});

app.controller('storeTreeController', function ($scope) {

});
app.controller('storeEdit',function($scope,$http,$timeout,storeInstance,setMetaInstance,smeDeploySet,smeCreateSet,spaceSchema,StatusesConstant,schemaInstance){

 //	$scope.isBaseSchema = false;
   $scope.createStore = false;
   $scope.createSetMeta = false;

  $scope.storeView = true;
  $scope.schemaView = false;
  $scope.firstShow = true;
  $scope.secondShow = false;

    $scope.store={
        data:storeModel,
        isCreate:false,
        pkeyTypes:dataTypeModel,
        storeTypes:storeTypeModel,
        createStore : function(){
            $scope.store.data = createStoreModel();
            $scope.set = createSetModel();

            $scope.store.data.appSpaceId = $scope.space.realname;
            $scope.store.data.entitySetName = $scope.space.realname+".";

            $("input,select,textarea").each(function(){
                this.disabled = false;
            });
            $scope.store.defaultValueUntil.gridOptions.enableCellEdit = true;

            this.isCreate = true;

            $scope.firstShow = false;
            $scope.secondShow = true;

            $scope.title = "创建store";
        },
        cancelStore : function(){
            $scope.firstShow = true;
            $scope.secondShow = false;

            $(".store_info").each(function(){
                this.disabled = true;
            });
            $scope.title = "查看store";

            storeInstance.get({setName:$scope.store.entitySetName}, this.data);  //change this.store.data to this.data
        },
        publishStore : function(){
            smeDeploySet.deploy(
                {spaceName:$scope.space.realname},
                function(data){
                    console.log("deploy success");
                },
                function(){
                    console.log("deploy failed");
                });
        },
        editStore : function(){
            //$scope.set = createSetModel();
            if(this.entitySetName == ''){
                alert("请先选择一个store");
                return;
            }
            $(".ex_store_info").each(function(){
                this.disabled = false;
            });
            this.isCreate = false;
            $scope.firstShow = false;
            $scope.secondShow = true;
            $scope.title = "编辑store";

            $scope.store.defaultValueUntil.gridOptions.enableCellEdit = true;
            console.log("entitySchemaList",$scope.store.set);
            if(typeof($scope.store.set.data.entitySchemaList)!="undefined" &&$scope.store.set.data.entitySchemaList.length ==0){
                $scope.createSetMeta = true;
            }
        },
        entitySetNameChange : function(){
            if( this.data.entitySetName.indexOf(this.data.appSpaceId+".")!=0 ){
                alert("请不要修改命名空间");
                this.data.entitySetName= this.data.appSpaceId+".";
            }
        },
        saveStore : function(){
            this.set.data.setName = this.data.entitySetName;
            this.set.data.appSpaceId = $scope.space.realname;

            if(this.isCreate == true){
                smeCreateSet.save({setName:this.data.entitySetName},this.data)
                if(typeof(this.set.data.entitySchemaList)!="undefined" && this.set.data.entitySchemaList.length > 0){
                    setMetaInstance.put({setMetaName:this.set.data.setName},this.set.data)
                }
            }
            else{
                storeInstance.save({setName:this.data.entitySetName},this.data);
                if(typeof(this.set.data.entitySchemaList)!="undefined" && this.set.data.entitySchemaList.length > 0 ){
                    if($scope.createSetMeta == true){
                        setMetaInstance.schemaByNameById
                        $scope.createSetMeta = false;
                    }else{
                        setMetaInstance.save({setMetaName:this.set.data.setName},this.set.data);
                    }

                }
            }
            $scope.firstShow = true;
            $scope.secondShow = false;
            $scope.createSetMeta = false;
            $(".store_info").each(function(){
                this.disabled = true;
            });
            $scope.title = "查看数据集";
        }
    };
    $scope.store.indexUnit={
        markTypes : markTypeModel,
        indexTypes : dataTypeModel,
        indexInfoInput:{},
        createIndex : function(){
            $("#input_index").show();
        },
        quitIndex : function(){
            this.indexInfoInput = {};
            $("#input_index").hide();
        },
        confirmIndex : function(){
            $scope.store.data.index.push(this.indexInfoInput);
            this.indexInfoInput = null;
            $("#input_index").hide();
        },
        delIndex :function(index){
            $scope.store.data.index.splice(index,1);
        }
    };
    $scope.store.set={
        data:setModel,
        spaceSchemaList:spaceSchemaListModel,
        schemaNameInput:"",
        isBaseSchemaInput:false,
        createSchema : function(){
            $("#input_schema").show();
            var URL = "/MDE/DSE/EntitySchema?$filter=appSpace eq \'"+$scope.space.realname+"\'";
            $http({
                url:URL,
                method:"GET"
            }).success(function(data){
                $scope.store.set.spaceSchemaList = data;
            });
        },
        confirmSchema : function(){
            var flag = true;
            var tempList = this.data.entitySchemaList;
            if(tempList.length > 1){
                for(var i=0;i<tempList.length;i++){
                    if(tempList[i].schemaName == this.schemaNameInput){
                        flag = false;
                        alert("schema already exist");
                    }
                }
            }
            if(flag==true){
                if(this.isBaseSchemaInput == "true"){
                    this.isBaseSchemaInput = true;
                }else{
                    this.isBaseSchemaInput = false;
                }
                this.data.entitySchemaList.push({
                        id: this.data.entitySchemaList.length+1,
                        schemaName :this.schemaNameInput,
                        isBaseSchema:this.isBaseSchemaInput
                    }
                );
                $("#input_schema").hide();
            }

        },
        quitSchema : function(){
            $("#input_schema").hide();
        },
        delSchema : function(index){
            this.data.entitySchemaList.splice(index,1);
        }
    };
    $scope.store.defaultValueUntil={

      gridOptions : {  //initial grid
        data: 'store.data.defaultValues',
          name:"<select ng-model='COL_FIELD' ng-class='\'colt\' + col.index'   ng-input='COL_FIELD' ng-options='key as key for (key, value) in store.set.data.entitySchemaList[store.defaultValueUntil.gridOptions.selected.id-1].schema |dfvname' ng-blur='store.defaultValueUntil.gridOptions.updateEntity(row)'></select>",
          dType :"<select ng-model='COL_FIELD' ng-class='\'colt\' + col.index'   ng-input='COL_FIELD' ng-options='id as name for (id, name) in statuses' ng-blur='this.updateEntity(row)'></select>",
          buttonCell : '<button  class="btn btn-link" ng-click="store.defaultValueUntil.gridOptions.deleteDFV(row)" ng-show="secondShow">删除</button>',
        enableCellEdit: false,
        selected:{},
        updateEntity:function(row){
            console.log($scope.store.data.defaultValues);
        },
        columnDefs: [{field: "name",displayName:"名称",editableCellTemplate: this.name },
            {field:"dType",displayName:"类型", editableCellTemplate: this.dType ,cellFilter: 'mapStatus'},
            {field:"defaultValue",displayName:"默认值"},
            {field:"evalExp",displayName:"表达式"},
            {displayName:"操作",cellTemplate:this.buttonCell, enableCellEdit: false}
        ]},
        editDFV : function(){
          console.log("edit dfv");
        },
        deleteDFV : function(row){
          $scope.store.defaultValues.remove(row.rowIndex);
        },
        saveSelectSchema: function(){
          var index = this.selected.id-1;
          if($scope.store.data.defaultValues===null){
              $scope.store.data.defaultValues=[];
          }
          $scope.store.data.defaultValues.unshift({name:null,dType:1,defaultValue:null,evalExp:null});
          schemaInstance.get({schemaName:$scope.store.set.data.entitySchemaList[index].schemaName},function(data) {
              $scope.store.set.data.entitySchemaList[index].schema = data;
          })
        },
        createDefaultValue:function(){
            $("#defaultValueModel").modal();
        }
}
  console.log("show initial...............",$scope.storeView);
  $scope.title = "查看数据集";
  $scope.statuses = StatusesConstant;
  $scope.$on("storeChangeFromParent", function (event, treeNode) {


	 if(treeNode.isParent == true){

        $scope.title = "查看store";
        $scope.firstShow = true;
        $scope.secondShow = false;


        $scope.store.data = createStoreModel();

        $scope.store.set.data = createSetModel();

        $scope.$apply(
          storeInstance.get({setName:treeNode.name}).$promise.then(
            function(resolve){
              $scope.store.data = resolve;


              if(typeof($scope.store.data.entitySetName) == "undefined"){
                  alert("获取store出错");
              }else{
                  setMetaInstance.get({setMetaName:treeNode.name}).$promise.then(
                        function(resolve){
                         $scope.store.set.data = resolve;
                          if(typeof($scope.store.set.setName)=="undefined"){
                               $scope.store.set.data = createSetModel();
                               $scope.store.set.setName = $scope.store.data.entitySetName;
                               $scope.createSetMeta = true;
                           }
                       },
                   function(reject){console.log("get SetMetadata failed"+reject)}
                  );
              }
            },
            function(reject){console.log("get store failed")}
          ));

     //   $scope.set.appSpaceId = $scope.space.realname;

        $(".store_info").each(function(){
            this.disabled = true;
        });

       }

    });

});

app.directive('ngBlur', function () {
      return function (scope, elem, attrs) {
        elem.bind('blur', function () {
          scope.$apply(attrs.ngBlur);
        });
      };
    });

app.filter('mapStatus', function( StatusesConstant ) {
     return function(input) {
       if (StatusesConstant[input]) {
         return StatusesConstant[input];
       } else {
         return 'unknown';
       }
     };
   })

app.factory( 'StatusesConstant', function() {
      return {
        1: 'preset',
        2: 'evaljs'
      };
    });

app.filter("dfvname",function(){
    return function (value){
        var returnValue ={};
        for(var key in value){
            if(key !="defaultValues" && key[0] !="$"){
                returnValue[key]=value[key];
            };
        }

        return returnValue;
    };
})



Array.prototype.remove=function(dx)
{
    if(isNaN(dx)||dx>this.length){return false;}
    for(var i=0,n=0;i<this.length;i++)
    {
        if(this[i]!=this[dx])
        {
            this[n++]=this[i]
        }
    }
    this.length-=1
}



	
