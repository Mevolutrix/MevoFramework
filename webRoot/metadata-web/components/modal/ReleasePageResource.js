function ReleasePR(){
   var releasePR={
       "appSpace" :"",
       "path" : "",
       "parameterVal" :{},
       "MediaType" : 1,
       "isDefault" : false,
       "data" :  ""
   }
    return releasePR;
};

var htmlTemplate={
  /* "htmlHead":'<!DOCTYPE html><html ><head lang="en" ><meta charset="UTF-8">' +
    ' <title> New Document </title>' +
    '<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />'+
    '<link href="/WEB/Portal/metadata-web/lib/style/zTreeStyle/zTreeStyle.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/bootstrap.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/store.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/ng-grid.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/angular-motion.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/layout.css" rel="stylesheet">' +
    '<link href="/WEB/Portal/metadata-web/lib/style/jquery-ui.css" rel="stylesheet">' +
    '<script src="/WEB/Portal/metadata-web/lib/lib.js"></script>' +

    '</head><body ng-app="MyApp" ng-controller="storeManage">',  */
    "htmlHead":'<!DOCTYPE html><html ><head lang="en" ><meta charset="UTF-8">' +
' <title> UIDesigner </title>' +
'<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />'+
'<link href="/WEB/Portal/metadata-web/lib/style/zTreeStyle/zTreeStyle.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/bootstrap.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/store.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/ng-grid.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/angular-motion.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/layout.css" rel="stylesheet">' +
'<link href="/WEB/Portal/metadata-web/lib/style/jquery-ui.css" rel="stylesheet">' +
'<script src="/WEB/Portal/metadata-web/lib/CMSlib.js"></script>',

 scriptStart:"<script>pageApp.controller('routeParamsController', ['$scope', '$location', '$timeout', "+
 "function ($scope, $location, $timeout) {$timeout(function () { "+
   " for (var key in $location.$$search) {" +
   " $scope.$broadcast(key, $location.$$search[key])"+
"};}, 5, true);",

    scriptEnd:"}]);</script>",

 pageScript: function(recev, send){

    var sendStr="";
     for(var key in send){
         sendStr +="$scope.$broadcast(\'"+ send[key] +"\', d);\n";
    }
     var str = "$scope.$on(\'" + recev +"\',function(e,d){\n"+ sendStr+ "});";
     return str;
  },

htmlMield :'</head><body ng-app="pageApp" ng-controller="routeParamsController">' ,

    "htmlEnd" : '</body></html>'
}
