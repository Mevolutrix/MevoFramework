'use strict';



dashboardApp.controller('CreateCtrl', function($scope, $dialog, FormService) {
    // preview form mode
    $scope.previewMode = false;

    // new form
    $scope.form = {};
    $scope.form.form_id = 1;
    $scope.form.form_name = 'My Form';
    $scope.form.form_fields = [];

    // previewForm - for preview purposes, form will be copied into this
    // otherwise, actual form might get manipulated in preview mode
    $scope.previewForm = {};

    // add new field drop-down:
    $scope.addField = {};
    $scope.addField.types = FormService.fields;
    $scope.addField.new = $scope.addField.types[0].name;
    $scope.addField.lastAddedID = 0;

    // accordion settings
    $scope.accordion = {}
    $scope.accordion.oneAtATime = true;

    // create new field button click
    $scope.addNewField = function() {

        // incr field_id counter
        $scope.addField.lastAddedID++;

        var newField = {
            "field_id": indexId,
            "field_title": "New field - " + (indexId),
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
        $("#sortable").sortable({
            handle: "a"
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
        if (field.field_type == "radio" || field.field_type == "dropdown")
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

    //20150416 jj --ADD-------START---------------------
    $scope.loadForm = {};
    $scope.loadForm.ID = "";

    $scope.loadForms = function(){
        alert(1)
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


});