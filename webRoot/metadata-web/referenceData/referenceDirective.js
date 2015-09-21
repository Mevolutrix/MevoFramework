

app.directive('refertree', function () {
    var zNodes=[];
    return {
        scope:{
            schema:"@schema",
            event:"@event"
        },
        restrict: 'A',
        link: function ($scope, element, attrs) {

            var setting = {
                view: {
                    showIcon: showIconForTree
                },
                data: {
                    key: {
                        title: ""
                    },

                },
                callback: {
                    onClick: function (event, treeId, treeNode, clickFlag) {
                        $scope.$emit($scope.event,treeNode);
                    }
                }
            };

            function showIconForTree(treeId, treeNode) {
                return !treeNode.isParent;
            };

            zNodes =[];
            zNodes.push({name:$scope.$parent.space.realname,isParent:true,open:true,children:[]});
            var URL = "/MDE/DSE/"+$scope.schema+"?$filter=appSpaceId eq \'"+$scope.$parent.space.realname+"\' &$select=id";

            $.get(
                URL,
                function(data){
                    for(var i=0;i<data.length;i++){
                        zNodes[0].children.push({name:data[i].id,children:[],open:false});
                    };
                    $.fn.zTree.init(element, setting, zNodes);
                });
        }
    };
});

app.directive('formRefer', function ($http,schemaByNameById,$timeout,schemaBySpaceByNameById) {

    return {
     /* scope: {
            data:'='
        }, */
        restrict: 'E',
        replace : true,
        templateUrl:"/MDE/Portal/referenceData/template/formReference.html",
        link: function (scope, element, attrs) {

           scope.schemadata = {
               schema: '',
               properties: '',
               isModify: '@'
           };

           scope.$on('data',function(event,newValue){
               scope.tempdata =newValue;
               if(scope.isModify ==true) {
                   $("#isUpdateModal").modal();
               }
               else{
                   scope.data =newValue;
               }

           });

            scope.updateData = function(){
                scope.data =   scope.tempdata;
            };

            scope.createRefer= function(){
                var URL = "/MDE/DSE/EntitySchema?$filter=appSpaceId eq \'" + scope.space.realname + "\'";

                $http({
                    url: URL,
                    method: "GET"
                }).success(
                    function (data) {
                        scope.schemas  = data;
                    });
                scope.firstShow = false;
                scope.secondShow = true;
                scope.title = "新增枚举值";
                scope.createFlag = true;
                $('#selectColumns').modal();
                $(".ex_schema_info").each(function(){
                    this.disabled = false;
                });
                scope.data.appSpaceName = scope.$parent.space.realname;
                scope.idInput ='';
                scope.isModify = true;
            };
            scope.selectSchema =function(){
                 if(scope.schemadata.schema!=''){
                     scope.properties =  scope.schemadata.schema.properties;

                     $("#id").each(function(){
                         this.readOnly = true;
                     });
                 }
            };
            scope.selectProperty =function(){
                if(scope.schemadata.property !=''){
                    scope.idInput= scope.space.name + '.' + scope.schemadata.schema.id.split('.').pop() + '.' +  scope.schemadata.property.name;
                    $("#id").each(function(){
                        this.readOnly = false;
                    });
                }
            };
            scope.saveId = function(){
                scope.data.id  = scope.idInput;
                scope.data.propertyMap = [];

                var ChannelData = {"id":34000,"name":"Content","isShowOperate":true,"filter":[],"isDragRow":true,"operateBtn":[{"id":1,"name":"删除","class":"btn-xs btn-danger","events":[{"id":1,"event":1,"function":"deleteData","script":"function($event,$index){schemaBySpaceByNameById.delete({SPACE:$scope.space, NAME:$scope.schemaName,ID:$scope.schema[$scope.index].id},function(data) {  console.log(data);$scope.$emit('filterContents', $scope.schema[$scope.index].parentId);},function(data){console.log(data);});} "}]}],"formId":0,"showType":"Grid","description":"","data":[{"id":1,"name":"内容","value":"content","isShow":true,"isSort":true,"index":1,"events":[],"isCustom":false}, {"id":2,"name":"标题","value":"title","isShow":false,"isSort":true,"index":2,"events":[{"id":3,"event":1,"function":"modifyContent","script":"function($event, $index){$scope.schemaInput= $scope.schema[$scope.index];$scope.$emit('form');}"}],"isCustom":false}, {"id":3,"name":"id","value":"id","isShow":false,"isSort":true,"index":3,"events":[],"isCustom":false}, {"id":4,"name":"类型","value":"type","isShow":false,"isSort":true,"index":4,"events":[],"isCustom":false}, {"id":5,"name":"父类Id","value":"parentId","isShow":false,"isSort":true,"index":5,"events":[],"isCustom":false}, {"id":6,"name":"开始时间","value":"beginData","isShow":false,"isSort":true,"index":6,"events":[],"isCustom":false}, {"id":7,"name":"结束时间","value":"endData","isShow":false,"isSort":true,"index":7,"events":[],"isCustom":false}, {"id":8,"name":"创建时间","value":"createData","isShow":false,"isSort":true,"index":8,"events":[],"isCustom":false}, {"id":9,"name":"作者","value":"author","isShow":false,"isSort":true,"index":9,"events":[],"isCustom":false}, {"id":10,"name":"其他","value":"extentProperty","isShow":false,"isSort":true,"index":10,"events":[],"isCustom":false}],"appSpace":"MDE","path":"System.Metadata"};
                schemaBySpaceByNameById.save({Space:'MDE', NAME:'gridList', ID:ChannelData.id},ChannelData);
            };
          scope.modifyRefer = function(){
                scope.isModify = true;
            };
            scope.cancelRefer = function(){
                scope.isModify = false;
                scope.isModify =  scope.isModify;
            };
            scope.submitRefer = function(){
              //提交代码
                schemaByNameById.delete({NAME:'PropertyReference', ID:scope.data.id});
                schemaByNameById.put({NAME:'PropertyReference', ID:scope.data.id},scope.data);
                scope.cancelRefer();
            };

        }
    };
});


app.directive('propertyMap', function(schemaByNameById){
    return {
        scope:{
            propertyMap:'=myProperty',
            isModify :'=isModify'
        },

        templateUrl: 'referenceData/template/propertyMap.html',
        restrict: 'E',
        replace: true,
        link: function (scope, element, attrs) {
            scope.add = function(){
              scope.isShowAddInput = true;
            };
            scope.confirm = function(){
                scope.propertyMap.push({key:scope.keyInput, description: scope.descriptionInput});
                scope.isShowAddInput = false;
            };
            scope.cancel = function(){
                scope.isShowAddInput = false;
            };
            scope.delete = function($index){
                scope.propertyMap.remove($index);
            };
            scope.modify = function($index){
                scope.keyInput  =  scope.propertyMap[$index].key;
                scope.descriptionInput = scope.propertyMap[$index].description;
                scope.idxSave = $index;
                $("#modifyTab").modal();
            };
            scope.save =function(){
                scope.propertyMap[scope.idxSave].key  = scope.keyInput ;
                scope.propertyMap[scope.idxSave].description = scope.descriptionInput;
            };


        }
    }
});



