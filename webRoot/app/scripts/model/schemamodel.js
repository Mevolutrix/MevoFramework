var SchemaModel={};
var propertyModel = {
  name : '',
  pType : '',
  complexTypeName: null,
  nullable: false,
  isArray : false,
  verificationRegEx : null,
}


var schemaModel = {
	id :'',
	appSpaceId: '',
	entitySetName: '',
	entityName:'',
	description:'',
	version:'',
	properties: [],	
	entityType: 1,
	status: 1	
};

function createSchemaModel(){
	var r = {};
	r.id ='';
	r.appSpaceId = '';
	r.entitySetName = '';
	r.entityName ='';
	r.description ='';
	r.version ='';
	r.properties = [];	
	r.entityType = 1;
	r.status = 1;
	return r;	
}

var CreateNew=function (model,name){
    if(isClass(this[name]) === "Undefined"){
        this.add(model,name,this);
    }
    return this[name].CreateNew();
}

var add=function(model, name, schemaModel){
    var thisModel ,obj =  isClass(model);
    if(obj==="Object"){
        thisModel={};
    }else if(obj==="Array"){
        thisModel=[];
    }else{
        return obj;
    }

    for(var key in model){
        var copy=model[key];
        var type = isClass(copy);
        if(type ==="Object"){
         /*   if(isClass(schemaModel[name][key]) === "Undefined")
            {
                schemaModel[name][key]={};
                schemaModel[name][key].CreateNew=CreateNew;
                schemaModel[name][key].add=add;
            }
            thisModel[key]=schemaModel[name][key].CreateNew(copy, key);//递归调用
            */
        }else if(type ==="Array"){
         /*   if(isClass(schemaModel[name][key]) === "Undefined")
            {
                this.schemaModel[name][key]=[];
                this.schemaModel[name][key].CreateNew=CreateNew;
                this.schemaModel[name][key].add=add;
            }

            thisModel[key]=schemaModel[name][key].CreateNew(copy, key);*/
        }else{
            thisModel[key]=model[key];
        }
    }
    schemaModel[name]= {
        Model:thisModel,
        CreateNew: function () {
            var tempModel = {};
            for (var key in this.Model) {
                tempModel[key] = this.Model[key];
            }



        }
    }
}
SchemaModel.CreateNew = CreateNew;
SchemaModel.add=add;

function isClass(o){
    if(o===null) return "Null";
    if(o===undefined) return "Undefined";
    return Object.prototype.toString.call(o).slice(8,-1);
}

