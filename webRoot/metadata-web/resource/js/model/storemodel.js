
var storeModel = {
		entitySetName : '',
		appSpaceId : '',
		description: '',
		pKey: '',
		pkeyType:'',
		index: [],
		_storageType: '',
		defaultValues: [],
		autoValue: "",
		minValueOfPKey: "",		
}


function createStoreModel(){
   var r ={};
   r.entitySetName = '';
   r.appSpaceId = '';
   r.description = '';
   r.pKey = '';
   r.pkeyType ='';
   r.index =[];
   r._storageType = '';
   r.defaultValues = [];
   r.autoValue = "";
   r.minValueOfPKey = "";	
   return r;
}
  


var indexModel = {
	path: '',
	direct: true,
	markType: 0,
	indexDataType: 2,
	expression: null
}


var schemaModel = {
	id :'',
	appSpaceId: '',
	entitySetName: '',
	entityName:'',
	description:'',
	version:'',
	entityType:'',
	status:'',
	properties: []	
};

var setModel = {
	setName:'',
	appSpaceId:'',
	baseSchemaName:'',
	entitySchemaList:[]
}

function createSetModel(){
	var r ={};
	r.setName = '';
	r.appSpaceId = '';
	r.baseSchemaName = '';
	r.entitySchemaList =  [];
	return r;

}

var listElementModel ={
	id:'',
	schemaName:'',
	isBaseSchema:'false',
}

var spaceSchemaListModel = [];