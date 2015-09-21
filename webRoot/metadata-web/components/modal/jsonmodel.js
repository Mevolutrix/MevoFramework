var dataTypeModel =[  
      {id:1,name:'FloatingPoint'},
      {id:2,name:'UTF8String'},
      {id:3,name:'EmbeddedDocument'},
      {id:5,name:'BinaryData'},
      {id:7,name:'ObjectId'},
      {id:8,name:'Boolean'},
      {id:9,name:'UTCDatetime'},
      {id:11,name:'RegularExpression'},
      {id:12,name:'DBPointer'},
      {id:14,name:'JavaScriptCode'},
      {id:15,name:'Symbol'},
      {id:16,name:'Int32'},
      {id:17,name:'Timestamp'},
      {id:18,name:'Int64'},
      {id:19,name:'Decimal'},
      {id:20,name:'NullElement'},
      {id:21,name:'Int8'},
      {id:22,name:'Int16'},
      {id:23,name:'Single'}
    ];
var ValidatorModel = [
    {
        "name": "无校验规则",
        "invokeType": 0,
        "pattern": "",
        "errMsg": "无校验规则"
    },{
        "name": "字符串验证规则",
        "invokeType": 0,
        "pattern": "/.+/",
        "errMsg": "请输入正确字符"
    },
    {
        "name": "数字验证规则",
        "invokeType": 0,
        "pattern": "/^[-+]?[0-9]{1,}[\\.]?[0-9]*$/",
        "errMsg": "请输入正确数字"
    },
    {
        "name": "手机号验证规则",
        "invokeType": 0,
        "pattern": "/^1[0-9]{10}$/",
        "errMsg": "请输入正确手机号"
    },
    {
        "name": "电子邮箱验证规则",
        "invokeType": 1,
        "pattern": "/^(([^<>()[\\]\\.,;:\\s@\"]+(\\.[^<>()[\\]\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$/",
        "errMsg": "请输入正确的电子邮件"
    }
];
var storeTypeModel=[
    {id:0,name:'仅缓存'},
    {id:1,name:'分库分表存储'},
    {id:2,name:'集中存储'},
    {id:3,name:'共享（主数据）'}
]

var markTypeModel=[
	{id:0,name:'缺省'},
	{id:1,name:'哈希'},
	{id:2,name:'异步'}
]

spaceModel =[
  {realname:'Content.XEDU', name: 'XEDU'},
  {realname:'Content.MgmtSystem',name:'CMS'},
  {realname:'System.Metadata',name:'MDE'},
  {realname:'System.Configuration',name:'CFG'}
]
booleanModel=[
    {key:"是",value:true},
    {key:"否",value:false}];

httpMethodsModel=[
    {name:'get',method:'get'},
    {name:'put',method:'put'},
    {name:'post',method:'post'},
    {name:'delete',method:'delete'},
];
serviceTypeModel=[
    {name:'DSE'},
    {name:'SVC'},
    {name:'SME'}
];
paramForSMEModel=[
    {name:'deploySets'},
    {name:'createSet'}
]
paramForServiceModel=[
    {name:'filter'},
    {name:'select'}
]
