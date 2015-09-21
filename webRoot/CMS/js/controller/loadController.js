var loadApp=angular.module('loadApp',['ngResource','ngRoute']).config(function ($routeProvider) {
    $routeProvider
        .when('/preview', {
            template: function(){
                return $("#showPage").html();
            },
            controller: ''
        })
        .when('/schema', {
            templateUrl: 'page/schema.html',
            controller: ''
        })
        .when('/storeinfo', {
            templateUrl: 'page/storeinfo.html',
            controller: ''
        })
});;

loadApp.controller('loadController' ,['$scope','schemaByNameById', function($scope,schemaByNameById) {
    $scope.pageid = "";
    $scope.pageSchema={};
    $scope.loadPageSchema = function(){
        schemaByNameById.get({NAME:"page",ID: $scope.pageid}, $scope.pageSchema);
        alert($scope.pageSchema);
        $scope.pageSchema = {"id": 2,
            "appSpace": "CMS.PAGE",
            "data": {"class": "container-fluid", "type": 1, "pageContent": {}, "pageRefer": "", "script": {}, "children": [
                {"class": "row-fluid", "type": 1, "pageContent": {}, "pageRefer": "", "script": {}, "children": [
                    {"class": "row-fluid", "type": 1, "pageContent": {}, "pageRefer": "", "script": {}, "children": [
                        {"class": "span12", "type": 1, "pageContent": {}, "pageRefer": "", "script": {}, "children": [
                            {"class": "pageContent", "type": 2, "pageContent": {"html": "< table ng-repeat='key in param.tableParam'> <tr ng-repeat=' value in param.tableParam[key]'> <td ng-modal= 'value' > </td> </tr> </table>", "script": {}}, "pageRefer": "", "script": {"scriptStart": "layoutApp.controller('myTableContrl' ,['$scope', function($scope){$scope.pageId='pageOne;$scope.param={'tableParam':{}}; }]);", "scriptEnd": "}]);", "injectScript": "$scope.param.tableParam={}", "scriptParam": "$scope.param.tableParam"}, "children": [

                            ]}
                        ]}
                    ]}
                ]}
            ]}
        };

        var buildpage = $scope.pageSchema["data"];
        formatToHTML(buildpage);
    };

    var formatToHTML = function (buildpage){
            this.json = {"class":"container-fluid","type":1, "pageContent":{}, "pageRefer":"", "script":{},"children":[]};
            var child = "<div></div>";
            var stackHtml=[];
            var stackJson=[];
            stackJson.push(buildpage);
            stackHtml.push($(child).appendTo($("#showPage")));
            var stringHtml="";
            while(stackJson.length !=0)
            {

                var currentJson = stackJson.pop();
                var currentNode =stackHtml.pop();
                if(currentJson["type"] == 1)
                {

                    currentNode.attr("class", currentJson["class"]);
                }
                else if(currentJson["type"] == 2)
                {
                    currentNode.attr("class", currentJson["class"]);
                    currentNode.append(currentJson["pageContent"]["html"]);
                    var script = currentJson["script"]["scriptStart"].substr(0,currentJson["script"]["scriptStart"].length-currentJson["script"]["scriptEnd"].length);
                    script += currentJson["script"]["injectScript"] +  currentJson["script"]["scriptEnd"];

                    var myScript = document.createElement("script");
                    myScript.text = script;
                    document.body.appendChild(myScript);
                }
                else if(currentJson["type"] == 3)
                {

                }
                else {
                    continue;
                }
                for(var i= currentJson.children.length-1; i>=0;  i--)
                {
                    stackJson.push(currentJson.children[i]);
                    stackHtml.push($(child).appendTo(currentNode));
                }
            }
    };
    $scope.producePageFile = function(){
        var blob= new Blob($("#showPage").html());
        saveAs(blob, "page.html");
    };

}]);


loadApp.factory('schemaByNameById',['$http','$resource',function($http,$resource){
    return $resource('http://localhost:8080/MDE/DSE/:NAME(\':ID\')',
        {NAME:'@name',ID:'@id'},
        { put:{method:"PUT"}, params:{},isArray: false}) ;
}]);