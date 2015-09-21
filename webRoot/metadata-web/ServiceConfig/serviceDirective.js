/**
 * Created by zhoutao1 on 2015/6/2.
 */

app.directive('servicetree', function (dataInterface2) {
    var zNodes=[];
    var isChange = true;
    return {

        restrict: 'A',
        link: function ($scope, element, attrs) {

            var setting = {
                view: {
                    showIcon: showIconForTree
                },
                data: {
                    key: {
                        title: ""
                    }

                },
                callback: {
                    onClick: function (event, treeId, treeNode, clickFlag) {
                        if(isChange){
                            $scope.$emit("serviceTreeChange",treeNode, treeId);
                        }

                    }
                }
            };

            function showIconForTree(treeId, treeNode) {
                return !treeNode.isParent;
            };

            zNodes =[];
            //zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
            zNodes.push({name:"System.Configuration.ServiceConfig",isParent:true,open:true,children:[]});
            dataInterface2.query({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig'}).$promise.then(
                function(data){
                    for(var i=0;i<data.length;i++){
                        console.log(data[i].name)
                        zNodes[0].children.push({name:data[i].name,children:[],open:false,id:data[i].id});
                    };
                    $.fn.zTree.init(element, setting, zNodes);
                }
            );
            $scope.$on('isModify',function(){
                isChange = false;
            });
            $scope.$on('isAdd',function(){
                isChange = false;
            });
            $scope.$on('isSave',function(){
                isChange = true;
            });
            $scope.$on('isCancel',function(){
                isChange = true;
            });
            $scope.$on('reloadService',function(){
                zNodes =[];
                //zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
                zNodes.push({name:"System.Configuration.ServiceConfig",isParent:true,open:true,children:[]});
                dataInterface2.query({SPACE:"CFG",METHOD:"DSE",NAME:'ServiceConfig'}).$promise.then(
                    function(data){
                        for(var i=0;i<data.length;i++){

                            zNodes[0].children.push({name:data[i].name,children:[],open:false,id:data[i].id});
                        };
                        $.fn.zTree.init(element, setting, zNodes);
                    }
                );
            })
        }
    };
});


