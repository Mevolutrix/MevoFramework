'use strict';

dashboardApp.service('FormService', function FormService($http, schemaByNameById) {

    // var formsJsonPath = './static-data/sample_forms.json';
    //var allFormPath = 'http://172.31.224.83:8080/getFormForSelect.do?callback=JSON_CALLBACK';
    //var loadFormPath = 'http://172.31.224.83:8080/getFormByID.do?callback=JSON_CALLBACK&id=';
    //var saveFormJsonPath = 'http://172.31.224.83:8080/saveFormInfo.do?callback=JSON_CALLBACK&id=';
    var getAllID  =  function() {
        //
        var URL = "http://localhost:8080/MDE/DSE/Form?$select=id ";
        $http({
            url:URL,
            method:"GET"
        }).success(function(data){
            return data;
        });
    };

    var formData = {};

    return {
        fields:[
            {
                name : 'textfield',
                value : 'Textfield',
                description: "一般输入框"
            },
            {
                name : 'number',
                value : 'Number',
                description: "数字输入框"
            },
            {
                name : 'password',
                value : 'Password',
                description: "密码输入框"
            },
            {
                name : 'email',
                value : 'Email',
                description: "邮件输入框"
            },
            {
                name : 'radio',
                value : 'Radio Buttons',
                description: "单选按钮"
            },
            {
                name : 'checkbox',
                value : 'Checkbox',
                description: "多选按钮"
            },
            {
                name : 'dropdown',
                value : 'Dropdown List',
                description: "下拉选择框"
            },
            {
                name : 'date',
                value : 'Date',
                description: "日期"
            },
            {
                name : 'textarea',
                value : 'Text Area',
                description: "多行文本输入框"
            },
            {
                name : 'hidden',
                value : 'Hidden',
                description: "hidden"
            }
        ],
        //form: function (id) {
        //    // $http returns a promise, which has a then function, which also returns a promise
        //    return $http.get(allFormPath).then(function (response) {
        //        var requestedForm = {};
        //        angular.forEach(response.data, function (form) {
        //            if (form.form_id == id) requestedForm = form;
        //        });
        //        return requestedForm;
        //    });
        //},
        //forms: function() {
        //    return $http.get(allFormPath).then(function (response) {
        //        return response.data;
        //    });
        //},
        // jj --start--
        getAllForm: function() {
            //return $http.jsonp(allFormPath).then(function (response) {
            //    return response.data;
            //});

            //
            var URL = "http://localhost:8080/MDE/DSE/FORM";
            return $http({
                url:URL,
                method:"GET"
            }).success(function(data){
                return data;
            });
        },
        getFormByID: function(id) {
            //return $http.jsonp(loadFormPath + id).then(function (response) {
            //    return response.data;
            //});

            ////
            //return schemaByNameById.get({NAME: "Form", ID:id});

            //
            var URL = "http://localhost:8080/MDE/DSE/FORM(\'" + id + "\')";
            return $http({
                url:URL,
                method:"GET"
            }).success(function(data){
                //console.log(data.data);
                return data.data;
            });
        },
        // jj --end--

        saveForm:function(json, id){
            //return $http({
            //    url: saveFormJsonPath + id + "&detail=" + JSON.stringify(json),
            //    method: "JSONP"
            //}).success(function(data){
            //    return data;
            //}).error(function(data){
            //    alert("ERROR")
            //});
            //
            //var URL = "http://localhost:8080/MDE/DSE/FORM(\'" + id + "\')";
            //$http({
            //    url:URL,
            //    method:"GET"
            //}).success(function(data){
            //    return data;
            //});

            var data = getAllID();
            var flg = false;
            var sid = 0;
            // 遍历
            for (var i = 0; i < data.length; i++) {
                if (Number(data[i].id) == id) {
                    flg = true;
                }

                if (Number(data[i].id) > id) {
                    sid = Number(data[i].id);
                }
            }
            //更新/新规
            if (flg) {
                formData = {};
                formData.id = id;
                formData.appSpace = "Content.XEDU";
                formData.name = "fromdata";
                formData.path = "survey";
                formData.data = JSON.stringify(json);
                //更新业务表from数据
                return  schemaByNameById.save({NAME:"Form",ID:id}, formData);
            } else {
                sid++;
                formData = {};
                formData.id = sid;
                formData.appSpace = "Content.XEDU";
                formData.name = "fromdata";
                formData.path = "survey";
                formData.data = JSON.stringify(json);
                //保存业务表from数据
                return schemaByNameById.put({NAME:"Form",ID:sid}, formData);
            }
        },
        // jj --end--

        //by zhou
        validates: function(){
            return $http.get('./static-data/sample_validate.json').then(function(response){
                return response.data;
            });
        },
        validate: function(type){
            return $http.get('./static-data/sample_validate.json').then(function(response){
                var requestedForm = {};
                angular.forEach(response.data, function (form) {
                    if (form.type == type) requestedForm = form;
                });
                return requestedForm;
            });
        },
        validators: function(){
            return $http.get('./static-data/validators.json').then(function(response){
                // console.log(response.data);
                return response.data;
            });
        },
        isRequired:function(isRequired){
            if(isRequired){
                return "Required";
            }else{
                return "not_required";
            }
        }

    };
});


dashboardApp.service('DataService', function DataService($http, dataTable) {
    var saveFormData = 'http://172.31.224.83:8080/saveFormData.do?callback=JSON_CALLBACK&id=';
    var getFormDataByID = 'http://172.31.224.83:8080/getFormDataByID.do?callback=JSON_CALLBACK&id=';
    var getAllDataID = function() {
        return $http.get("http://localhost:8080/XEDU/DSE/Survey?$select=id").success(function(data){
            console.log(data);
            return data;
        });
        //return $resource('/MDE/DSE/Form?$select=id',{},   {
        //    get:{method:"GET", params:{},isArray: true}
        //}) ;

    };

    return{
        saveData: function(json){
             getAllDataID().success(function(data){
                 var sid = 0;
                 for (var i = 0; i < data.length; i++) {
                     if (Number(data[i].id) > sid) {
                         sid = Number(data[i].id);
                     }
                 }
                 //
                 sid = sid + 2;
                 //
                 json.id = Number(sid);
                 json.question1 = Number(json.question1);
                 json.question2 = Number(json.question2) ;
                 //
                 dataTable.put({NAME:"Survey",ID: sid}, json);
                 //
                 return ;
            });
        },
        delData:  function(id) {
            return $http.delete("http://localhost:8080/XEDU/DSE/Survey(\'" + id + "\')").success(function(data) {
                return data;
            });
        },
        getAllData: function() {
            return $http.get("http://localhost:8080/XEDU/DSE/Survey").success(function(data){
                return data;
            });
        },
        getDataByID: function(id) {
            return $http.get("http://localhost:8080/XEDU/DSE/Survey(\'" + id + "\')").success(function(data){
                return data;
            });
        }

    }
    
})