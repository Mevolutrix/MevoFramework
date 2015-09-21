


app.controller('schemaTreeController', function ($scope) {

});

app.controller('schemaEdit',function($scope,$http,schemaInstance,schemaByNameById, schemaByName ,dataTable,$filter){



    //=======
    $scope.firstShow = true;
    $scope.secondShow = false;

    $scope.title = "查看元数据";

    $(".schema_info").each(function(){
            this.disabled = true;
        })

    $scope.$on("schemaChangeFromParent",
	    function (event, treeNode) {
            $scope.title = "查看元数据";
          //  $scope.editFlag = true;

	       if(treeNode.isParent == false){  
                $scope.$apply(schemaInstance.get({schemaName:treeNode.name},function(data){
                       $scope.schema.data = data;


                }));

                 $scope.firstShow = true;
                 $scope.secondShow = false;

		   }

        });


    $scope.schema={             //schema数据和方法对象
        data:{},     //schema数据
        entitySets:[],                 //数据集名称
        createSchema:function(){      //创建Schema
                     $scope.firstShow = false;
                     $scope.secondShow = true;
                     $scope.title = "新增元数据";
                     $scope.createFlag = true;
                     this.data = createSchemaModel();
                     this.data.appSpaceId = $scope.space.realname;
                     this.data.id = $scope.space.realname + ".";

                     $(".schema_info").each(function(){
                        this.disabled = false;
                     })
         },
        editSchema:function(){
            if($scope.editFlag == false){
                alert("请先选择实体再修改");
                return;
            }
            $scope.firstShow = false;
            $scope.secondShow = true;
            $scope.title = "修改元数据";
            $scope.createFlag = false;

            $(".ex_schema_info").each(function(){
                this.disabled = false;
            })
        },
        cancelSchema:function(){
            $scope.firstShow = true;
            $scope.secondShow = false;
            $scope.title = "查看元数据";
            $scope.createFlag = false;

            $(".schema_info").each(function(){
                this.disabled = true;
            })
            //$scope.schema.propertyUntil.propertyInputShow = false;
        },
        saveSchema:function(){
            /*  for(var i =0 ;i < $scope.schema.properties.length ;i++){
             if($scope.schema.properties[i].isArray == "1"){
             $scope.schema.properties[i].isArray = true;
             }else {
             $scope.schema.properties[i].isArray = false;
             }
             }  */

            if($scope.createFlag == true){
                schemaInstance.put({schemaName:this.data.id},this.data);
            }else{
                schemaInstance.save({schemaName:this.data.id},this.data);
            }
            $scope.firstShow = true;
            $scope.secondShow = false;
        },
        entitySetNameChange : function(){               //shema id 输入框修改时不能修改命名空间
        if( this.data.id.indexOf(this.data.appSpaceId+".")!=0 ){
            alert("请不要修改命名空间");
            this.data.id= this.data.appSpaceId+"."+this.data.entityName;
        }
    }
    };


    $scope.schema.propertyUntil={
        isEdit:false,
        spaceSchemaList:{},    //负责数据类型下来框数据
        propertyInputShow:false,                //是否显示propertyInput输入框
        propertyId:"",                          //记录修改或删除的propertyID
        boolean:booleanModel,                   //是否数组输入框
        propertyInput:propertyModel,           //修改或新建properties输入框
        propertiesType:dataTypeModel,           //pType输入框
        delProperty:function(index){
           $scope.schema.data.properties.splice(index,1);
        },      //删除属性
        editProperty : function(index){        //编辑属性
           this.propertyInputShow = true;
           angular.copy($scope.schema.data.properties[index], this.propertyInput);
           this.isEdit = true;
           this.propertyId = index;
       },
       createProperty : function(){            //创建属性
           this.propertyInputShow = true;
           this.isEdit = false;
           this.propertyInput.name = '';
           this.propertyInput.description = '';
           this.propertyInput.pType = '';
           this.propertyInput.complexTypeName = null;
           this.propertyInput.verificationRegEx =null;

       },
        quitProperties:function(){
          this.propertyInputShow = false;
       },
        confirmProperties:function(){
          console.log("complextype",this.propertyInput.complexTypeName);
          if(this.propertyInput.complexTypeName!= null){
            this.propertyInput.pType = 3;
          }
          if(this.isEdit == true){
            angular.copy(this.propertyInput, $scope.schema.data.properties[this.propertyId]);
          }else{
            $scope.schema.data.properties.push({});
            angular.copy(this.propertyInput, $scope.schema.data.properties[$scope.schema.data.properties.length-1])
          }
            this.propertyInputShow = false;
          }
       };

    $scope.schema.propertiesValidator={
        //data:ValidatorModel,
        //name invokeType patten errMsg
        data:[],
        validatorField:{
            invokeTypes:["watch","blur"],
            editID:-1,
            name:"",
            invokeType:"",
            pattern:"",
            errMsg:""
        },
        currentPage : 1,
        pageSize : 2,
        load: function(){

        },
        loadData : function(){

        },

        addValidator:function(){
            $("#addValidator").modal();
            this.loadData();
        },
        prevPage : function(){
          if(this.currentPage > 1){
            this.currentPage--;
          }
       },
       prevPageDisable : function(){
            return (this.currentPage == 1 || this.currentPage > this.pageCount()) ? "disabled" : "";
        },
       pageCount : function(){
            var temp = this.data;
           return Math.ceil(($filter('filter')(temp,$scope.schema.propertiesValidator.query,false)).length/this.pageSize);
       },
       nextPage : function(){
            if(this.currentPage < this.pageCount()){
                this.currentPage++;
            }
        },
       nextPageDisable : function(){
            return this.currentPage === this.pageCount() ? "disabled" : "";
        },
        addNewValidatorField:function(newValidatorField) {

            console.log("id",this.validatorField.editID);
        if(this.validatorField.editID < 0) { //add
            var newValidator = {
                "name": newValidatorField.name,
                "invokeType":newValidatorField.invokeType == 'watch'?0:1,
                "pattern": newValidatorField.pattern,
                "errMsg": newValidatorField.errMsg
            };
            $scope.schema.propertiesValidator.data.push(newValidator);
            schemaByNameById.put({NAME:"ValidationRule",ID:newValidator.name},newValidator);
            this.validatorField.name="";
            this.validatorField.invokeType ="";
            this.validatorField.pattern="";
            this.validatorField.errMsg="";
            this.validatorField.editID = -1;
            this.currentPage = this.pageCount();
        }else{ //modify
            var newValidator = {
                "name": newValidatorField.name,
                "invokeType":newValidatorField.invokeType == 'watch'?0:1,
                "pattern": newValidatorField.pattern,
                "errMsg": newValidatorField.errMsg
            };
            $scope.schema.propertiesValidator.data[this.validatorField.editID].name = newValidatorField.name;
            $scope.schema.propertiesValidator.data[this.validatorField.editID].invokeType = newValidatorField.invokeType == 'watch'?0:1,
            $scope.schema.propertiesValidator.data[this.validatorField.editID].pattern = newValidatorField.pattern;
            $scope.schema.propertiesValidator.data[this.validatorField.editID].errMsg = newValidatorField.errMsg;
            schemaByNameById.save({NAME:"ValidationRule",ID:this.data[this.validatorField.editID].name},newValidator);
            this.validatorField.editID = -1;
            this.validatorField.name = "";
            this.validatorField.invokeType = "";
            this.validatorField.pattern = "";
            this.validatorField.errMsg = "";
        }
    },

        deleteValidatorField : function (name, index){
            console.log("name",name);
            schemaByNameById.remove({NAME:"ValidationRule",ID:name});
            $scope.schema.propertiesValidator.data.splice(index,1);
            this.currentPage = this.currentPage > this.pageCount()? this.pageCount():this.currentPage;
         },
        getInvokeTypes : function(){
          return  this.validatorField.invokeTypes;
        },
        editValidatorField : function(validator,index){
            this.validatorField.name=validator.name;
            this.validatorField.invokeType =validator.invokeType == 0?"watch":"blur";
            this.validatorField.pattern=validator.pattern;
            this.validatorField.errMsg=validator.errMsg;
            this.validatorField.editID = index;
       },
       cancelValidator :function(){
            this.validatorField.name="";
            this.validatorField.invokeType ="";
            this.validatorField.pattern="";
            this.validatorField.errMsg="";
            this.validatorField.editID = -1;
      }
    };

    var URL = "/MDE/DSE/"+" ValidationRule";
    schemaByName.get({NAME:" ValidationRule"},function(data){
        $scope.schema.propertiesValidator.data=data;
    });
   //$http({
   //     url:URL,
   //     method:"GET",
   //     headers:{'Content-Tranfer-Encoding': 'utf-8'},
   //     cache:true
   // }).success(function(data){
   //     console.log("data",data);
   //     //no default value in set
   //     $scope.schema.propertiesValidator.data=data;
   //     console.log("final storeINfo",this.data);
   // }).error(function(){
   //     console.log("get property error");
   // }).finally(function(){
   //     console.log("final get data");
   // });
    $scope.$watch("schema.propertiesValidator.query",function(newValue,oldValue){
        if(newValue!=oldValue){

            $scope.schema.propertiesValidator.currentPage = 0;
        }
    });
    if($scope.space.realname!="" && typeof($scope.space.realname ) != "undefined") {
        var URL = "/MDE/DSE/EntitySet?$filter=appSpaceId eq \'" + $scope.space.realname + "\' &$select=entitySetName";
        $http({
            url: URL,
            method: "GET"
        }).success(
            function (data) {
                $scope.schema.entitySets = data;
            });
    }
    if($scope.space.realname!="" && typeof($scope.space.realname ) != "undefined") {
        var URL2 = "/MDE/DSE/EntitySchema?$filter=appSpace eq \'" + $scope.space.realname + "\'";
        $http({
            url: URL2,
            method: "GET"
        }).success(function (data) {
            for (var i = 0; i < data.length; i++) {
                data[i].id = data[i].id.substring(data[i].id.lastIndexOf(".") + 1);
            }
            $scope.schema.propertyUntil.spaceSchemaList = data;
        });
    }
});


app.filter('offset',function(){
    return function(input, start){
        start=parseInt(start,10);
        return input.slice(start);
    }
});









	
