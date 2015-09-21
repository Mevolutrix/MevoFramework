/*app.controller('schemaTreeController', function ($scope) {

});  */

app.controller('propertyReferCtl',function($scope,schemaInstance,schemaByNameById, schemaByName ,$filter) {




    //=======
    $scope.isModify = false;
    $scope.data={
        id:'',
        appSpaceName:'',
        propertyMap:[]
    }

    $scope.title = "查看元数据";

    $(".schema_info").each(function () {
        this.disabled = true;
    })

    $scope.$on("referTreeChange",
        function (event, treeNode) {
            $scope.title = "查看元数据";
            //  $scope.editFlag = true;

            if (treeNode.isParent == false) {
                $scope.$apply(schemaByNameById.get({NAME:'PropertyReference',ID: treeNode.name}, function (data) {
                   $scope.data = data;
                    $scope.$broadcast("data",$scope.data);
                }));

                $scope.firstShow = true;
                $scope.secondShow = false;

            }

        });
   });












