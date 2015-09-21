/**
 * Created by zhoutao1 on 2015/6/2.
 */
app.controller('svcCtrl', ['$scope',"$http","$timeout","$compile",'dataInterface',function($scope,$http,$timeout,$compile,dataInterface){
    $scope.svcDisplay = true;
    $scope.svcAdd = false;
    $scope.space = spaceModel;
    $scope.readAsBinary = function(){
        var file = document.getElementById("file").files[0];
        console.log(file.value)
        console.log(file)
        if(file.substr(file.lastIndexOf(".")+1).toLowerCase()!="class"){

            return;
        }
        var reader = new FileReader();
        //reader.readAsText(file);
        reader.readAsBinaryString(file);
        reader.onload = upLoaded;
    }

    var upLoaded = function(evt){
        var fileString = evt.target.result;
        var byteArray="";
        console.log(fileString.length)
        for( var i = 0; i< fileString.length; i++){
            var ch = fileString.charCodeAt(i);
            var zero = '00';
            var tmp = 2-((ch &0xFF).toString(16)).length;
            byteArray = byteArray+zero.substr(0,tmp)+(ch &0xFF).toString(16);
        }
        $scope.temp.jarPath=null;
        $scope.temp.assembly = byteArray;
        dataInterface.put({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: $scope.temp.alias},$scope.temp).$promise.then(
            function(value){
                console.log(value);
                $scope.$broadcast("reloadSVC");
            },
            function(error){
                console.log("error");
            })
    }
    $scope.AddSVC = function(){
        $scope.svcDisplay = false;
        $scope.svcAdd = true;
        $scope.temp ={
            alias : "",
            storedType: 0,
            svcObjFullName:"",
            jarPath:null,
            assembly:null
        }

    }
    $scope.ModifySVC = function(alias){
        console.log($scope)
        $scope.svcDisplay = false;
        $scope.oldalias = alias;

    }

    $scope.DelSVC = function(alias){
        $scope.svcDisplay = true;
        dataInterface.remove({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: alias}).$promise.then(
            function(value){
                console.log(value);
                $scope.$broadcast("reloadSVC");
            },
            function(error){
                console.log("error");
            })

    }
    $scope.SaveSVC = function(alias){
        $scope.svcDisplay = true;
        if($scope.svcAdd){   //增加svcConfig
            $scope.temp.storedType = Number($scope.temp.storedType);
            console.log($scope.temp.storedType)
            if($scope.temp.storedType == 2){//jar包
                $scope.temp.assembly = null;
                dataInterface.put({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: $scope.temp.alias},$scope.temp).$promise.then(
                    function(value){
                        console.log(value);
                        $scope.$broadcast("reloadSVC");
                    },
                    function(error){
                        console.log("error");
                    })
            }else if($scope.temp.storedType == 3){ //class文件
                var file = document.getElementById("file").files[0];
                if(file.name.substr(file.name.lastIndexOf(".")+1).toLowerCase()!="class"||file.type!="java/*"){
                    alert("请上传.class文件！")
                    $scope.svcAdd = true;
                    return;
                }
                if(file.size/1024>10){
                    alert("文件超过10k，建议配置jar包")
                    $scope.svcAdd = true;
                    return;
                }
                var reader = new FileReader();
                reader.readAsBinaryString(file);
                reader.onload = upLoaded;
            }

            $scope.svcAdd = false;
        }else{    //修改
            $scope.svcdata.storedType = Number($scope.svcdata.storedType);
            if($scope.oldalias == alias){
                console.log($scope.svcdata);
                dataInterface.save({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: alias},$scope.svcdata).$promise.then(
                    function(value){
                        console.log(value);
                    },
                    function(error){
                        console.log("error");
                    })
            }else{
                console.log("============not same===========");
                dataInterface.remove({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: $scope.oldalias}).$promise.then(
                    function(value){
                        console.log(value);
                    },
                    function(error){
                        console.log("error");
                    })
                dataInterface.put({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: alias},$scope.svcdata).$promise.then(
                    function(value){
                        console.log(value);
                    },
                    function(error){
                        console.log("error");
                    })
            }
        }



    }
    $scope.CancleSVC = function(){
        $scope.svcDisplay = true;
    }


    $scope.$on("svcTreeChange",
        function (event, treeNode) {
            $scope.svcDisplay = true;
            $scope.svcAdd = false;
            console.log(treeNode.name);
            if (treeNode.isParent == false) {
                dataInterface.get({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig',ID: treeNode.name}).$promise.then(
                    function(value){
                        $scope.svcDisplay = true;
                        $scope.svcdata = value;
                        $scope.oldalias = treeNode.name;
                    },
                    function(error){
                        console.log("error");
                    })
            }


        });
}])