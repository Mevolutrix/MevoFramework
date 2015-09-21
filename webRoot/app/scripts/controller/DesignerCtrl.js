/**
 * Created by Ming on 2015/4/7.
 */


dashboardApp.controller('DesignerCtrl', function($scope, FormService) {
    // preview form mode
    $scope.previewMode = false;

    // new form
    $scope.form = {};
    $scope.form.form_id = 1;
    $scope.form.form_name = 'My Form';
    $scope.form.form_fields = [];

    // add new field drop-down:
    $scope.addField = {};
    $scope.addField.types = FormService.fields;
    $scope.addField.new = $scope.addField.types[0].name;
    $scope.addField.lastAddedID = 0;

    // accordion settings
    $scope.accordion = {}
    $scope.accordion.oneAtATime = true;

    //20150425 mark schema
    $scope.fromSchema = {};
    $scope.fromSchema.$schema = 'http://json-schema.org/draft-04/schema#';
    $scope.fromSchema.id = 1;
    $scope.fromSchema.title = 'My Form'
    $scope.fromSchema.type = 'object';
    $scope.fromSchema.properties = [];
    $scope.fromSchema.required = [];

    // create new field button click
    $scope.addNewField = function() {

        // incr field_id counter
        var indexId = 0;
        for (var n = 0; n < $scope.form.form_fields.length; n++) {
            if ($scope.form.form_fields[n].field_id > indexId)
                indexId = $scope.form.form_fields[n].field_id;
        }
        indexId++;
        var newField = {
            "field_id": indexId,
            "field_title": "New field - " + (indexId),
            // "field_group": "",
            "field_type": $scope.addField.new,
            "field_value": "",
            "field_required": true,
            "field_patten": "",
            "field_validators": [],
            "field_minLength": 0,
            "field_maxLength": 0,
            "field_minValue": 0,
            "field_maxValue": 0,
            "field_step": 0,
            "field_same": "",
            "field_less": "",
            "field_larger": "",
            "field_disabled": false,
            "field_ingrid": true
        };

        // put newField into fields array
        $scope.form.form_fields.push(newField);
        // add jquery-ui sortable  created by yangshuo, typed by mark
        var baseData = $scope.form.form_fields;
        $("#sortable").sortable({
            handle: "a",
            stop: function(event, ui) {
                var sortList = [];
                var htn = $("div .idclass");
                for (var n = 0; n < htn.length; n++) {
                    var index = parseInt($(htn[n]).text());
                    sortList.push(baseData[index - 1]);
                }
                $scope.form.form_fields = sortList;
                // console.log($scope.form);
            }
        });
    }

    // deletes particular field on button click
    $scope.deleteField = function(field_id) {
        for (var i = 0; i < $scope.form.form_fields.length; i++) {
            if ($scope.form.form_fields[i].field_id == field_id) {
                $scope.form.form_fields.splice(i, 1);
                break;
            }
        }
    }

    // add new option to the field
    $scope.addOption = function(field) {
        if (!field.field_options)
            field.field_options = new Array();

        var lastOptionID = 0;

        if (field.field_options[field.field_options.length - 1])
            lastOptionID = field.field_options[field.field_options.length - 1].option_id;

        // new option's id
        var option_id = lastOptionID + 1;

        var newOption = {
            "option_id": option_id,
            "option_title": "Option " + option_id,
            "option_value": option_id
        };

        // put new option into field_options array
        field.field_options.push(newOption);
    }

    // delete particular option
    $scope.deleteOption = function(field, option) {
        for (var i = 0; i < field.field_options.length; i++) {
            if (field.field_options[i].option_id == option.option_id) {
                field.field_options.splice(i, 1);
                break;
            }
        }
    }

    $scope.previewOn = function() {
        if ($scope.form.form_fields == null || $scope.form.form_fields.length == 0) {
            alert("还没有创建新Form！");
        } else {
            angular.copy($scope.form, $scope.previewForm);
        }
    }

    $scope.previewOff = function() {
        $scope.previewForm.form_fields == null;
    }

    // decides whether field options block will be shown (true for dropdown and radio fields)
    $scope.showAddOptions = function(field) {
        if (field.field_type == "radio" || field.field_type == "checkbox" || field.field_type == "dropdown" )
            return true;
        else
            return false;
    }

    // deletes all the fields
    $scope.reset = function() {
        $scope.form.form_fields.splice(0, $scope.form.form_fields.length);
        $scope.addField.lastAddedID = 0;
        angular.copy($scope.form, $scope.previewForm);
    }

    //20150425 mark save json
    $scope.saveJson = function() {
        var formid = $scope.loadForm.ID == "" ? "0" : $scope.loadForm.ID;
        // console.log($scope.form)
        FormService.saveFormJson($scope.form, formid).then(function(data) {
            if (data.data.ret) {
                FormService.formsType().then(function(formType) {
                    $scope.loadForm.selects = formType;
                });
                alert("恭喜，创建成功");
                $scope.reset();
            }

        });
    }

    //20150425 mark schema
    $scope.createSchema = function() {

        $scope.fromSchema.properties = [];
        $scope.fromSchema.required = [];

        var thisType = "";
        var thisPattern = "";
        switch ($scope.addField.new) {
            case 'number':
                thisType = "number";
                break;
            default:
                thisType = "string";
                break;
        }

        var newSchemaPpObj = "{ " + $scope.addField.lastAddedID +
            " : { type : thisType, pattern : thisPattern } }";

        newSchemaPpObj = eval('(' + newSchemaPpObj + ')');

        $scope.fromSchema.properties.push(newSchemaPpObj);

        var i = $scope.addField.lastAddedID - 1;

        if ($scope.form.form_fields[i].field_required == true) {
            $scope.fromSchema.required.push($scope.addField.lastAddedID);
        }
    }

    //20150416 jj --ADD-------START---------------------
    $scope.loadForm = {};
    $scope.loadForm.ID = "";

    $scope.loadForms = function(){
        FormService.formsType().then(function(formType) {
            $scope.loadForm.selects = formType;
        });
    }

    $scope.thisLoadForm = function() {
        var formid = $scope.loadForm.ID == "" ? "0" : $scope.loadForm.ID;
        if (!$scope.loadForm.ID == "") {
            FormService.getFormByID(formid).then(function(form) {
                // console.log(form);
                angular.copy(form, $scope.form);
                var baseData = $scope.form.form_fields;
                $("#sortable").sortable({
                    stop: function(event, ui) {
                        var sortList = [];
                        var htn = $("div .idclass");
                        baseData.sort(function(a, b) {
                            return a.field_id - b.field_id;
                        });
                        for (var n = 0; n < htn.length; n++) {
                            var index = parseInt($(htn[n]).text());
                            sortList.push(baseData[index - 1]);
                        }
                        $scope.form.form_fields = sortList;
                        // console.log($scope.form);
                    }
                });
            });
        } else {
            $scope.reset();
        }
    }
    // ---------------END----------------------

    $scope.showValidate = function(field) {
        if (field.field_type == "radio" || field.field_type == "dropdown" || field.field_type == "checkbox" || field.field_type == "pwdConfirm")
            return true;
        else
            return false;
    }

    $scope.showLengthLimit = function(field) {
        if (field.field_type == "radio" || field.field_type == "dropdown" || field.field_type == "checkbox" || field.field_type == "number")
            return false;
        else
            return true;
    }

    $scope.getPwdConfirmPatten = function(field) {
        for (var i = 0; i < $scope.form.form_fields.length; i++) {
            if ($scope.form.form_fields[i].field_id == field.field_same) {
                field.field_patten = $scope.form.form_fields[i].field_patten;
            }
        }
    }

    //20150424 created by mark<jiongxiang.dai@newtouch.cn>  validator
    $scope.setValidator = function(){
        FormService.validators().then(function(data) {
            $scope.Validators = data;
        });
    }

    // by zhou


    $scope.validatorFunc = function(state) {
        if (state > 0) {
            return true;
        } else {
            return false;
        }
    }

    $scope.showChooseValidate = function(field) {
        if (field.field_type == "textfield")
            return true;
        else
            return false;
    }

    $scope.getRequired = function(isRequired) {
        if (isRequired) {
            return "required";
        } else {
            return "non_required";
        }
    }

    // add new option to the field
    $scope.addValidator = function(field) {
        if (!field.field_validators)
            field.field_validators = new Array();

        var lastValidatorID = 0;

        if (field.field_validators[field.field_validators.length - 1])
            lastValidatorID = field.field_validators[field.field_validators.length - 1].validator_id;

        // new option's id
        var validator_id = lastValidatorID + 1;

        var newValidator = {
            "validator_id": validator_id,
            "validator_value": field.validate
        };

        // put new option into field_options array
        field.field_validators.push(newValidator);
    }

    // delete particular option
    $scope.deleteValidator = function(field, validator) {
        for (var i = 0; i < field.field_validators.length; i++) {
            if (field.field_validators[i].validator_id == validator.validator_id) {
                field.field_validators.splice(i, 1);
                break;
            }
        }
    }

    $scope.getPasswordForm = function() {
        var ids = [];
        for (var i = 0; i < $scope.form.form_fields.length; i++) {
            if ($scope.form.form_fields[i].field_type == "password") {
                ids.push($scope.form.form_fields[i]);
            }
        }
        return ids;
    }

    $scope.getValidators = function(field) {
        // console.log(field);
        var validators = "[";
        if (field.field_required == true) {
            validators += "required";
        }
        for (var i = 0; i < field.field_validators.length; i++) {
            validators += ",";
            validators += field.field_validators[i].validator_value;
        }
        validators += "]";
        return validators;
    }

    $scope.isNumberField = function(field) {
        // console.log(field);
        if (field.field_type == "number") {
            return true;
        }
        return false;
    }

    //================================添加验证规则======================================
    // 读取validator.json版本


    $scope.getPattens = function() {
        return $scope.Validators;
    }

    $scope.validatorField = {};
    $scope.validatorField.invokeTypes = ["watch", "blur"];
    $scope.validatorField.editID = -1;
    $scope.validatorField.lastAddedID = 0;

    $scope.addNewValidatorField = function(newValidatorField) {
        // console.log($scope.validatorField.isEdit);
        // console.log(newValidatorField);
        // incr field_id counter
        if ($scope.validatorField.editID < 0) {
            if ($scope.Validators.length > 0) {
                $scope.validatorField.lastAddedID = $scope.Validators[$scope.Validators.length - 1].validator_id;
            }
            $scope.validatorField.lastAddedID++;

            var newValidator = {
                "validator_id": $scope.validatorField.lastAddedID,
                "validator_name": newValidatorField.validator_name,
                //"validator_tag": newValidatorField.validator_tag,
                "validator_invoke": newValidatorField.validator_invoke,
                "validator_patten": newValidatorField.validator_patten,
                "validator_error": newValidatorField.validator_error
            };
            // console.log(newValidator);
            // put newField into fields array
            // if(newField.type)

            $scope.Validators.push(newValidator);
            //$scope.validatorField.validator_tag = "";
            $scope.validatorField.validator_invoke = "";
            $scope.validatorField.validator_patten = "";
            $scope.validatorField.validator_name = "";
            $scope.validatorField.validator_error = "";
            // console.log($scope.Validators);
        } else {
            for (var i = 0; i < $scope.Validators.length; i++) {
                if ($scope.Validators[i].validator_id == $scope.validatorField.editID) {
                    $scope.Validators[i].validator_name = newValidatorField.validator_name;
                    //$scope.Validators[i].validator_tag = newValidatorField.validator_tag;
                    $scope.Validators[i].validator_invoke = newValidatorField.validator_invoke;
                    $scope.Validators[i].validator_patten = newValidatorField.validator_patten;
                    $scope.Validators[i].validator_error = newValidatorField.validator_error;
                    break;
                }
            }
            $scope.validatorField.editID = -1;
            //$scope.validatorField.validator_tag = "";
            $scope.validatorField.validator_invoke = "";
            $scope.validatorField.validator_patten = "";
            $scope.validatorField.validator_name = "";
            $scope.validatorField.validator_error = "";
            // console.log($scope.Validators);
        }
    }

    $scope.deleteValidatorField = function(validator) {
        for (var i = 0; i < $scope.Validators.length; i++) {
            if ($scope.Validators[i].validator_id == validator.validator_id) {
                $scope.Validators.splice(i, 1);
                break;
            }
        }
    }

    $scope.getTagTypes = function() {
        return $scope.validatorField.tagTypes;
    }

    $scope.getInvokeTypes = function() {
        return $scope.validatorField.invokeTypes;
    }

    $scope.editValidatorField = function(validator) {
        //$scope.validatorField.validator_tag=validator.validator_tag;
        $scope.validatorField.validator_invoke = validator.validator_invoke;
        $scope.validatorField.validator_patten = validator.validator_patten;
        $scope.validatorField.validator_name = validator.validator_name;
        $scope.validatorField.validator_error = validator.validator_error;
        $scope.validatorField.editID = validator.validator_id;
    }

});

            ///////////////////////////////////////////////////////////////
            //  20150428 Created by Mark<jiongxiang.dai@newtouch.cn>     //
            // choose spaces ,schemas ,                                  //
            //////////////////////////////////////////////////////////////
dashboardApp.controller('PreCreateCtrl', function($scope, spaceIdSchema, getAllId, schemaByNameById, $compile) {
    spaceModel = [{
        realname: 'Content.XEDU',
        name: 'XEDU'
    }, {
        realname: 'Content.MgmtSystem',
        name: 'CMS'
    }, {
        realname: 'System.Metadata',
        name: 'MDE'
    }, {
        realname: 'System.Configuration',
        name: 'CFG'
    }]

    $scope.spaces = spaceModel;
    $scope.schemas = {};

    $scope.spaceChanged = function(space){
        if(space != null && space != ""){
            spaceIdSchema.schema(space.realname).success(function(data){
                $scope.schemas = data;
            });
        }
    }

    $scope.showTag = function(obj){
        if(obj==undefined){
            return false;
        }else{
            return true;
        }
    }

    $scope.schemaChanged = function(schema){
        if(schema != null && schema != ""){
            spaceIdSchema.columns(schema.id).success(function(data){
                $scope.checkboxes = data;
            })
        }else{
            $scope.schemas = {};
        }
    }

    $scope.checkedCheckbox = [];
    $scope.clicked = function(e){
        if(e.target.checked){
            $scope.checkedCheckbox.push(e.target.id);
        }else{
            for (var i in $scope.checkedCheckbox) {
                if ($scope.checkedCheckbox[i] == e.target.id) {
                    $scope.checkedCheckbox.splice(i, 1);
                }
            }
        }
    }

    $scope.nextStep = function(schema){
        $scope.form = {};
        $scope.form.appspace = schema.appSpaceId;
        $scope.form.form_id = schema.id;
        $scope.form.form_name = schema.entityName;
        $scope.form.form_fields = [];

        $scope.previewForm = {};

        var field_id = 1;
        for(var i=0; i < schema.properties.length; i++){
            var newField = {
                "field_id": "",
                "field_title": "",
                "field_desc": "",
                "field_type": "",
                "field_value": "",
                "field_required": true,
                "field_patten": "",
                "field_validators": [],
                "field_minLength": 0,
                "field_maxLength": 0,
                "field_minValue": 0,
                "field_maxValue": 0,
                "field_step": 0,
                "field_same": "",
                "field_less": "",
                "field_larger": "",
                "field_disabled": false,
                "field_ingrid": true
            };

            for(var j=0; j < $scope.checkedCheckbox.length; j++){
                if(schema.properties[i].name == $scope.checkedCheckbox[j]){
                    newField.field_id = j+1;
                    newField.field_title = $scope.checkedCheckbox[j];
                    newField.field_desc = schema.properties[i].description;
                    $scope.form.form_fields.push(newField);
                    continue;
                }
            }
        }
        $("#accordionPanel").show();
        $("#choosePanel").hide();
    }

    //add validator
    spaceIdSchema.validator("ValidationRule").success(function(data){
        $scope.validatorSchema = data;
    })

    //20150429 yangshuo
    $scope.addField = [{
        name: 'textfield',
        value: 'Textfield',
        description: "一般输入框"
    }, {
        name: 'number',
        value: 'Number',
        description: "数字输入框"
    }, {
        name: 'password',
        value: 'Password',
        description: "密码输入框"
    }, {
        name: 'email',
        value: 'Email',
        description: "邮件输入框"
    }, {
        name: 'radio',
        value: 'Radio Buttons',
        description: "单选按钮"
    }, {
        name: 'checkbox',
        value: 'Checkbox',
        description: "多选按钮"
    }, {
        name: 'dropdown',
        value: 'Dropdown List',
        description: "下拉选择框"
    }, {
        name: 'date',
        value: 'Date',
        description: "日期"
    }, {
        name: 'textarea',
        value: 'Text Area',
        description: "多行文本输入框"
    }, {
        name: 'hidden',
        value: 'Hidden',
        description: "hidden"
    }];

    var id = 0;
    $scope.saveData = function() {
        getAllId.get({}, function(data) {
            for (var i = 0; i < data.length; i++) {
                if (Number(data[i].id) > id) {
                    id = Number(data[i].id);
                }
            }
            id++;
            $scope.fromdata = {};
            $scope.fromdata.id = id;
            $scope.fromdata.appSpace = $scope.form.appspace;
            $scope.fromdata.name = $scope.tempSchema.entityName+"_"+id;
            $scope.fromdata.path = "survey";
            $scope.fromdata.data = JSON.stringify($scope.form);
            console.log($scope.fromdata)
            //保存业务表from数据
            schemaByNameById.put({NAME:"Form",ID:id}, $scope.fromdata);
            //删除数据
            //$http.delete("http://localhost:8080/MDE/DSE/Form('1002')").success(function(data) {
            //    console.log(data.delete);
            //});
        });
    }

    // previewForm - for preview purposes, form will be copied into this
    // otherwise, actual form might get manipulated in preview mode
    $scope.previewForm = {};
    $scope.previewOn = function() {
        angular.copy($scope.form, $scope.previewForm);
        $("#accordionPanel").hide();
        $("#preview").show();
        $("#preContain").empty().append($compile("<form-directive form=\"previewForm\"></form-directive>")($scope));

    }

    $scope.stepBack = function(divShow, divHide){
        $("#"+divHide).hide();
        $("#"+divShow).show()
    }

    //201450430 yangshuo
    $scope.showAddOptions = function(field) {
        if (field.field_type == "radio" || field.field_type == "checkbox" || field.field_type == "dropdown" )
            return true;
        else
            return false;
    }
    // add new option to the field
    $scope.addOption = function(field) {
        if (!field.field_options)
            field.field_options = new Array();

        var lastOptionID = 0;

        if (field.field_options[field.field_options.length - 1])
            lastOptionID = field.field_options[field.field_options.length - 1].option_id;

        // new option's id
        var option_id = lastOptionID + 1;

        var newOption = {
            "option_id": option_id,
            "option_title": "Option " + option_id,
            "option_value": option_id
        };

        // put new option into field_options array
        field.field_options.push(newOption);
    }

    // delete particular option
    $scope.deleteOption = function(field, option) {
        for (var i = 0; i < field.field_options.length; i++) {
            if (field.field_options[i].option_id == option.option_id) {
                field.field_options.splice(i, 1);
                break;
            }
        }
    }

});