myAppDirective.directive('myTable', function($compile,$templateCache){
    return {
        restrict: "E",
        templateUrl: 'components/directive/template/myTable.html',
        replace: true,
        link:function (scope, element, attributes) {
            scope.id = "table";

            //   var html = "< table ng-repeat='key in param.tableParam'> <tr ng-repeat=' value in param.tableParam[key]'> <td >{{value}}</td> </tr> </table>";
            //     var html = "&lt; table ng-repeat=\'key in param.tableParam\'&gt; &lt;tr ng-repeat=\' value in param.tableParam[key]\'&gt; &lt;td ng-modal= \'value\' &gt; &lt;/td&gt; &lt;/tr&gt; &lt;/table&gt;";
            var html =  "&lt;div ng-controller=\'myTableContrl\'&gt&lt;input type=\'text\' ng-model=\'param.input1\'&gt input1  &lt;\/input &gt &lt;input type=\'text\' ng-model=\'param.input2\'&gt input2 &lt;\/input &gt &lt;\/div&gt";
            var scriptStart = "pageApp.controller(\'myTableContrl\' ,[\'$scope\', function($scope){" +
                "$scope.param={};" +
                "$scope.param.input1='';"  +
                "$scope.param.input2='';" ;
            var scriptEnd="}]);";
            var injectScript ="$scope.param";
            var id = "myTable";
            var scriptParam = "$scope.param.input1 $scope.param.input2"
            var abc =  element.find("div")[2];

            //   $(abc).attr("ng-controller",id +"Contrl");
            $(abc).find(".pageContent").attr("id", id +"Script").attr("code", html).attr("scriptStart", scriptStart).attr("injectScript",injectScript).attr("scriptEnd" , scriptEnd).attr("scriptParam",scriptParam);

            $compile(element.contents())(scope);
        }
    }
});


myAppDirective.directive('myChannel',['$compile', function($compile,$templateCache){
    return {
        restrict: "E",
        scope: true,
        templateUrl: 'components/directive/template/myChannel.html',
        replace: true,
        link:function (scope, element, attributes) {
            scope.id="channel";
        //   $(".sidebar-nav .box").draggable({
            var a = $("#"+ scope.id);
            $(element).draggable({
                connectToSortable: ".column",
                helper: "clone",
                handle: ".drag",
                start: function(e,t) {

                },
                drag: function(e, t) {

                    t.helper.width(400)
                },
                stop: function(e, t) {
                    //     handleJsIds();
                    scope.id="channelgh";
                    var asf = element.contents();
                    element= $("<my-table></my-table>");
                    $compile("<my-table></my-table>")(scope);
                    return scope.$apply(
                        scope.id="channelgh"
                    )
                    if(stopsave>0) stopsave--;
                    startdrag = 0;


                    //      $("#editorModalOne").modal("toggle");
                }
            });
            //   var html = "< table ng-repeat='key in param.tableParam'> <tr ng-repeat=' value in param.tableParam[key]'> <td >{{value}}</td> </tr> </table>";
            //     var html = "&lt; table ng-repeat=\'key in param.tableParam\'&gt; &lt;tr ng-repeat=\' value in param.tableParam[key]\'&gt; &lt;td ng-modal= \'value\' &gt; &lt;/td&gt; &lt;/tr&gt; &lt;/table&gt;";
            var html =  "&lt;div ng-controller=\'myChannelContrl\'&gt&lt;input type=\'text\' ng-model=\'channel.description\'&gt channel  &lt;\/input &gt &lt;\/div&gt";
            var scriptStart = "pageApp.controller(\'myChannelContrl\' ,[\'$scope\', \'schemaBySpaceByNameById\' ,function($scope,schemaBySpaceByNameById){" +
                "$scope.param={};" +
                "$scope.param.channelId;"  +
                "$scope.channel={};"
            var scriptEnd="" +
                "}]);";
            var injectScript ="$scope.param.channelId";
            var id = "myTable";
            var scriptParam = "$scope.param.input1 $scope.param.input2"
            var abc =  element.find("div")[2];


            //   $(abc).attr("ng-controller",id +"Contrl");
            $(abc).find(".pageContent").attr("id", id +"Script").attr("code", html).attr("scriptStart", scriptStart).attr("injectScript",injectScript).attr("scriptEnd" , scriptEnd).attr("scriptParam",scriptParam);

            $compile(element.contents())(scope);
        }
    }
}]);


myAppDirective.directive('myGrid', function($compile,$templateCache){
    return {
        restrict: "E",
        templateUrl: 'components/directive/template/myGrid.html',
        replace: true,
        link:function (scope, element, attributes) {
            $(".sidebar-nav .box").draggable({
                connectToSortable: ".column",
                helper: "clone",
                handle: ".drag",
                start: function (e, t) {

                },
                drag: function (e, t) {

                    t.helper.width(400)
                },
                stop: function (e, t) {
                    //     handleJsIds();
                    $compile(element.contents())(scope);
                    if (stopsave > 0) stopsave--;
                    startdrag = 0;

                }
            });


            var html = "&lt;div ng-controller=\'gridDirectCtrl\' &gt&lt;table class=\'table\'&gt;" +
                "&lt;tr&gt;" +
                "&lt;th ng-repeat=\'th in table.data | tableFilter \'&gt;" +
                "&lt;div ng-bind=\'th.name\'&gt;&lt;/div&gt;" +
                "&lt;/th&gt;" +
                "&lt;th ng-show=\'table.isShowOperate\'&gt;" +
                "操作" +
                "&lt;/th&gt;" +
                "&lt;/tr&gt;" +
                "&lt;tr ng-repeat=\'data in dataEdit\'&gt;" +
                "&lt;td ng-repeat=\'th in table.data | tableFilter \'&gt;" +
                "&lt;div ng-bind=\'data[th.value]\'&gt;" +
                "&lt;/div&gt;" +
                "&lt;/td&gt;" +
                "&lt;td ng-show=\'table.isShowOperate\'&gt;" +
                "&lt;input type=\'button\' class=\'btn\' ng-click=\'updateDt(data)\' value=\'修改\'&gt;" +
                "&lt;input type=\'button\' class=\'btn\' ng-click=\'deleteDt(data)\' value=\'删除\'&gt;" +
                "&lt;/td&gt;" +
                "&lt;/tr&gt;" +
                "&lt;/table&gt;&lt;\/div&gt";

            var scriptStart = "pageApp.controller(\'gridDirectCtrl\', [\'$scope\',\'schemaBySpaceByNameById\',\'schemaByNameCms\' ,function (scope,schemaBySpaceByNameById,schemaByNameCms) {" +
                "scope.formId={}; scope.table={}; scope.dataEdit={};" ;

            var scriptEnd = "scope.newData = function (){" +
                "$(\'#controlDilog\').modal();}" +
                " ;scope.deleteDt=function(obj){for(var n=0;n< scope.dataEdit.length;n++){if(obj.id==scope.dataEdit[n].id){scope.dataEdit.splice(n,1);}}};" +
                "scope.updateDt=function(obj){console.log(\'更新\')};}]);";
            var injectScript = "";
            scope.id = "myGrid";
            var scriptParam = "scope.formId,scope.name"
            var abc = element.find("div")[2];

            //   $(abc).attr("ng-controller",id +"Contrl");
            $(abc).find(".pageContent").attr("id", scope.id + "Script").attr("code", html).attr("scriptStart", scriptStart).attr("scriptInject", injectScript).attr("scriptEnd", scriptEnd).attr("scriptParam", scriptParam);

            $compile(element.contents())(scope);

        }
    }});



myAppDirective.directive('myGridPrefer', function($compile,$templateCache){
    return {
        scope:{},
        restrict: "E",
        templateUrl: 'components/directive/template/myGridPrefer.html',
        replace: true,
        link:function (scope, element, attributes) {
            $(".sidebar-nav .box").draggable({
                connectToSortable: ".column",
                helper: "clone",
                handle: ".drag",
                start: function (e, t) {

                },
                drag: function (e, t) {

                    t.helper.width(400)
                },
                stop: function (e, t) {
                    //     handleJsIds();
                    num ++;
                    scope.id = id + num;

                    //   $(abc).attr("ng-controller",id +"Contrl");
                    element.find(".pageRefer").attr("id", scope.id + "Script").attr("pageReferId", 0).attr("type", 2)
                    $compile(element.contents())(scope);
                    if (stopsave > 0) stopsave--;
                    startdrag = 0;

                }
            });

            var id = "myGridRefer";
            var num =1 ;
            scope.id = id + num;


            //   $(abc).attr("ng-controller",id +"Contrl");
            element.find(".pageRefer").attr("id", scope.id + "Script").attr("pageReferId", 0).attr("type", 3);
            $compile(element.contents())(scope);

        }
    }});

