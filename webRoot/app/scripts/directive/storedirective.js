

app.directive('storetree', function () {
    var zNodes =[];
    return {
       
        restrict: 'A',
        link: function ($scope, element, attrs) {
           console.log("onclicktest",'=onClickTest');
           console.log("showIcon",'=showIcon');
           console.log("element",element);
           console.log("attrs",attrs);
           
            var setting = {
                view: {
                   showIcon: '=showIcon'
                },
                data: {
                    key: {
                        title: ""
                    },
                  
                },
                callback: {

                    onClick: function (event, treeId, treeNode, clickFlag) {
                      if(treeNode.isParent == true){
                         $scope.$emit("storeChange", treeNode);
                     }else{
                         $scope.$emit("schemaChange",treeNode);
                     }
                    },

                    //处理展开事件
                    onExpand:function(event, treeId, treeNode) {
                        //get schema in the store
                      
                        var URL =  "/MDE/DSE/SetMetadata(\'"+treeNode.name+"\')";
                        $.get(
                            URL,
                            function(data){     
                                var schemaNodes = data.entitySchemaList;
                                var storeNodes = zNodes[0].children;

                                for(var j =0 ;j<storeNodes.length;j++){
                                    if(storeNodes[j].name == treeNode.name&&storeNodes[j].open == false){               
                                            storeNodes[j].open = true;
                                            if(storeNodes[j].children.length==0){
                                                 for(var i=0;i<schemaNodes.length;i++){
                                                 storeNodes[j].children.push({id:i,name:schemaNodes[i].schemaName,children:[],isParent:false});
                                            }                    
                                        }                                        
                                    }
                                }   

                                $.fn.zTree.init(element, setting, zNodes);      
                            }
                        );


                    },//onExpand

                    //处理折叠事件，未完成
                    onCollapse:function(event,treeid,treeNode){
                      
                        var storeNodes = zNodes[0].children;
                        for(var i =0 ;i<storeNodes.length;i++){
                            if(storeNodes[i] == treeNode.name){
                                storeNodes[i].open = false;
                            }
                        }

                    }//onExpand
                }
            };

            function showIconForTree(treeId, treeNode) {
            return !treeNode.isParent;
        };
             zNodes = [];
             zNodes.push({name:$scope.space.realname,isParent:true,open:true,children:[]});
             
             console.log("spacename"+$scope.space.realname);
           
              var URL =  "/MDE/DSE/EntitySet?$filter=appSpaceId eq \'"+$scope.space.realname+"\' &$select=entitySetName";

				$.get(
				  URL,
				  function(data){ 
				     for(var i=0;i<data.length;i++){
				     	zNodes[0].children.push({name:data[i].entitySetName,isParent:true,children:[],open:false});  
					 };	
					 $.fn.zTree.init(element, setting, zNodes);		
				});         
        }
    };
});