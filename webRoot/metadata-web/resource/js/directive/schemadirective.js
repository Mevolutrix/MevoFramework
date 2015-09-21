app.directive('schematree', function () {
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
                    },
                  
                },
                callback: {
                    onClick: function (event, treeId, treeNode, clickFlag) {
                             $scope.$emit("schemaChange",treeNode);    
                     }
                }
            };

            function showIconForTree(treeId, treeNode) {
            return !treeNode.isParent;
            };

             zNodes =[];
             zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
             var URL = "/MDE/DSE/EntitySchema?$filter=appSpaceId eq \'"+$scope.space.realname+"\' &$select=id";

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


