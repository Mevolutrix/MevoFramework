app.service('NewCreateService', ['$http', function ($http) {
	return {
		// get
		forms: function(){
			return $http({
				url: "/MDE/DSE/EntitySchema?$filter=appSpaceId eq 'Content.XEDU'",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("forms ERROR");
				alert("ERROR");
				return false;
			})
		},
		form: function(form){
			return $http({
				url: "/MDE/DSE/EntitySchema(\'"+form+"\')",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("form ERROR");
				alert("ERROR");
				return false;
			})
		},
		grids: function(){
			return $http({
				url: "/MDE/DSE/gridList?$filter=appSpace eq 'MDE'",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("grids ERROR");
				alert("ERROR");
				return false;
			})
		},
		grid: function(grid){
			return $http({
				url: "/MDE/DSE/gridList(\'"+grid+"\')",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("grid ERROR");
				alert("ERROR");
				return false;
			})
		},
		// 查询所有建立的业务form
		cForms: function(){
			return $http({
				url: "/MDE/DSE/CForm",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("cForms ERROR");
				alert("ERROR");
				return false;
			})
		},
		getForm: function(modifyForms){
			return $http({
				url: "/MDE/DSE/cForm(\'" +modifyForms+ "\')",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("get cForm ERROR");
				alert("ERROR");
				return false;
			})
		},
		updateForm: function(form){
			return $http({
				url: "/MDE/DSE/cForm(\'" +form.id+ "\')",
				method: "POST",
				data: form
			}).success(function(data){
				return data;
			}).error(function(){
				console.log("updateForm cForm ERROR");
				alert("ERROR");
				return false;
			})
		},
		// new grid
		saveGrid: function(grid){
			return $http({
				url: "/MDE/DSE/gridList",
				method: 'PUT',
				data: grid
			}).success(function(data){
				return data;
			}).error(function(data){
				console.log("saveGrid ERROR");
				alert("ERROR");
				return false;
			})
		},
		updateGrid: function(grid){
			return $http({
				url: "/MDE/DSE/gridList(\'"+grid.id+"\')",
				method: 'POST',
				data: grid
			}).success(function(data){
				return data;
			}).error(function(data){
				console.log("updateGrid ERROR");
				alert("ERROR");
				return false;
			})
		},
		// new form
		saveForm: function(form){
			console.log(form)
			return $http({
				url: "/MDE/DSE/CForm",
				method: "PUT",
				data: form
			}).success(function(data){
				return data;
			}).error(function(data){
				console.log("saveForm ERROR");
				alert("ERROR");
				return false;
			})
		},
		// find by id
		findGrid: function(id){
			return $http({
				url: "/MDE/DSE/CForm("+ id + ")",
				method: "POST"
			}).success(function(data){
				console.log(data)
				return data;
			}).error(function(data){
				console.log("ERROR");
				alert("ERROR");
				return false;
			})
		},
		// find list
		findValidators: function(){
			return $http({
				url: "/MDE/DSE/ValidationRule",
				method: "GET"
			}).success(function(data){
				return data;
			}).error(function(data){
				console.log("ERROR");
				alert("ERROR");
				return false;
			})
		},
		getEventEnum:function(){
			return $http({
				url:"/MDE/DSE/PropertyReference('MDE.events.event')",
				method:"GET"
			}).success(function(data){
				return data;
			}).error(function(data){
				console.log("ERROR");
				alert("ERROR");
				return false;
			});
		}
	}
}])

