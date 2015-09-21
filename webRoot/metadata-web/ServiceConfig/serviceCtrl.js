/**
 * Created by zhoutao1 on 2015/6/2.
 */
app.controller('serviceCtrl',function($scope,$http,dataInterface,spaceSchema,spaceStore,storeInstance){
    console.log("serviceCtrl")
    $scope.serviceDisplay = true;
    $scope.serviceAdd = false;

    $scope.tempData={
        httpMethod :"",
        serviceType:"",
        paramForService:"",
        store:"",
        filter:"",
        select:"",
        pKey:true
    };
    $scope.schema=[];
    $scope.stores=[];
    $scope.storeIndex = [];
    $scope.httpMethods = httpMethodsModel;
    $scope.space = spaceModel;
    $scope.serviceType = serviceTypeModel;
    $scope.paramForService = paramForServiceModel;
    $scope.$watch("tempData.store",function(newValue) {
        if (newValue != "" && typeof(newValue) != "undefined") {
            storeInstance.get({setName: newValue}).$promise.then(
                function (value) {
                    $scope.storeIndex = ["无"];
                    for (var j = 0; j < value.index.length; j++) {
                        $scope.storeIndex.push(value.index[j].path);
                    }
                }
            )
        }
    })
    $scope.$watch("temp.appAlias",function(newValue){
        var spaceName="";
        for(var i = 0; i < $scope.space.length; i++ ){
            if($scope.space[i].name == newValue){
                spaceName = $scope.space[i].realname;
            }
        }
        if(spaceName!=""){
            spaceSchema.query({spaceName:spaceName}).$promise.then(
                function(value){
                    $scope.schema=[];
                    for(var i=0;i < value.length; i++){
                        $scope.schema.push(value[i].id)
                    }
                },
                function(error){
                    console.log("error");
                })
        }
        if(spaceName!=""){
            spaceStore.query({spaceName:spaceName}).$promise.then(
                function(value){
                    console.log(value)
                    $scope.stores=[];
                    for(var i=0;i < value.length; i++){
                        $scope.stores.push(value[i].entitySetName)
                    }
                },
                function(error){
                    console.log("error");
                })
        }
    })
    $scope.$watch("servicedata.appAlias",function(newValue){
        var spaceName="";
        for(var i = 0; i < $scope.space.length; i++ ){
            if($scope.space[i].name == newValue){
                spaceName = $scope.space[i].realname;
            }
        }
        if(spaceName!=""){
            spaceSchema.query({spaceName:spaceName}).$promise.then(
                function(value){
                    $scope.schema=[];
                    for(var i=0;i < value.length; i++){
                        $scope.schema.push(value[i].id)
                    }
                },
                function(error){
                    console.log("error");
                })
        }
        if(spaceName!=""){
            spaceStore.query({spaceName:spaceName}).$promise.then(
                function(value){
                    console.log(value)
                    $scope.stores=[];
                    for(var i=0;i < value.length; i++){
                        $scope.stores.push(value[i].entitySetName)
                    }
                },
                function(error){
                    console.log("error");
                })
        }
    })




    $scope.AddService = function(){
        $scope.$broadcast("isAdd");
        $scope.serviceDisplay = false;
        $scope.serviceAdd = true;

        $scope.temp ={
            id:0,
            appAlias:"",
            name:"",
            serviceURL:"",
            inSchemaId:"",
            outSchemaId:"",
            description:"",
            version:""
        }

    }
    $scope.ModifyService = function(id){
        $scope.$broadcast("isModify");
        $scope.serviceDisplay = false;
        $scope.serviceAdd = false;
        $scope.tempData={
            httpMethod :"",
            serviceType:"",
            paramForService:"",
            store:"",
            filter:"",
            select:"",
            pKey:true
        };
        //get:/XEDU/DSE/Answer?$filter='masterid=@0'&$select='uid'
        $scope.tempData.httpMethod = $scope.servicedata.serviceURL.substr(0,$scope.servicedata.serviceURL.indexOf(":"));
        console.log($scope.servicedata.serviceURL)
        console.log($scope.servicedata.serviceURL.split("/")[2])
        var serviceURLArray = $scope.servicedata.serviceURL.split("/");
        $scope.tempData.serviceType = serviceURLArray[2];
        if($scope.tempData.serviceType == "DSE"){
            var index1 = serviceURLArray[3].indexOf("filter")
            var index2 = serviceURLArray[3].indexOf("select")
            var index3 = serviceURLArray[3].indexOf("(@0)")
            var index4 = serviceURLArray[3].indexOf("?")
            if(index3 > 0){  //主键操作 get:/XEDU/DSE/Answer(@0)
                $scope.tempData.pKey = true;
                console.log(serviceURLArray[3].substr(0,index3));
                for(var i =0; i < $scope.stores.length; i++){
                    if($scope.stores[i].substr($scope.stores[i].lastIndexOf(".")+1)==serviceURLArray[3].substr(0,index3)){
                        $scope.tempData.store = $scope.stores[i];
                        console.log($scope.stores[i])
                        break;
                    }
                }
            }else{
                $scope.tempData.pKey = false;
                console.log(serviceURLArray[3].substr(0,index4));
                for(var i =0; i < $scope.stores.length; i++){
                    if($scope.stores[i].substr($scope.stores[i].lastIndexOf(".")+1)==serviceURLArray[3].substr(0,index4)){
                        $scope.tempData.store = $scope.stores[i];
                        console.log($scope.stores[i])
                        break;
                    }
                }
                if(index1 > 0){
                    var tempString1 = serviceURLArray[3].substr(index1+8);
                    $scope.tempData.filter = tempString1.substr(0,tempString1.indexOf("'")-3)
                }
                if(index2 > 0){
                    var tempString2 = serviceURLArray[3].substr(index2+8);
                    $scope.tempData.select = tempString2.substr(0,tempString2.indexOf("'"))
                }
            }

        }
        console.log($scope.servicedata.inSchemaId)
        console.log($scope.servicedata.outSchemaId)
        console.log($scope.tempData.filter)
        console.log($scope.tempData.select)
    }

    $scope.DelService = function(id){
        $scope.serviceDisplay = true;
        dataInterface.remove({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig',ID: id}).$promise.then(
            function(value){
                console.log(value);
                $scope.servicedata={
                    id:0,
                    appAlias:"",
                    name:"",
                    serviceURL:"",
                    inSchemaId:"",
                    outSchemaId:"",
                    description:"",
                    version:""
                }
                $scope.$broadcast("reloadService");
            },
            function(error){
                console.log("error");
            })

    }
    $scope.SaveService = function(id){
        $scope.serviceDisplay = true;
//添加
        if($scope.serviceAdd){
            $scope.temp.serviceURL = $scope.tempData.httpMethod+":";
            if($scope.tempData.serviceType=="SVC"){
                $scope.temp.serviceURL = $scope.temp.serviceURL + "/"+ $scope.temp.appAlias+"/SVC/";
            }else if($scope.tempData.serviceType=="DSE"){
                $scope.temp.serviceURL = $scope.temp.serviceURL + "/"+ $scope.temp.appAlias+"/DSE/" + $scope.tempData.store.substr($scope.tempData.store.lastIndexOf(".")+1);
                if($scope.tempData.pKey==1){
                    $scope.temp.serviceURL = $scope.temp.serviceURL + "(@0)";
                }else{
                    if($scope.tempData.filter!="无"&&$scope.tempData.filter!=""){
                        $scope.temp.serviceURL = $scope.temp.serviceURL +"?$filter='"+$scope.tempData.filter+"=@0'";
                        if($scope.tempData.select!="无"&&$scope.tempData.select!=""){
                            $scope.temp.serviceURL = $scope.temp.serviceURL +"&$select='"+$scope.tempData.select+"'";
                        }
                    }else{
                        if($scope.tempData.select!="无"&&$scope.tempData.select!=""){
                            $scope.temp.serviceURL = $scope.temp.serviceURL +"?$select='"+$scope.tempData.select+"'";
                        }
                    }

                }

            }else if($scope.tempData.serviceType=="SME"){

            }
            console.log($scope.temp.serviceURL)
            dataInterface.put({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig',ID: $scope.temp.id},$scope.temp).$promise.then(
                function(value) {
                    if ($scope.tempData.serviceType == "SVC") {
                        $scope.temp.serviceURL = value.serviceURL + value.id;
                        $scope.temp.id = value.id;
                        dataInterface.save({
                            SPACE: "CFG",
                            METHOD: "DSE",
                            NAME: 'ServiceConfig',
                            ID: value.id
                        }, $scope.temp).$promise.then(
                            function (value) {
                                $scope.$broadcast("isSave");
                                $scope.$broadcast("reloadService");
                                $scope.serviceAdd = false;
                                $scope.serviceDisplay = true;
                                $scope.tempData={
                                    httpMethod :"",
                                    serviceType:"",
                                    paramForService:"",
                                    store:"",
                                    filter:"",
                                    select:"",
                                    pKey:true
                                };
                            },
                            function (error) {
                                console.log("error");
                                $scope.serviceAdd = false;
                                $scope.serviceDisplay = true;
                                $scope.tempData={
                                    httpMethod :"",
                                    serviceType:"",
                                    paramForService:"",
                                    store:"",
                                    filter:"",
                                    select:"",
                                    pKey:true
                                };
                            })
                    }else{
                        $scope.$broadcast("isSave");
                        $scope.$broadcast("reloadService");
                        $scope.serviceAdd = false;
                        $scope.tempData={
                            httpMethod :"",
                            serviceType:"",
                            paramForService:"",
                            store:"",
                            filter:"",
                            select:"",
                            pKey:true
                        };
                    }
                },
                function(error){
                    console.log("error");
                })

        }else{
            //修改
            $scope.servicedata.serviceURL = $scope.tempData.httpMethod+":";
            if($scope.tempData.serviceType=="SVC"){

                $scope.servicedata.serviceURL = $scope.servicedata.serviceURL+$scope.servicedata.appAlias+"/SVC/"+$scope.servicedata.id;
            }else if($scope.tempData.serviceType=="DSE"){
                $scope.servicedata.serviceURL = $scope.servicedata.serviceURL + "/"+ $scope.servicedata.appAlias+"/DSE/" + $scope.tempData.store.substr($scope.tempData.store.lastIndexOf(".")+1);
                if($scope.tempData.pKey==1){
                    $scope.servicedata.serviceURL = $scope.servicedata.serviceURL + "(@0)";
                }else{
                    if($scope.tempData.filter!="无"){
                        $scope.servicedata.serviceURL = $scope.servicedata.serviceURL +"?$filter='"+$scope.tempData.filter+"=@0'";
                        if($scope.tempData.select!="无"){
                            $scope.servicedata.serviceURL = $scope.servicedata.serviceURL +"&$select='"+$scope.tempData.select+"'";
                        }
                    }else{
                        if($scope.tempData.select!="无"){
                            $scope.servicedata.serviceURL = $scope.servicedata.serviceURL +"?$select='"+$scope.tempData.select+"'";
                        }
                    }

                }
            }

            console.log($scope.servicedata);
            dataInterface.save({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig',ID: id},$scope.servicedata).$promise.then(
                function(value){
                    console.log(value);
                    $scope.$broadcast("isSave");
                    $scope.$broadcast("reloadService");
                },
                function(error){
                    console.log("error");
                })
        }
    }

    $scope.CancelService = function(){
        $scope.$broadcast("isCancel");
        $scope.serviceDisplay = true;
    }

    //$scope.$on('getService',function(event,newValue){
    //    $scope.servicedata = newValue;
    //    $scope.servicedata.appAlias = newValue.appAlias;
    //    console.log(newValue)
    //
    //});

    $scope.$on("serviceTreeChange",
        function (event, treeNode, treeId) {
            if (treeNode.isParent == false) {
                dataInterface.get({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig',ID: treeNode.id}).$promise.then(
                    function(value){
                        $scope.svcDisplay = true;
                        $scope.servicedata = value;
                    },
                    function(error){
                        console.log("error");
                    })
            }
        });
})