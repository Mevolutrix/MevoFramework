/**
 * Created by zhangyw on 2015-5-28.
 */

var pageApp = angular.module('pageApp', ['ngResource'], function ($locationProvider) {
    $locationProvider.html5Mode({enabled: true, requireBase: false});
});

pageApp.controller('routeParamsController', ['$scope', '$location', '$timeout', function ($scope, $location, $timeout) {
    $timeout(function () {
        for (var key in $location.$$search) {
            $scope.$broadcast(key, $location.$$search[key])
        }
        ;
    }, 5, true);
}]);


pageApp.factory('schemaBySpaceByNameById', ['$http', '$resource', function ($http, $resource) {
    return $resource('/:SPACE/DSE/:NAME(\':ID\')',
        {SPACE: '@space', NAME: '@name', ID: '@id'},
        {put: {method: "PUT", params: {}, isArray: false}},
        {post: {method: "POST", params: {}, isArray: false}},
        {get: {method: "GET", params: {}, isArray: false}},
        {delete: {method: "DELETE", params: {}, isArray: false}});
}]);

pageApp.factory('schemaBySpaceByNameByfilter', ['$http', '$resource', function ($http, $resource) {
    return $resource('/:SPACE/DSE/:NAME?$filter=:COLUMN eq :FILTER',
        {SPACE: '@space', NAME: '@name', COLUMN: '@column', FILTER: '@filter'},
        {
            put: {method: "PUT", params: {}, isArray: true},
            get: {method: "GET", params: {}, isArray: true}
        });
}]);

pageApp.factory('instance', function () {
    return {};
})


pageApp.filter('tableFilter', function () {
    return function (obj) {
        if (!obj) return obj;
        var temp = obj;
        for (var n = 0; n < temp.length; n++) {
            if (!temp[n].isShow) {
                temp.splice(n, 1);
            }
        }
        return temp;
    }
});

pageApp.filter('gridDataFilter', function () {
    return function (obj, name, value) {
        if (!angular.isArray(obj)) return obj;
        if (!name) return obj;
        if (value == null || value == undefined) return obj;
        return obj.filter(function (item) {
            return item[name] == value;
        });
    }
});
pageApp.directive('listbtnDirective', ['$compile', 'schemaBySpaceByNameById', function (compile, schemaBySpaceByNameById) {
    return {
        restrict: 'E',
        scope: {

            eventEnume: "=eventEnume",//事件枚举数据
            funcArray: "=funcArray",
            index: "=index",
            tableTh: "=gridList",
            schema: "=schema",
            space: "=space",
            schemaName: "=schemaName"
            //     tempData: "=tempData"
        },
        link: function ($scope, element, attr) {


            //        $scope.$on("initParam", function(data) {
            //             $scope.schemaInput = data.targetScope["schemaInput"];
            //             $scope.tableTh = data.targetScope["gridList"];
            var btn = document.createElement('div');
            var evt = "";
            //-----------------------------------
            //构造按钮及按钮事件
            for (var n = 0; n < $scope.tableTh.operateBtn.length; n++) {
                //创建按钮
                var el = document.createElement('input');
                $(el).attr("type", "button");
                $(el).attr("class", $scope.tableTh.operateBtn[n].class);
                $(el).attr("value", $scope.tableTh.operateBtn[n].name);

                //拼接事件
                for (var i = 0; i < $scope.tableTh.operateBtn[n].events.length; i++) {
                    var enums = $scope.tableTh.operateBtn[n].events[i].event;
                    for (var e = 0; e < $scope.eventEnume.length; e++) {
                        if (enums == $scope.eventEnume[e].key) {
                            //给按钮添加事件
                            $(el).attr($scope.eventEnume[e].description, $scope.tableTh.operateBtn[n].events[i].function + "($event,$index)");
                            //-----事件------
                            if ($scope.funcArray.length == 0) {
                                var funStr = "$scope.#name=#body;";
                                evt += funStr.replace("#name", $scope.tableTh.operateBtn[n].events[i].function).replace('#body', $scope.tableTh.operateBtn[n].events[i].script);
                            }
                        }
                    }
                    if ($scope.funcArray.length > 0) {
                        for (var on = 0; on < $scope.funcArray.length; on++) {


                            if ($scope.tableTh.operateBtn[n].events[i].id == $scope.funcArray[on].id)
                                $scope[$scope.tableTh.operateBtn[n].events[i].function] = $scope.funcArray[on].func;
                        }
                    } else {
                        //执行事件
                        eval(evt);
                    }
                }
                $(btn).append(el);
            }
            $(element).append(compile(btn)($scope));
            /*   $scope.deleteData = function($event,$index){

             schemaBySpaceByNameById.delete({SPACE:$scope.space, NAME:$scope.schemaName,ID:$scope.schema[$scope.index].id},function(data) {
             console.log(data);
             $scope.$emit("filterContents", $scope.schema[$scope.index]);
             },function(data){
             console.log(data);

             });
             }  */
            //       });


        }
    }
}]);
pageApp.directive('alistDirective', ['$compile', function (compile) {
    return {
        restrict: 'E',
        scope: {
            allData: "=allData",//行数据
            lname: "=labelName",
            schema: "=schema",//所有业务数据
            eventEnume: "=eventEnume",//事件枚举数据
            funcArray: "=funcArray",
            index: "=index"
        },
        replace: true,
        link: function ($scope, element, attr) {
            //       $scope.$on("initParam", function(data) {

            //           $scope.schema = data.targetScope["schema"];
            //     $scope.tableTh = data.targetScope["gridList"];
            var aElement = document.createElement("a");
            $(aElement).attr("href", "javaScript:void(0)");
            $(aElement).attr("ng-bind", "lname");
            $scope.data = $scope.allData.data;
            for (var n = 0; n < $scope.data.length; n++) {
                for (var i = 0; i < $scope.data[n].events.length; i++) {
                    var funStr = "$scope.#name=#body;";
                    funStr = funStr.replace("#name", $scope.data[n].events[i].function).replace('#body', $scope.data[n].events[i].script);
                    var enums = $scope.data[n].events[i].event;
                    for (var e = 0; e < $scope.eventEnume.length; e++) {
                        if (enums == $scope.eventEnume[e].key) {
                            //添加事件
                            $(aElement).attr($scope.eventEnume[e].description, $scope.data[n].events[i].function + "($event,$index)");
                        }
                    }
                    if ($scope.funcArray.length > 0) {
                        for (var on = 0; on < $scope.funcArray.length; on++) {
                            console.log($scope.data[n].events[i].function);
                            console.log($scope.funcArray[on].func);
                            if ($scope.data[n].events[i].id == $scope.funcArray[on].id) {
                                $scope[$scope.data[n].events[i].function] = $scope.funcArray[on].func;
                            }
                        }
                    } else {
                        eval(funStr);
                    }
                }
            }
            $(element).append(compile(aElement)($scope));
            /*     $scope.modifyContent = function($event, $index){
             $scope.schemaInput= $scope.schema[$scope.index];
             $scope.$emit("form");
             //       $("#form").modal();
             } */
            //    });


        }
    }
}
]);


pageApp.directive("formTemplate", ['schemaBySpaceByNameById', function (schemaBySpaceByNameById) {
    return {
        restrict: 'E',
        scope: {},
        replace: true,
        templateUrl: "formTemplate.html",
        link: function ($scope, element, attr) {
            $scope.schemaInput = {extentProperty: []};

            $scope.$on("getParam", function (data, d) {
                $scope.schemaInput = d;
                $scope.space = data.targetScope["space"];
                $scope.schemaName = data.targetScope["schemaName"];
            });


            $scope.create = function () {
                schemaBySpaceByNameById.put({
                    SPACE: $scope.space,
                    NAME: $scope.schemaName,
                    ID: $scope.schemaInput.id
                }, $scope.schemaInput, function (data) {
                    $scope.$emit("filterContents", $scope.schemaInput.parentId);
                })
            };

            $scope.modify = function () {
                schemaBySpaceByNameById.save({
                    SPACE: $scope.space,
                    NAME: $scope.schemaName,
                    ID: $scope.schemaInput.id
                }, $scope.schemaInput, function (data) {
                    $scope.$emit("filterContents", $scope.schemaInput.parentId);
                })
            };


        }

    }
}]);

pageApp.directive("gridForm", ['schemaBySpaceByNameById', function (schemaBySpaceByNameById) {
    return {
        restrict: 'E',
        scope: {},
        replace: true,
        templateUrl: "gridFormTemplate.html",
        link: function ($scope, element, attr) {
            $scope.gridInput = {extentPropertySchema: []};
            $scope.epshow = false;
            $scope.extentPSchema = [
                {
                    name: "data",
                    value: ""
                },
                {
                    name: "image",
                    value: []
                },
                {
                    name: "video",
                    value: ""
                },
                {
                    name: "table",
                    value: ""
                },
                {
                    name: "link",
                    value: ""
                },
                {
                    name: "form",
                    value: ""
                },
                {
                    name: "favor",
                    value: ""
                },
                {
                    name: "recommend",
                    value: ""
                },
            ];
            $scope.type = [
                {id: 1, name: 'FloatingPoint'},
                {id: 2, name: 'UTF8String'},
                {id: 3, name: 'EmbeddedDocument'},
                {id: 5, name: 'BinaryData'},
                {id: 7, name: 'ObjectId'},
                {id: 8, name: 'Boolean'},
                {id: 9, name: 'UTCDatetime'},
                {id: 11, name: 'RegularExpression'},
                {id: 12, name: 'DBPointer'},
                {id: 14, name: 'JavaScriptCode'},
                {id: 15, name: 'Symbol'},
                {id: 16, name: 'Int32'},
                {id: 17, name: 'Timestamp'},
                {id: 18, name: 'Int64'},
                {id: 19, name: 'Decimal'},
                {id: 20, name: 'NullElement'},
                {id: 21, name: 'Int8'},
                {id: 22, name: 'Int16'},
                {id: 23, name: 'Single'}
            ];
            $scope.$on("getgridparam", function (data) {
                $scope.gridInput = data.targetScope["gridInput"];
                $scope.space = data.targetScope["space"];
                $scope.schemaName = data.targetScope["schemaName"];
            });


            $scope.newEP = function () {
                $scope.epInput = {
                    name: "",
                    description: "",
                    pType: 2,
                    complexTypeName: "",
                    nullable: true,
                    isArray: "",
                    verificationRegEx: {}
                };
                $scope.epInput.name = "";
                $scope.epInput.description = "";
                $scope.epshow = true;

            };

            $scope.confirmEP = function () {
                $scope.epshow = false;
                $scope.epInput.name = $scope.selectTemp.name;
                $scope.epInput.isArray = $scope.epInput.name == "image" ? true : false;
                $scope.gridInput.extentPropertySchema.push($scope.epInput);
            }
            $scope.cancelEP = function () {
                $scope.epshow = false;
            };
            $scope.deleteEP = function ($index) {
                $scope.gridInput.extentPropertySchema.splice($index, 1);
            }


            $scope.createGrid = function () {
                $scope.gridInput.id = 0;
                $scope.gridInput.parentId = Number($scope.gridInput.parentId);
                $scope.gridInput.pageid = Number($scope.gridInput.pageid);
                schemaBySpaceByNameById.put({
                    SPACE: $scope.space,
                    NAME: $scope.schemaName,
                    ID: $scope.gridInput.id
                }, $scope.gridInput, function (data) {
                    $scope.$emit("refresh", $scope.gridInput);
                })
            }

            $scope.modifyGrid = function () {
                $scope.gridInput.id = Number($scope.gridInput.id);
                $scope.gridInput.parentId = Number($scope.gridInput.parentId);
                $scope.gridInput.pageid = Number($scope.gridInput.pageid);
                schemaBySpaceByNameById.save({
                    SPACE: $scope.space,
                    NAME: $scope.schemaName,
                    ID: $scope.gridInput.id
                }, $scope.gridInput, function (data) {
                    $scope.gridInput = data;
                    $scope.$emit("refresh", $scope.gridInput);
                })

            };


        }
    }
}]);
pageApp.directive('buttonDirective', ['$compile', 'schemaBySpaceByNameById', function ($compile, schemaBySpaceByNameById) {
    return {
        restrict: 'E',
        scope: {
            data: "=data",
            tableTh: "=th",
            dataEdit: "=edit",
            eventEnume: "=eventEnume",
            funcArray: "=funcArray",
            index: "=index",
            space: "=space",
            schemaName: "=schemaName",
            dto: "=dto"
        },
        link: function ($scope, element, attr) {
            if ($scope.tableTh == null) return;
            var btn = document.createElement('div');
            var evt = "";

            for (var n = 0; n < $scope.tableTh.operateBtn.length; n++) {

                var el = document.createElement('input');
                $(el).attr("type", "button");
                $(el).attr("class", $scope.tableTh.operateBtn[n].class);
                $(el).attr("value", $scope.tableTh.operateBtn[n].name);

                for (var i = 0; i < $scope.tableTh.operateBtn[n].events.length; i++) {
                    var enums = $scope.tableTh.operateBtn[n].events[i].event;
                    for (var e = 0; e < $scope.eventEnume.length; e++) {
                        if (enums == $scope.eventEnume[e].key) {
                            $(el).attr($scope.eventEnume[e].description, $scope.tableTh.operateBtn[n].events[i].function + "(data)");

                            if ($scope.funcArray.length == 0) {
                                var funStr = "$scope.#name=#body;";
                                evt += funStr.replace("#name", $scope.tableTh.operateBtn[n].events[i].function).replace('#body', JXG.Util.UTF8.decode(JXG.Util.Base64.decode($scope.tableTh.operateBtn[n].events[i].script)));
                            }
                        }
                    }
                    if ($scope.funcArray.length > 0) {
                        for (var on = 0; on < $scope.funcArray.length; on++) {
                            if ($scope.tableTh.operateBtn[n].events[i].id == $scope.funcArray[on].id)
                                $scope[$scope.tableTh.operateBtn[n].events[i].function] = $scope.funcArray[on].func;
                        }
                    } else {
                        eval(evt);
                    }
                }
                $(btn).append(el);
            }
            $(element).append($compile(btn)($scope));

            $scope.modifyData = function ($event, $index) {
                $scope.gridInput = $scope.dataEdit[$scope.index];
                $scope.$emit("getGridInput", $scope.gridInput);
            };
        }
    }
}
]);

pageApp.directive('evtDirective', ['$compile', 'schemaBySpaceByNameByfilter', 'schemaBySpaceByNameById', function ($compile, schemaBySpaceByNameByfilter, schemaBySpaceByNameById) {
    return {
        restrict: 'E',
        scope: {
            data: "= rowData",
            eventDt: "= eventData",
            eventEnume: "=eventEnume",
            index: "=index",
            schema: "=schema",
            space: "=space",
            schemaName: "=schemaName",
            filtercolumn: "=columnFilter",
            funcArray: "=funcArray",
            dto: "=dto"
        },

        link: function ($scope, element, attr) {
            var evt = "";
            var aElement = document.createElement("a");
            $(aElement).attr("href", "javascript:void(0)");
            $(aElement).text($scope.data.description);
            for (var num = 0; num < $scope.eventDt.length; num++) {
                var funStr = "$scope.#name=#body;";
                evt += funStr.replace("#name", $scope.eventDt[num].function).replace('#body', JXG.Util.UTF8.decode(JXG.Util.Base64.decode($scope.eventDt[num].script)));
                var enums = $scope.eventDt[num].event;

                for (var e = 0; e < $scope.eventEnume.length; e++) {
                    if (enums == $scope.eventEnume[e].key) {
                        $(aElement).attr($scope.eventEnume[e].description, $scope.eventDt[num].function + "(data)");

                        if ($scope.funcArray.length > 0) {
                            for (var on = 0; on < $scope.funcArray.length; on++) {
                                if ($scope.eventDt[num].id == $scope.funcArray[on].id)
                                    $scope[$scope.eventDt[num].function] = $scope.funcArray[on].func;
                            }
                        }
                    }
                }
            }
            $(element).append($compile(aElement)($scope));

            if ($scope.funcArray.length == 0) {
                eval(evt);
            }
        }
    }
}]);


pageApp.directive('myCollapse', function () {
    return {
        scope: {
            collapse: '&',
            collapseName: '&'
        },
        replace: false,
        template: "<div ng-click='collapse()'>{{collapseName}}</div>",
        link: function ($scope, element, attributes) {
            $scope.collapse = function () {
                $("#" + attributes.value).collapse('toggle');
            };
            $scope.collapseName = attributes.name;
            //   element.attr('ng-click' ,'collapse()');
            //    $compile(element.contents())($scope);
        }
    }
});
pageApp.directive('parentstree', function () {
    var zNodes = [];

    return {
        scope: {
            event: "@event",
            filtercolumn: "=parentid",
            space: "=space",
            schemaName: "=schemaName",
            sendEvent: "=sendEvent"
        },

        restrict: 'A',
        link: function ($scope, element, attrs) {
            console.log("onclicktest", '=onClickTest');
            console.log("showIcon", '=showIcon');
            console.log("element", element);
            console.log("attrs", attrs);

            var setting = {
                view: {
                    showIcon: '=showIcon'
                },
                data: {
                    key: {
                        title: ""
                    }

                },
                callback: {

                    onClick: function (event, treeId, treeNode, clickFlag) {
                        if (treeNode.isParent == true) {
                            $scope.$emit($scope.sendEvent, treeNode.data[$scope.filtercolumn]);
                        } else {
                            $scope.$emit("schemaChange", treeNode);
                        }
                    },

                    //处理展开事件
                    onExpand: function (event, treeId, treeNode) {
                        //get schema in the store

                        var URL = "/" + $scope.space + "/DSE/" + $scope.schemaName + "?$filter=" + $scope.filtercolumn + " eq " + treeNode.data.id;
                        //    treeNode.name= "sdgsadg";
                        $.get(
                            URL,

                            function (data) {
                                //  for(var j =0 ;j<zNodes.length && data.length>0;j++){
                                var znodes = [];
                                znodes.push(zNodes[0]);
                                while (znodes.length > 0 && data.length > 0) {
                                    var currentNode = znodes.pop();
                                    if (currentNode.data.id == data[0][$scope.filtercolumn]) {//&& currentNode.open == false){
                                        currentNode.open = true;
                                        if (currentNode.children.length == 0) {
                                            for (var i = 0; i < data.length; i++) {
                                                currentNode.children.push({
                                                    id: i,
                                                    name: data[i].description,
                                                    children: [],
                                                    isParent: true,
                                                    data: data[i]
                                                });
                                            }
                                        }
                                    }
                                    for (var i in currentNode.children) {
                                        znodes.push(currentNode.children[i]);
                                    }
                                }

                                $.fn.zTree.init(element, setting, zNodes);
                            }
                        );

                        $scope.$emit($scope.sendEvent, treeNode.data.id);
                    },//onExpand

                    //处理折叠事件，未完成
                    onCollapse: function (event, treeid, treeNode) {

                        var storeNodes = zNodes[0].children;
                        for (var i = 0; i < storeNodes.length; i++) {
                            if (storeNodes[i] == treeNode.name) {
                                storeNodes[i].open = false;
                            }
                        }
                        //     $scope.$emit("treeChange", treeNode.data[$scope.filtercolumn]);
                    }//onExpand
                }
            };

            function showIconForTree(treeId, treeNode) {
                return !treeNode.isParent;
            };
            zNodes = [];


            //   console.log("spacename "+$scope.space.realname);
            //  if($scope.space.realname!="" && typeof($scope.space.realname ) != "undefined")
            {
                console.log("here")
                var URL = "/" + $scope.space + "/DSE/" + $scope.schemaName + "?$filter=" + $scope.filtercolumn + " eq \'0\'";
                //      zNodes.push({name:treeNode.description,isParent:true,open:true,children:[]});
                $.get(
                    URL,
                    function (data) {
                        for (var i = 0; i < data.length; i++) {
                            zNodes.push({
                                name: data[i].description,
                                isParent: true,
                                children: [],
                                open: false,
                                data: data[i]

                            });
                        }
                        ;
                        $.fn.zTree.init(element, setting, zNodes);
                    });
            }
        }
    };
});


pageApp.controller('gridDirectCtrl', ['$scope', 'schemaBySpaceByNameById', 'schemaBySpaceByNameByfilter', '$q', 'instance', function ($scope, schemaBySpaceByNameById, schemaBySpaceByNameByfilter, $q, instance) {
    $scope.formId = null;
    $scope.table = {id: ""};


    $scope.dataEdit = null;
    $scope.eventEnume = [{key: "1", description: "ng-click"},
        {key: "2", description: "ng-show"}, {key: "3", description: "ng-disabled"},
        {key: "4", description: "ng-submit"}, {key: "5", description: "ng-bind"}];
    $scope.space = "CMS";
    $scope.formId = 33000;
    $scope.schemaName = "";
    $scope.gridInput = {};
    $scope.cform = [53000, 58000];
    $scope.filtercolumn = " parentId"
    $scope.filter = "";
    $scope.funcArray = [];
    var sortData = "sortData";

    var promises = [];

    $scope.$on('formId', function (e, data) {
        $scope.formId = data;
        $scope.initData();
    });
    $scope.$on('filter', function (e, data) {
        $scope.filter = data;
        for (var key in e.targetScope["schema"]) {
            if (e.targetScope["schema"][key].id == data) {
                instance.extentPropertySchema = e.targetScope["schema"][key].extentPropertySchema;
            }
        }

        $scope.gridInput = {parentId: data};
        $scope.$broadcast("getgridparam");

        $scope.initData();
    });
    $scope.$on('schemaName', function (e, data) {
        $scope.schemaName = data;
        $scope.initData();
    });
    $scope.$on('refresh', function (e, data) {
        $scope.dataEdit.push(data);
    });
    //数据初始化
    $scope.initData = function () {
        if ($scope.formId == "" || $scope.schemaName == "") return;
        promises = [];
        promises.push(schemaBySpaceByNameByfilter.get({
            SPACE: $scope.space,
            NAME: $scope.schemaName,
            COLUMN: $scope.filtercolumn,
            FILTER: $scope.filter
        }).$promise);
        if ($scope.formId != $scope.table.id) {
            promises.push(schemaBySpaceByNameById.get({SPACE: 'MDE', NAME: 'gridList', ID: $scope.formId}).$promise);
        }

        if ($scope.eventEnume == null) {
            promises.push(schemaBySpaceByNameById.get({
                SPACE: 'MDE',
                NAME: 'PropertyReference',
                ID: 'MDE.events.event'
            }).$promise);
        }
        $q.all(promises).then(function (datas) {

            if (datas[0] != undefined) {
                $scope.dataEdit = datas[0];
            }
            if (datas[1] != undefined) {
                datas[1].data.sort(function (a, b) {
                    return a.index - b.index;
                });
                $scope.table = datas[1];
            }
            if (datas[2] != undefined) {
                $scope.eventEnume = datas[2].propertyMap;

            }


            for (var i = 0; i < $scope.table.data.length; i++) {
                //添加未有的属性
                for (var n = 0; n < $scope.dataEdit.length; n++) {
                    if (typeof ($scope.dataEdit[n][$scope.table.data[i].value]) == "undefined") {
                        $scope.dataEdit[n][$scope.table.data[i].value] = "";
                    }
                }
            }

        }, function () {
            console.log("some $resource has not request success");
        });
    }
    $scope.initData();


    //排序固定函数
    $scope.sortData = function (value) {
        $scope.dataEdit.sort(function (a, b) {
            if (sortData == value) {
                return a[value] > b[value]
            }
            return a[value] < b[value];
        });
        if (sortData == value) {
            sortData = "sortData";
        } else {
            sortData = value;
        }
    }


    $scope.add = function () {
        $scope.gridInput = {
            parentId: $scope.filter,
            extentPropertySchema: []
        }
        //    $scope.$broadcast("getgridparam");
        $scope.$broadcast("busData" + $scope.cform[0], $scope.gridInput);
        $("#form_" + $scope.cform[0]).modal();
    }
    $scope.$on("getGridInput", function (e, data) {
        $scope.gridInput = data.data;
        $scope.$broadcast("busData" + $scope.cform[data.index], $scope.gridInput);
        $("#form_" + $scope.cform[data.index]).modal();
    })


}]);

pageApp.directive('newForm', ['$http', '$compile', function ($http, $compile) {
    var linker = function (scope, element) {
        console.log(scope.dto);
        if (scope.params != undefined) {
            $http({
                url: "/MDE/DSE/CForm(\'" + scope.params + "\')",
                method: 'GET'
            }).success(function (data) {
                var busData = scope.busdata;
                scope.newForm = data;
                var fields = scope.newForm.form_fields;
                if (busData) {
                    for (f in fields) {
                        scope.newForm.form_fields[f].field_value = busData[scope.newForm.form_fields[f].field_field];
                    }
                }
            })
        } else {
            scope.newForm = scope.formData;
        }

    };
    return {
        link: linker,
        templateUrl: '/WEB/Portal/metadata-web/UIDesigner/newForm.html',
        restrict: 'E',
        scope: {
            params: '=',
            busdata: "=busdata",
            dto:"=dto"
        }
        // scope: false
    };
}]);

pageApp.controller('ListCtrl', ['$scope', '$http', 'schemaBySpaceByNameById', 'schemaBySpaceByNameByfilter', '$q', 'instance', function ($scope, http, schemaBySpaceByNameById, schemaBySpaceByNameByfilter, $q, instance) {
    $scope.gridList = "";
    $scope.schema = [];
    $scope.schemaInput = {};
    $scope.showData = []
    $scope.docName = null;
    $scope.listId = null;
    $scope.extentProperty = {};
    $scope.isHide = false;

    $scope.funcArray = [];
    $scope.eventEnume = [{key: "1", description: "ng-click"},
        {key: "2", description: "ng-show"}, {key: "3", description: "ng-disabled"},
        {key: "4", description: "ng-submit"}, {key: "5", description: "ng-bind"}];

    $scope.tempData = new Object();
    $scope.tempData.isAdd = true;

    $scope.isShow = [];
    $scope.space = "CMS";
    $scope.schemaName = "Content";
    $scope.listId = 27000;
    $scope.cform = [];
    $scope.filtercolumn = "parentId"
    $scope.filterContents = "";

    var promise = [];

    $scope.$on("filterContents", function (e, d) {
        $scope.extentProperty = [];
        for (var key in  instance.extentPropertySchema) {
            $scope.extentProperty.push({
                name: instance.extentPropertySchema[key].name,
                value: "",
                verificationRegEx: {}
            });
        }
        $scope.filterContents = d;
        $scope.loadList();

    })

    /* schemaBySpaceByNameByfilter.get({SPACE: 'MDE', NAME: 'PropertyReference', ID: 'MDE.events.event'}, function (data) {
     $scope.eventEnume = data.propertyMap
     });  */


    $scope.loadList = function () {
        if ($scope.schemaName == null || $scope.listId == null)
            return;
        promise = [];
        promise.push(schemaBySpaceByNameByfilter.get({
            SPACE: $scope.space,
            NAME: $scope.schemaName,
            COLUMN: $scope.filtercolumn,
            FILTER: $scope.filterContents
        }).$promise);
        if ($scope.listId != $scope.gridList.id) {
            promise.push(schemaBySpaceByNameById.get({SPACE: 'MDE', NAME: 'gridList', ID: $scope.listId}).$promise);
        }
        $q.all(promise).then(function (datas) {
            if (datas[0] != undefined) {
                $scope.schema = datas[0];
                $scope.showData = [];
            }
            if (datas[1] != undefined) {
                $scope.gridList = datas[1];
            }
            $scope.gridList.data.sort(function (a, b) {
                return a.index - b.index;
            });
            for (var i = 0; i < $scope.gridList.data.length; i++) {
                if ($scope.gridList.data[i].isShow) {
                    for (var n = 0; n < $scope.schema.length; n++) {
                        $scope.showData.push({
                            "showTitle": $scope.schema[n][$scope.gridList.data[i].value],
                            events: $scope.gridList.data[i].events,
                            data: $scope.schema[n]
                        });

                    }
                }
            }
        })

    }


    //   $scope.loadList();


    $scope.newAdd = function () {
        $scope.schemaInput = {};
        $scope.schemaInput.extentProperty = $scope.extentProperty;
        $scope.schemaInput.parentId = $scope.filterContents;
        $scope.schemaInput.id = 0;
        $scope.schemaInput.type = 1;
        $scope.schemaInput.beginData = "";
        $scope.schemaInput.endData = "";
        $scope.title = "";
        $scope.content = "";
        $scope.author = "";
        $scope.createData = "";


        $scope.$broadcast("getParam", $scope.schemaInput);
        $("#listform").modal();
    };
    $scope.$on("form", function (data) {
        $scope.schemaInput = data.targetScope.schemaInput;
        $scope.$broadcast("getParam", $scope.schemaInput);
        $("#listform").modal();
    })

}]);


pageApp.directive('newField', function ($http, $compile) {
    var type = "";
    var getTemplateUrl = function (field) {
        type = field.field_type == "" ? "text" : field.field_type;
        var templateUrl = '';

        switch (type) {
            case 'text':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/text.html';
                break;
            case 'email':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/email.html';
                break;
            case 'textarea':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/textarea.html';
                break;
            case 'checkbox':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/checkbox.html';
                break;
            case 'date':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/date.html';
                break;
            case 'select':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/dropdown.html';
                break;
            case 'hidden':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/hidden.html';
                break;
            case 'password':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/password.html';
                break;
            case 'radio':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/radio.html';
                break;
            case 'number':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/number.html';
                break;
            case 'tel':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/tel.html';
                break;
            case 'file':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/file.html';
                break;
            case 'datetime':
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/datetime.html';
                break;
            default :
                templateUrl = '/WEB/Portal/metadata-web/formDesigner/views/newfield/text.html';
                break;
        }
        return templateUrl;
    }

    var linker = function (scope, element) {
        // GET template content from path
        var templateUrl = getTemplateUrl(scope.field);

        $http({
            method: 'GET',
            url: templateUrl,
            cache: true
        }).success(function (data) {
            var events = scope.field.events;
            var id = scope.field.field_value;
            var ctrl = $(data);
            ctrl.find("label").addClass("control-label");
            if (type == "select") {
                ctrl.find("select[class*=form]").attr("id", id);
            }
            else if (type == "textarea") {
                ctrl.find("textarea[class*=form]").attr("id", id);
            }
            else {
                ctrl.find("input[type*=" + type + "]").attr("id", id);
            }

            // add events
            var evt = "";
            if (events.length > 0) {
                for (var e = 0; e < events.length; e++) {
                    var event = "";
                    if (events[e].event == 1)
                        event = "ng-show";
                    else if (events[e].event == 2)
                        event = "ng-disabled";
                    else if (events[e].event == 3)
                        event = "ng-click";
                    else if (events[e].event == 4)
                        event = "ng-focus";
                    else if (events[e].event == 5)
                        event = "ng-change";
                    else if (events[e].event == 6)
                        event = "ng-checked";
                    else if (events[e].event == 7)
                        event = "ng-keyup";
                    else if (events[e].event == 8)
                        event = "ng-if";
                    // ctrl.find("input[type*="+type+"]").attr(event, events[e].function);

                    if (type == "select") {
                        ctrl.find("select[class*=form]").attr(event, events[e].function);
                    }
                    else if (type == "textarea") {
                        ctrl.find("textarea[class*=form]").attr(event, events[e].function);
                    }
                    else {
                        ctrl.find("input[type*=" + type + "]").attr(event, events[e].function);
                    }

                    //-----事件------
                    var funStr = "scope.#funName=#funBody;";
                    var funName = events[e].function.substr(0, events[e].function.indexOf('('));
                    evt += funStr.replace("#funName", funName).replace('#funBody', events[e].script);
                    eval(evt);
                }
            }

            element.append(ctrl);
            // element.html(data);
            $compile(element.contents())(scope);
        });
    }

    return {
        template: '',
        restrict: 'E',
        scope: {
            data: '=busdata',
            field: '=',
            dto:"=dto"
        },
        link: linker
    };
});

// 20150708 mark<daijx@techdop.cffex.net>
pageApp.directive('newBtns', function ($http, $compile,schemaBySpaceByNameById) {
    var type = "";
    var btnClass = "";
    var getBtn = function (btns) {
        type = btns == "" ? "提交" : btns.name;
        return type;
    }
    var getClass = function (btns) {
        btnClass = btns.class;
        return btnClass;
    }

    var linker = function (scope, element) {
        var events = scope.btns.events;
        var btn = '<button class="btn center" type="button" ';
        // add events
        var evt = "";
        for (var e = 0; e < events.length; e++) {
            var event = "";
            if (events[e].event == 1)
                event = "ng-show";
            else if (events[e].event == 2)
                event = "ng-disabled";
            else if (events[e].event == 3)
                event = "ng-click";
            else if (events[e].event == 4)
                event = "ng-focus";
            else if (events[e].event == 5)
                event = "ng-change";
            else if (events[e].event == 6)
                event = "ng-checked";
            else if (events[e].event == 7)
                event = "ng-keyup";
            else if (events[e].event == 8)
                event = "ng-if";
            btn += " " + event + "=" + events[e].function;

            //-----事件------
            var funStr = "scope.#funName=#funBody;";
            var funName = events[e].function.substr(0, events[e].function.indexOf('('));
            evt += funStr.replace("#funName", funName).replace('#funBody', JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script)));
            eval(evt);
        }
        type = getBtn(scope.btns);
        btnClass = getClass(scope.btns);
        btn += "><i class='glyphicon glyphicon-" + btnClass + "'></i> " + type + "</button>";
        element.append($(btn));
        $compile(element.contents())(scope);
    }

    return {
        template: '',
        restrict: 'E',
        scope: {
            busData: "=busdata",
            field: "=field",
            btns: "=btns",
            dto:"=dto"
        },
        link: linker
    }
});