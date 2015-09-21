'use strict';

app.directive('newBtns', function ($http, $compile) {
	var type = "";
	var btnClass = "";
	var getBtn = function(btns) {
        type = btns==""?"提交":btns.name;
        return type;
    }
    var getClass = function(btns) {
        btnClass = btns.class;
        return btnClass;
    }

	var linker = function(scope, element) {
        console.log(scope)
		var events = scope.btns.events;
		var btn = '<button class="btn center" type="button" ';
		// add events
        var evt = "";
		for(var e=0; e<events.length; e++){
            var event="";
            if(events[e].event==1)
                event = "ng-show";
            else if(events[e].event==2)
                event = "ng-disabled";
            else if(events[e].event==3)
                event = "ng-click";
            else if(events[e].event==4)
                event = "ng-focus";
            else if(events[e].event==5)
                event = "ng-change";
            else if(events[e].event==6)
                event = "ng-checked";
            else if(events[e].event==7)
                event = "ng-keyup";
            else if(events[e].event==8)
                event = "ng-if";
            btn += " " +event + "=" + events[e].function;
            
            //-----事件------
            var funStr = "scope.#funName=#funBody;";
            var funName = events[e].function.substr(0, events[e].function.indexOf('('));
            evt += funStr.replace("#funName", funName).replace('#funBody', events[e].script);
            eval(evt);
        }
		type = getBtn(scope.btns);
		btnClass = getClass(scope.btns);
		btn += "><i class='glyphicon glyphicon-" +btnClass+ "'></i> " +type+ "</button>";
		element.append($(btn));
        $compile(element.contents())(scope);
	}

	return {
		template: '',
        restrict: 'E',
        scope: false,
        link: linker
	}
});