

function Page(){
    var page= {
        "id":0,
        "appSpace": "",
        "class":"",
        "type":0,
        "pageContent":{},
        "pageRefer":0,
        "script":{},
         parameter:{},
        "description": "",
        "path":"",
        "children":[]
    };
    return page;
};

function Script(){
    var script={
        "scriptStart":"",
        "scriptEnd":"",
        "scriptInject":"",
        "scriptParam":[]
    };
    return script;
};

function PageContent(){
    var pageContent={
        "html": "",
        "script":{},
        appSpaceName:""
    };
    return pageContent;
};

function Spaces(){
    var space = [
        {realname:'Content.XEDU', name: 'XEDU'},
        {realname:'Content.MgmtSystem',name:'CMS'},
        {realname:'System.Metadata',name:'MDE'},
        {realname:'System.Configuration',name:'CFG'}
    ];
    return space;
};

function MediaTypes(){
    var mediaTypes = [
        {"name" : "html/htm" , "value" : 0},
        {"name" : "css", "value" : 1},
        {"name" : "js","value" : 2},
        {"name" : "mp4","value" : 3},
        {"name" : "ico","value" : 4},
        {"name" : "jpeg/jpg/jpe","value" : 5},
        {"name" : "mpga/mp2/mp2a/mp3/m2a/m3a","value" : 6},
        {"name" : "xml/xsl","value" : 7},
        {"name" : "xhtml/xht","value" : 8},
        {"name" : "png","value" : 9},
        {"name" : "_","value" : 10}
    ];
    return mediaTypes;
};


var JsTypes = function(){
    var jsTypes =[
        {name:"string"},
        {name:"number"},
        {name:"boolean"},
        {name:"array"},
        {name:"object"},
        {name:"function"},
        {name:"others"}
    ];
    return jsTypes;
};



var LayoutDraggable = function(id, colarry) {

    var start =  '<div id= '+id +'  class="lyrow ui-draggable" >'+
        '<span class="remove" ng-click="removeElm($event)"><i class="icon-remove"></i><div class="btn btn-danger btn-xs">删除</div></span>' +
        '<span class="drag label"><i class="icon-move"></i><div class="btn btn-success btn-xs " >拖动</div></span>' +
        '<div class="preview">' +
        '<input ng-model="inputValue" type="text" ng-keyup="inputCheck($event)"></input>'+
        '</div>'  +
        '<div class="view">' +
        '<div class="row-fluid clearfix" pageid="">';
    var model = ""

        model += '<div  class="'+colarry +' column ui-sortable"></div>'
    var str = start + model + '</div></div></div>'

    return str;



};

var subPageDraggable = function(id){
 /*   return '<div class="box box-element ui-draggable"> <a class="remove label label-important" ng-click="removeElm($event)"><i class="icon-remove icon-white"></i>删除</a> <span class="drag label"><i class="icon-move"></i>拖动</span>' +
        '<span class="configuration"><button type="button" id=' +
        id+'  class="btn btn-mini" ng-click="editePage($event)" role="button">编辑</button>     <span class="btn-group"> <button type="button" id={{id}}  class="btn btn-mini" ng-click="editeId($event)" role="button">PageReferid</button>'+
       ' </span> <span class="btn-group"> <a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">标记 <span class="caret"></span></a>' +
    '<ul class="dropdown-menu"><li class="active"><a href="#" rel="">默认</a></li><li class=""><a href="#" rel="muted">禁用</a></li><li class=""><a  rel="text-warning">警告</a></li><li class=""><a href="#" rel="text-error">错误</a></li><li class=""><a href="#" rel="text-info">提示</a></li><li class=""><a href="#" rel="text-success">成功</a></li></ul>' +
    '</span> </span><div class="preview">table</div><div class="view"><div></div></div></div>';  */

    return '<div><div class="box box-element ui-draggable">'+
              '<a class="remove label label-important" ng-click="removeElm($event)"><i class="icon-remove icon-white"></i><div class="remove btn-danger btn-xs">删除</div></a>'+
              '<span class="drag label"><i class="icon-move"></i><div class="btn btn-success btn-xs " >拖动</div></span>' +
    '<span class="configuration"><button type="button" id='+id +
    ' class="btn btn-primary btn-xs" ng-click="injectParams($event)" role="button">注入参数</button>'+
    '<button type="button" id='+ id + ' class="btn btn-primary btn-xs" ng-click="editeId($event)" role="button">选择页面</button>'+
    '<button type="button"  id=' + id +'  class="btn btn-primary btn-xs" ng-click="editPage($event)" role="button">编辑页面</button>' +
    '</span>' +
    '<div class="preview">gridRefer</div>' +
    '<div class="view">' +
     '<div class="pageRefer">' +
        '</div></div> </div> </div>' ;
}

