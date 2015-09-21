'use strict';

var ViewCtrl = dashboardApp.controller('ViewCtrl', function($scope, FormService, DataService, $routeParams,$compile) {
    $scope.form = {};

    // // read form with given id
    //FormService.getFormByID($routeParams.id).then(function(form) {
    //  $scope.form = form;
    //});
    //
    //DataService.data($routeParams.id).then(function(data) {
    //  $scope.data = data;
    //});

    //20150416 jj --ADD-------START---------------------
    $scope.loadForm = {};
    $scope.loadForm.ID = "";

    FormService.getAllForm().success(function(formType) {
      $scope.loadForm.selects = formType;
    });

    $scope.thisLoadForm = function() {

        var formid = $scope.loadForm.ID == "" ? "0" : $scope.loadForm.ID;

        FormService.getFormByID(formid).success(function(form) {
          $scope.form = JSON.parse(form.data);
        });

        if (!(formid == "")) {
            DataService.getAllData().success(function(data) {
                $scope.data={};
                $scope.data.data = data;
            });
        } else {
            $scope.data={};
            $scope.data.data = [];
        }
      };

    $scope.delete = function(id) {
      var yes = confirm("Delete?");
      if (yes) {
          //
          DataService.delData(id);
          //
          DataService.getAllData().success(function(data) {
              $scope.data={};
              $scope.data.data = data;
          });
      }
    }
  }
);

var DetailCtrl = dashboardApp.controller('DetailCtrl', ['$scope', '$routeParams', 'FormService', 'DataService',
  function($scope, $routeParams, FormService, DataService) {
    DataService.data().then(function(data) {
      $scope.form = data.data[$scope.loadForm.ID];
    });
  }
]);

var InsertCtrl = dashboardApp.controller('InsertCtrl', ['$scope', '$routeParams', 'FormService', 'DataService',
  function($scope, $routeParams, FormService, DataService) {
    $scope.form = {};
    $scope.data = {};
    $scope.checkbox = [];

    $scope.showid = $routeParams.id;
    console.log($routeParams.id);

    FormService.getFormByID($routeParams.id).success(function(form) {
      //console.log(form.data);
      $scope.form = JSON.parse(form.data);
    });

    //FormService.getFormByID($routeParams.id).then(function(data) {
    //  $scope.form = data;
    //});
    //
    //DataService.data($routeParams.id).then(function(data) {
    //  $scope.data = data;
    //});

    $scope.toggle = function(obj, name) {
      // console.log($(obj.target)[0]);
      var newChecked;
      if ($(obj.target)[0].checked) {
        newChecked = {
          value: $(obj.target)[0].value,
          parent: name
        };
        $scope.checkbox.push(newChecked);
      } else {
        for (var i in $scope.checkbox) {
          if ($scope.checkbox[i].value == $(obj.target)[0].value) {
            $scope.checkbox.splice(i, 1);
          }
        }
      }
      // console.log($(obj.target)[0].value+$(obj.target)[0].checked);
    };

    $scope.addData = function() {
      var newDataTemp = '';
      for (var i = 0; i < $scope.form.form_fields.length; i++) {
        if ($scope.form.form_fields[i].field_type == "checkbox") {
          var check = "";
          for (var n = 0; n < $scope.checkbox.length; n++) {
            if ($scope.checkbox[n].parent == $scope.form.form_fields[i].field_title) {
              check += "\""+$scope.checkbox[n].value + "\",";
            }
          }
          if (check.length > 0) {
            check = check.substr(0, check.length - 1);
          }
          check = "[" + check + "]";
          newDataTemp = newDataTemp + '\"' + $scope.form.form_fields[i].field_title + '\":' + check + ',';
        } else
          newDataTemp = newDataTemp + '\"' + $scope.form.form_fields[i].field_title + '\":\"' + $scope.form.form_fields[i].field_value + '\",';
      };
      //newDataTemp = '{\"dataid\":\"' + (Number($scope.data.data.length) + 1) + '\",\"' + newDataTemp.substr(0, newDataTemp.length - 2) + '}';

      newDataTemp = "{" + newDataTemp.substr(0, newDataTemp.length - 1) + "}";

      //var newData = eval('(' + newDataTemp + ')');
      var newData = JSON.parse(newDataTemp);

      //$scope.data.form_id = $scope.form.form_id;
      //$scope.data.form_name = $scope.form.form_name;
      //$scope.data.data.push(newData);

      //console.log(newData);

      DataService.saveData(newData);

      location.href = "#/list";
    };

    $scope.cancel = function() {
      location.href = "#/" + $routeParams.id + "/list";
    }


  }
]);

var UpdateCtrl = dashboardApp.controller('UpdateCtrl', ['$scope', '$routeParams', 'FormService', 'DataService',
  function($scope, $routeParams, FormService, DataService) {
    $scope.form = {};
    $scope.data = {};
    $scope.update = true;

    $scope.showid = $routeParams.id;
    console.log($routeParams.id);
    FormService.getFormByID($routeParams.id).success(function(form) {
      $scope.form = JSON.parse(form.data);
    });

    if (!($routeParams.id == "")) {
      DataService.getAllData().success(function(data) {
        $scope.data={};
        $scope.data.data = data;


      });
    } else {
      $scope.data={};
      $scope.data.data = [];
    }

    //var id = $routeParams.dataid - 1;
    //FormService.getFormByID($routeParams.id).then(function(data) {
    //  $scope.form = data;
    //});
    //
    //DataService.data().then(function(data) {
    //  $scope.dataList = data.data;
    //  $scope.data = data.data[id];
    //  for (var i in $scope.form.form_fields) {
    //    for (var j in $scope.data) {
    //      // console.log($scope.form.form_fields[i].field_value+" "+$scope.data[$scope.form.form_fields[i].field_title]);
    //      $scope.form.form_fields[i].field_value = $scope.data[$scope.form.form_fields[i].field_title];
    //    }
    //  }
    //});
    //
    //DataService.data($routeParams.id).then(function(data) {
    //
    //  $scope.data = data;
    //  for (var i in $scope.form.form_fields) {
    //    for (var j in data.data[id]) {
    //      // console.log($scope.form.form_fields[i].field_value+" "+$scope.data.data[$routeParams.dataid - 1][$scope.form.form_fields[i].field_title]);
    //      $scope.form.form_fields[i].field_value = data.data[$routeParams.dataid - 1][$scope.form.form_fields[i].field_title];
    //      // console.log($scope.form.form_fields[i].field_value)
    //    }
    //  }
    //});

    $scope.updateData = function() {
      for (var k in $scope.form.form_fields) {
        for (var l in $scope.data.data[id]) {
          $scope.data.data[id][$scope.form.form_fields[k].field_title] = $scope.form.form_fields[k].field_value;
        }
      };
      DataService.saveData($scope.data, 0);
      alert("update 成功了，恭喜恭喜！");
    }

    $scope.cancel = function() {
      location.href = "#/" + $routeParams.id + "/list";
    }

    $scope.ischecked = function(v, d, g) {
      if (d && d.data) {
        for (var i = 0; i < d.data[id][g].length; i++) {
          if (d.data[id][g][i]) {
            if (d.data[id][g][i] == v.option_value)
              return true;
          }
        }
        return false;
      }
    }
  }
])