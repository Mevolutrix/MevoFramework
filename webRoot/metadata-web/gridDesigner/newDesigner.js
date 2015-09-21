app.controller('NewCreateCtrl', ['$scope', '$compile', 'NewCreateService', function($scope, $compile, NewCreateService) {
	/***********************/
	/* Grid Designer start */
	/***********************/
	$scope.schema = {};
	$scope.gridList = {};
	$scope.cForms = {};
	$scope.modifyForms = "";
	$scope.eventEnume={};
	$scope.showSave = true;
	// fill model forms
	NewCreateService.forms().success(function(data){
		$scope.schema.forms = data
	})
	// fill ng-model grid
	NewCreateService.grids().success(function(data){
		$scope.gridList.grids = data
	})
	// fill ng-model newGrid.formId
	NewCreateService.cForms().success(function(data){
		$scope.cForms.forms = data
	})
	// get all events
	NewCreateService.getEventEnum().success(
		function(data){
			$scope.eventEnume=data.propertyMap;
		}
	);
	
	$scope.newGrid = {
		"id": 0, //表id，生成
		"name": "", //名称，填写
		"isShowOperate": false, //是否显示操作列
		"isDragRow": false, //是否允许行拖动
		"formId": 0, //返回的id
		"filter": [], //过滤器，暂时为空
		"data_pretreatment": "",		// 数据预处理
		"load_pretreatment": "",		// 数据预加载
		"headerBtn": [],		// 
		"operateBtn": [],
		"grid_style": "",	// grid中table样式
		"data": [],
		"description": "",
		"showType": "Grid",
		"path": "System.Metadata",
		"appSpace": "MDE"
	}

	// add btn function
	var btnId = 1;
	$scope.addBtn = function(param) {
		var btn = {
			"id": btnId, //id
			"name": "", //按钮名称
			"events": [],
			"class": ""
		}
		$scope.newGrid[param].push(btn);
		btnId ++;
	}
	$scope.delBtn = function(btnId) {
		var headBtns = $scope.newGrid.headerBtn;
		var btns = $scope.newGrid.operateBtn;
		for (i in headBtns) {
			if (headBtns[i].id == btnId) {
				$scope.newGrid.headerBtn.splice(i, 1);
				break;
			}
		}
		for (i in btns) {
			if (btns[i].id == btnId) {
				$scope.newGrid.operateBtn.splice(i, 1);
				break;
			}
		}
	}
	// add btn event
	var eventId = 1;
	$scope.addEvent = function(btnId) {
		var event = {
			"id": eventId,
			"event": "", //事件 ng-click ng-show等
			"function": "", //函数名称 delete  在组装时将整行数据传入
			"script": "" // function内容
		}
		var headBtns = $scope.newGrid.headerBtn;
		var btns = $scope.newGrid.operateBtn;
		for (h in headBtns) {
			if (headBtns[h].id == btnId) {
				$scope.newGrid.headerBtn[h].events.push(event);
				break;
			}
		}
		for (i in btns) {
			if (btns[i].id == btnId) {
				$scope.newGrid.operateBtn[i].events.push(event);
				break;
			}
		}
		eventId ++;
	}
	// delete btn event
	$scope.delEvent = function(btnId, eventId) {
		var headBtns = $scope.newGrid.headerBtn;
		var btns = $scope.newGrid.operateBtn;
		for (h in headBtns) {
			if (headBtns[h].id == btnId) {
				var events = headBtns[h].events;
				for (e in events) {
					if (events[e].id == eventId) {
						$scope.newGrid.headerBtn[h].events.splice(e, 1)
					}
				}
			}
		}
		for (b in btns) {
			if (btns[b].id == btnId) {
				var events = btns[b].events;
				for (e in events) {
					if (events[e].id == eventId) {
						$scope.newGrid.operateBtn[b].events.splice(e, 1)
					}
				}
			}
		}
	}
	var colId = 1;
	$scope.selectGrid = function(grid){
		if(grid==""){
			$scope.showUpdate = false;
			return;
		}
		NewCreateService.grid(grid).success(function(data){		
			if(data.data_pretreatment)
				data.data_pretreatment = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(data.data_pretreatment));
			else
				data.data_pretreatment = "";
			if(data.load_pretreatment)
				data.load_pretreatment = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(data.load_pretreatment));
			else
				data.load_pretreatment = "";
			if(data.headerBtn){
				for(dh in data.headerBtn){
					var events = data.headerBtn[dh].events;
					for(e in events){
						data.headerBtn[dh].events[e].script = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script));
					}
				}
			}else{
				data.headerBtn=[];
			}
			if(data.data){
				for(d in data.data){
					var events = data.data[d].events;
					for(e in events){
						data.data[d].events[e].script = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script));
					}
					colId = Number(d)+1;
				}
			}else{
				data.data=[];
			}
			if(data.operateBtn){
				for(o in data.operateBtn){
					var events = data.operateBtn[o].events;
					for(e in events){
						data.operateBtn[o].events[e].script = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script));
					}
				}
			}else{
				data.operateBtn=[];
			}
				
			$scope.newGrid = data;
			$scope.showUpdate = true;
			$scope.showSave = false;
		})
	}


	$scope.selectForm = function(form){
		if(form==""){
			$scope.showSave = false;
			return;
		}
		NewCreateService.form(form).success(function(data){
			$scope.newGrid.data = [];
			for(d in data.properties){
				var column = {
					"id": d, //表头id
					"name": data.properties[d].description, //表头显示名称
					"value": data.properties[d].name, //被绑定元数据 字段名称
					"isShow": false, //列是否显示
					"isSort": false, //是否按列排序
					"index": Number(d)+1, // 排列在第几列，非负数，手填
					"events": [],
					"isCustom": false
				}
				$scope.newGrid.data.push(column);
				colId = Number(d)+1;
			}
			$scope.showUpdate = false;
			$scope.showSave = true;
		})
	}

	// data
	$scope.addColumn = function() {
		var column = {
			"id": colId, //表头id
			"name": "", //表头显示名称
			"value": "", //被绑定元数据 字段名称
			"isShow": false, //列是否显示
			"isSort": false, //是否按列排序
			"index": "", // 排列在第几列，非负数，手填
			"events": [],
			"isCustom": false
		}
		$scope.newGrid.data.push(column);
		colId ++;
	}
	$scope.delColumn = function(id) {
		var datas = $scope.newGrid.data;
		for (i in datas) {
			if (datas[i].id == id) {
				$scope.newGrid.data.splice(i, 1);
			}
		}
	}

	// 先预设dataEventId为1，以后将读取数据库中的dataEventId，将最大设为初始值
	var dataEventId = 1;
	$scope.addDataEvent = function(id) {
		var event = {
			"id": dataEventId,
			"event": "",
			"function": "",
			"script": ""
		}
		var data = $scope.newGrid.data;
		for (i in data) {
			if (data[i].id == id) {
				$scope.newGrid.data[i].events.push(event);
				break;
			}
		}
		dataEventId ++;
	}
	$scope.delDataEvent = function(columnId, eventId) {
		var data = $scope.newGrid.data;
		for (d in data) {
			if (data[d].id == columnId) {
				var events = data[d].events;
				for (e in events) {
					if (events[e].id == eventId) {
						$scope.newGrid.data[d].events.splice(e, 1)
					}
				}
			}
		}
	}
	// save json
	$scope.saveNewJson = function() {
		var toSave = {};
		angular.copy($scope.newGrid, toSave);
		if(toSave.data_pretreatment)
			toSave.data_pretreatment = JXG.Util.Base64.encode(toSave.data_pretreatment);
		if(toSave.load_pretreatment)
			toSave.load_pretreatment = JXG.Util.Base64.encode(toSave.load_pretreatment);
		var hbtns = toSave.headerBtn;
		for(hb in hbtns){
			var hbtn = hbtns[hb];
			for (e in hbtn.events) {
				// change all event to int
				toSave.headerBtn[hb].events[e].event = Number(hbtn.events[e].event);
				toSave.headerBtn[hb].events[e].script = JXG.Util.Base64.encode(hbtn.events[e].script);
			}
		}
		var btns = toSave.operateBtn;
		for (b in btns) {
			var btn = btns[b];
			for (e in btn.events) {
				toSave.operateBtn[b].events[e].event = Number(btn.events[e].event);
				toSave.operateBtn[b].events[e].script = JXG.Util.Base64.encode(btn.events[e].script);
			}
		}
		var datas = toSave.data;
		for (d in datas) {
			toSave.data[d].id = Number(toSave.data[d].id);
			var data = datas[d];
			for (e in data.events) {
				toSave.data[d].events[e].event = Number(data.events[e].event);
				toSave.data[d].events[e].script = JXG.Util.Base64.encode(data.events[e].script);
			}
		}
		toSave.formId = Number(toSave.formId);
		// console.log(toSave)
		// return;
		NewCreateService.saveGrid(toSave).success(function(data) {
			if (data.id != 0 && data.id != null && data.id != undefined) {
				alert("保存成功！");
				$scope.grid="";
				$scope.formId="";
				$scope.newGrid = {
					"id": 0, //表id，生成
					"name": "", //名称，填写
					"isShowOperate": false, //是否显示操作列
					"isDragRow": false, //是否允许行拖动
					"formId": 0, //返回的id
					"filter": [], //过滤器，暂时为空
					"data_pretreatment": "",		// 数据预处理
					"load_pretreatment": "",		// 数据预加载
					"headerBtn": [],		// 
					"operateBtn": [],
					"grid_style": "",	// grid中table样式
					"data": [],
					"description": "",
					"showType": "Grid",
					"path": "System.Metadata",
					"appSpace": "MDE"
				}
			}
		});
	}
	$scope.newReset = function() {
        $scope.newGrid = {
            "id": 0, //表id，生成
            "name": "", //名称，填写
            "isShowOperate": false, //是否显示操作列
            "isDragRow": false, //是否允许行拖动
            "formId": 0, //返回的id
            "filter": [], //过滤器，暂时为空
            "operateBtn": [],
            "data": [],
            "description": "",
            "showType": "Grid",
            "path": "System.Metadata",
            "appSpace": "MDE"
        }
    }
   	$scope.updateNewJson = function(){
   		var toUpdate = {};
		angular.copy($scope.newGrid, toUpdate);
		if(toUpdate.data_pretreatment)
   			toUpdate.data_pretreatment = JXG.Util.Base64.encode(toUpdate.data_pretreatment);
		if(toUpdate.load_pretreatment)
			toUpdate.load_pretreatment = JXG.Util.Base64.encode(toUpdate.load_pretreatment);
		var hbtns = toUpdate.headerBtn;
		for(hb in hbtns){
			var hbtn = hbtns[hb];
			for (e in hbtn.events) {
				toUpdate.headerBtn[hb].events[e].event = Number(hbtn.events[e].event);
				toUpdate.headerBtn[hb].events[e].script = JXG.Util.Base64.encode(hbtn.events[e].script);
			}
		}
   		toUpdate.formId = Number(toUpdate.formId);
   		var btns = toUpdate.operateBtn;
		for (b in btns) {
			var btn = btns[b];
			for (e in btn.events) {
				toUpdate.operateBtn[b].events[e].event = Number(btn.events[e].event);
				toUpdate.operateBtn[b].events[e].script = JXG.Util.Base64.encode(btn.events[e].script);
			}
		}
		var datas = toUpdate.data;
		for (d in datas) {
			toUpdate.data[d].id = Number(toUpdate.data[d].id);
			var data = datas[d];
			for (e in data.events) {
				toUpdate.data[d].events[e].event = Number(data.events[e].event);
				toUpdate.data[d].events[e].script = JXG.Util.Base64.encode(data.events[e].script);
			}
		}
		// console.log(toSave)
		// return;
		NewCreateService.updateGrid(toUpdate).success(function(data) {
			if (data.id != 0 && data.id != null && data.id != undefined) {
				alert("保存成功！");
				$scope.grid="";
				$scope.formId="";
				$scope.newGrid = {
					"id": 0, //表id，生成
					"name": "", //名称，填写
					"isShowOperate": false, //是否显示操作列
					"isDragRow": false, //是否允许行拖动
					"formId": 0, //返回的id
					"filter": [], //过滤器，暂时为空
					"data_pretreatment": "",		// 数据预处理
					"load_pretreatment": "",		// 数据预加载
					"headerBtn": [],		// 
					"operateBtn": [],
					"grid_style": "",	// grid中table样式
					"data": [],
					"description": "",
					"showType": "Grid",
					"path": "System.Metadata",
					"appSpace": "MDE"
				}
			}
		});
   	}

    /***********************************/
    /*  Grid Designer end, form start  */
    /***********************************/
	$scope.newForm = {
		"id": 1,
		"name": "",
		"form_fields": [],
		"operateBtn": [],
		"path": "System.Metadata",
		"appSpace": "MDE"
	};
	var formFieldId = 1;
	$scope.selectNewSchema = function(forms){
		$scope.newForm.form_fields=[];
		formFieldId = 1;
		NewCreateService.form(forms).success(function(data){
			var pros = data.properties;
			for(p in pros){
				var formColumn = {
					"field_id": formFieldId,
					"field_title": pros[p].description,
					"field_desc": "",
					"field_type": "", // int
					"field_field": pros[p].name, // 对应数据库字段
					"field_value": "",
					"field_required": false,
					"field_disabled": true,
					"field_options": [],
					"field_validators": [],
					"events": []
				}
				$scope.newForm.form_fields.push(formColumn);
				formFieldId ++;
			}
		})
	}
	$scope.selectNewForm = function(modifyForms){
		$scope.newForm.form_fields=[];
		NewCreateService.getForm(modifyForms).success(function(data){
			for(d in data.form_fields){
				var events = data.form_fields[d].events;
				for(e in events){
					data.form_fields[d].events[e].event = Number(data.form_fields[d].events[e].event);
					data.form_fields[d].events[e].script = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script));
				}
			}
			for(o in data.operateBtn){
				var events = data.operateBtn[o].events;
				for(e in events){
					data.operateBtn[o].events[e].event = Number(data.operateBtn[o].events[e].event);
					data.operateBtn[o].events[e].script = JXG.Util.UTF8.decode(JXG.Util.Base64.decode(events[e].script));
				}
			}
			$scope.newForm = data;
		});
		$scope.showUpdateBtn = true;
	}
	$scope.addFormColumn = function() {
		var formColumn = {
			"field_id": formFieldId,
			"field_title": "",
			"field_desc": "",
			"field_type": "", // int
			"field_field": "", // 对应数据库字段
			"field_value": "",
			"field_required": false,
			"field_disabled": true,
			"field_options": [],
			"field_validators": [],
			"events": []
		}
		$scope.newForm.form_fields.push(formColumn);
		formFieldId ++;
	}
	$scope.delFormColumn = function(id) {
		var cols = $scope.newForm.form_fields;
		for (c in cols) {
			if (cols[c].field_id == id) {
				$scope.newForm.form_fields.splice(c, 1);
			}
		}
	}
	var formEventid = 1;
	$scope.addFormEvent = function(id) {
		var event = {
			"id": formEventid,
			"event": "", // int
			"function": "",
			"script": ""
		}
		var columns = $scope.newForm.form_fields;
		for (c in columns) {
			if (columns[c].field_id == id) {
				$scope.newForm.form_fields[c].events.push(event);
			}
		}
		formEventid ++;
	}
	$scope.delFormEvent = function(columnId, eventId) {
        var columns = $scope.newForm.form_fields;
        for (c in columns) {
            if (columns[c].field_id == columnId) {
                var events = columns[c].events;
                for (e in events) {
                    if (events[e].id == eventId) {
                        $scope.newForm.form_fields[c].events.splice(e, 1);
                    }
                }
            }
        }
    }
    /*******************************************************************/
    /*    这里读库，查询所有的验证，列举出来供勾选
    /*******************************************************************/
    NewCreateService.findValidators().success(function(data){
    	$scope.validators = {
    		"validator": [{
				"name": "无",
				"invokeType": 0,
				"pattern": "",
    			"errMsg": ""
    		}]
    	}
    	for(d in data){
    		$scope.validators.validator.push(data[d]);
    	}
    })
    /******************************************************/
    /* 这里以后读库，查询所有的控件选项      TODO
     /******************************************************/
	$scope.controlSelect = {
        "options": [{
            "name": "文本框",
            "value": "text",
            "errMsg": "errMsg5"
        }, {
            "name": "密码框",
            "value": "password",
            "errMsg": "errMsg5"
        }, {
            "name": "电话",
            "value": "tel",
            "errMsg": "errMsg5"
        }, {
            "name": "数字",
            "value": "number",
            "errMsg": "errMsg5"
        }, {
            "name": "邮箱",
            "value": "email",
            "errMsg": "errMsg5"
        }, {
            "name": "日期",
            "value": "date",
            "errMsg": "errMsg5"
        }, {
            "name": "下拉框",
            "value": "select",
            "errMsg": "errMsg5"
        }, {
            "name": "多选按钮",
            "value": "checkbox",
            "errMsg": "errMsg5"
        }, {
            "name": "单选按钮",
            "value": "radio",
            "errMsg": "errMsg5"
        }, {
            "name": "多行文本框",
            "value": "textarea",
            "errMsg": "errMsg5"
        }, {
            "name": "文件上传",
            "value": "file",
            "errMsg": "errMsg5"
        }, {
            "name": "时间",
            "value": "datetime",
            "errMsg": "errMsg5"
        }, {
            "name": "隐藏域",
            "value": "hidden",
            "errMsg": "errMsg5"
        }]
    }
    // 点击添加验证事件
	$scope.setValidate = function(columnId, name) {
		var columns = $scope.newForm.form_fields;
		var valis = $scope.validators.validator;
		for(c in columns){
			if(columns[c].field_id == columnId){
				$scope.newForm.form_fields[c].field_validators=[];
				$("input[name*=invoke]").removeAttr("checked");
				for(v in valis){
					if(valis[v].name == name){
						$scope.newForm.form_fields[c].field_validators.push(valis[v]);
					}
					if(name == "无"){
						$scope.newForm.form_fields[c].field_validators=[];
					}
				}
			}
		}
	}
	$scope.setValiInvoke = function(columnId, invokeType){
		var columns = $scope.newForm.form_fields;
		for(c in columns){
			if(columns[c].field_id == columnId){
				$scope.newForm.form_fields[c].field_validators[0].invokeType=invokeType;
			}
		}
	}
	$scope.showAddOptions = function(select) {
		if (select == "select" || select == "radio" || select == "checkbox") {
			return true;
		} else {
			return false;
		}
	}

	var option_id = 1;
	$scope.addOption = function(field) {
		var newOption = {
			"option_id": option_id,
			"option_title": "",
			"option_value": ""
		};
		// put new option into field_options array
		var fields = $scope.newForm.form_fields;
		for (f in fields) {
			if (fields[f].field_id == field.field_id) {
				$scope.newForm.form_fields[f].field_options.push(newOption);
			}
		}
		option_id++;
	}
	$scope.delOption = function(field_id, option_id){
		var columns = $scope.newForm.form_fields;
		for(c in columns){
			if(columns[c].field_id == field_id){
				var options = columns[c].field_options;
				for(o in options){
					if(options[o].option_id == option_id){
						$scope.newForm.form_fields[c].field_options.splice(o, 1);
					}
				}
			}
		}
	}
	// form event list
	$scope.eventList = {
		"events": [
		{
			"id":1,
			"attr":"ng-show"
		},{
			"id":2,
			"attr":"ng-disabled"
		},{
			"id":3,
			"attr":"ng-click"
		},{
			"id":4,
			"attr":"ng-focus"
		},{
			"id":5,
			"attr":"ng-change"
		},{
			"id":6,
			"attr":"ng-checked"
		},{
			"id":7,
			"attr":"ng-keyup"
		},{
			"id":8,
			"attr":"ng-if"
		},{
			"id":9,
			"attr":"ng-bind"
		}]
	}
	var formBtnId = 1;
	$scope.addFormBtn = function(){
		var newBtn = {
			"id": formBtnId,
			"name": "",
			"events": [],
			"class": ""
		}
		$scope.newForm.operateBtn.push(newBtn);
		formBtnId ++;
	}
	$scope.delFormBtn = function(btnId){
		var btns = $scope.newForm.operateBtn;
		for(b in btns){
			if(btns[b].id == btnId){
				$scope.newForm.operateBtn.splice(b, 1);
			}
		}
	}
	var formEventId = 1;
	$scope.addFormBtnEvent = function(btnId) {
		var event = {
			"id": formEventId,
			"event": "", //事件 ng-click ng-show等
			"function": "", //函数名称 delete  在组装时将整行数据传入
			"script": "" // function内容
		}
		var btns = $scope.newForm.operateBtn;
		for (i in btns) {
			if (btns[i].id == btnId) {
				$scope.newForm.operateBtn[i].events.push(event);
			}
		}
		formEventId ++;
	}
	$scope.classes = {
		"classes":[
			{
				"id": 1,
				"name": "提交",
				"class": "ok-sign"
			},{
				"id": 2,
				"name": "取消",
				"class": "remove-sign"
			},{
				"id": 3,
				"name": "保存",
				"class": "save-file"
			},{
				"id": 4,
				"name": "下一步",
				"class": "step-forward"
			},{
				"id": 5,
				"name": "上一步",
				"class": "step-backward"
			},{
				"id": 6,
				"name": "新建",
				"class": "plus-sign"
			},{
				"id": 7,
				"name": "编辑",
				"class": "pencil"
			},{
				"id": 8,
				"name": "重置",
				"class": "refresh"
			}
		]
	};
	$scope.delFormBtnEvent = function(btnId, eventId) {
		var btns = $scope.newForm.operateBtn;
		for (b in btns) {
			if (btns[b].id == btnId) {
				var events = btns[b].events;
				for (e in events) {
					if (events[e].id == eventId) {
						$scope.newForm.operateBtn[b].events.splice(e, 1)
					}
				}
			}
		}
	}
	$scope.previewOn = function() {
		$scope.previewForm = {};
        angular.copy($scope.newForm, $scope.previewForm);
        $("#preContain").empty().append($compile("<new-form form=\"previewForm\"></new-form>")($scope));
        $scope.showPreview = true;
        $scope.btnsPreview = true;
    }
    $scope.preStep = function(){
    	$scope.showPreview = false;
    	$scope.btnsPreview = false;
    	$("#preContain").empty();
    }
	$scope.validateForm = function(){
		var flag = true;
		if($scope.newForm.form_fields.length>0){
			flag = false;
		}
		return flag;
	}
	$scope.saveFormJson = function() {
		var fields = $scope.newForm.form_fields;
		for (f in fields) {
			for (e in fields[f].events) {
				$scope.newForm.form_fields[f].events[e].event = Number(fields[f].events[e].event);
				$scope.newForm.form_fields[f].events[e].script = JXG.Util.Base64.encode(fields[f].events[e].script);
			}
		}
		var btns = $scope.newForm.operateBtn;
		for (b in btns) {
			var events = btns[b].events;
			for (e in events) {
				$scope.newForm.operateBtn[b].events[e].event = Number(events[e].event);
				$scope.newForm.operateBtn[b].events[e].script = JXG.Util.Base64.encode(events[e].script);
			}
		}
		NewCreateService.saveForm($scope.newForm).success(function(data) {
			if (data.id != 0 && data.id != null && data.id != undefined) {
				alert("保存成功！");
				$scope.newForm = {
					"id": 1,
					"name": "",
					"form_fields": [],
					"operateBtn": [],
					"path": "System.Metadata",
					"appSpace": "MDE"
				}
			}
			$scope.showPreview = false;
			$scope.btnsPreview = false;
		});
	}
	$scope.newResetForm = function() {
		$scope.grid="";
		$scope.newForm = {
			"id": 1,
			"name": "",
			"form_fields": [],
			"operateBtn": [],
			"path": "System.Metadata",
			"appSpace": "MDE"
		}
	}
	$scope.disabled = function(){
		if($scope.newForm.name=="" || $scope.newForm.form_fields.length==0 || $scope.newForm.operateBtn.length==0){
			return true;
		}
	}

	$scope.updateFormJson = function(){
		var fields = $scope.newForm.form_fields;
		for (f in fields) {
			for (e in fields[f].events) {
				$scope.newForm.form_fields[f].events[e].event = Number(fields[f].events[e].event);
				$scope.newForm.form_fields[f].events[e].script = JXG.Util.Base64.encode(fields[f].events[e].script);
			}
		}
		var btns = $scope.newForm.operateBtn;
		for (b in btns) {
			var events = btns[b].events;
			for (e in events) {
				$scope.newForm.operateBtn[b].events[e].event = Number(events[e].event);
				$scope.newForm.operateBtn[b].events[e].script = JXG.Util.Base64.encode(events[e].script);
			}
		}
		
		NewCreateService.updateForm($scope.newForm).success(function(data) {
			if (data.id != 0 && data.id != null && data.id != undefined) {
				alert("保存成功！");
				$scope.newForm = {
					"id": 1,
					"name": "",
					"form_fields": [],
					"operateBtn": [],
					"path": "System.Metadata",
					"appSpace": "MDE"
				}
			}
			$scope.showPreview = false;
			$scope.btnsPreview = false;
		});
	}
}])