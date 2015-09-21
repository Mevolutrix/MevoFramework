/**
 * Created by zhoutao1 on 2015/6/2.
 */


app.directive('svctree', function (dataInterface2) {
    var zNodes=[];
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
                        $scope.$emit("svcTreeChange",treeNode);
                    }
                }
            };

            function showIconForTree(treeId, treeNode) {
                return !treeNode.isParent;
            };

            zNodes =[];
            //zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
            zNodes.push({name:"System.Configuration.SVCConfig",isParent:true,open:true,children:[]});
            dataInterface2.query({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig'}).$promise.then(
                function(data){
                    for(var i=0;i<data.length;i++){
                        zNodes[0].children.push({name:data[i].alias,children:[],open:false});
                    };
                    $.fn.zTree.init(element, setting, zNodes);
                }
            );

            $scope.$on('reloadSVC',function(){
                zNodes =[];
                //zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
                zNodes.push({name:"System.Configuration.SVCConfig",isParent:true,open:true,children:[]});
                dataInterface2.query({SPACE:"CFG",METHOD:"DSE",NAME:'SVCConfig'}).$promise.then(
                    function(data){
                        for(var i=0;i<data.length;i++){
                            zNodes[0].children.push({name:data[i].alias,children:[],open:false});
                        };
                        $.fn.zTree.init(element, setting, zNodes);
                    }
                );
            })
        }
    };
});

