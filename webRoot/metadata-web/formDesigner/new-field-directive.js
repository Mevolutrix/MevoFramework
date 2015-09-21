'use strict';

app.directive('newField', function ($http, $compile) {
    var type = "";
    var getTemplateUrl = function(field) {
        type = field.field_type==""?"text":field.field_type;
        var templateUrl = '';

        switch(type) {
            case 'text':
                templateUrl = './formDesigner/views/newfield/text.html';
                break;
            case 'email':
                templateUrl = './formDesigner/views/newfield/email.html';
                break;
            case 'textarea':
                templateUrl = './formDesigner/views/newfield/textarea.html';
                break;
            case 'checkbox':
                templateUrl = './formDesigner/views/newfield/checkbox.html';
                break;
            case 'date':
                templateUrl = './formDesigner/views/newfield/date.html';
                break;
            case 'select':
                templateUrl = './formDesigner/views/newfield/dropdown.html';
                break;
            case 'hidden':
                templateUrl = './formDesigner/views/newfield/hidden.html';
                break;
            case 'password':
                templateUrl = './formDesigner/views/newfield/password.html';
                break;
            case 'radio':
                templateUrl = './formDesigner/views/newfield/radio.html';
                break;
            case 'number':
                templateUrl = './formDesigner/views/newfield/number.html';
                break;
            case 'tel':
                templateUrl = './formDesigner/views/newfield/tel.html';
                break;
            case 'file':
                templateUrl = './formDesigner/views/newfield/file.html';
                break;
            case 'datetime':
                templateUrl = './formDesigner/views/newfield/datetime.html';
                break;
            default :
                templateUrl = './formDesigner/views/newfield/text.html';
                break;
        }
        return templateUrl;
    }

    var linker = function(scope, element) {
        // GET template content from path
        var templateUrl = getTemplateUrl(scope.field);

        $http({
            method: 'GET',
            url: templateUrl,
            cache: true
        }).success(function(data) {
            var events = scope.field.events;
            var id = scope.field.field_field;
            var ctrl = $(data);
            ctrl.find("label").addClass("control-label");
            if(type=="select"){
                ctrl.find("select[class*=form]").attr("id", id);
            }
            else if(type=="textarea"){
                ctrl.find("textarea[class*=form]").attr("id", id);
            }
            else {
                ctrl.find("input[type*="+type+"]").attr("id", id);
            }
            
            // add events
            var evt = "";
            if(events.length>0){
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
                    // ctrl.find("input[type*="+type+"]").attr(event, events[e].function);
                    
                    if(type=="select"){
                        ctrl.find("select[class*=form]").attr(event, events[e].function);
                    }
                    else if(type=="textarea"){
                        ctrl.find("textarea[class*=form]").attr(event, events[e].function);
                    }
                    else {
                        ctrl.find("input[type*="+type+"]").attr(event, events[e].function);
                    }
                    
                    //-----事件------
                    var funStr = "scope.#funName=#funBody;";
                    var funName = events[e].function.substr(0, events[e].function.indexOf('('));
                    evt += funStr.replace("#funName", funName).replace('#funBody', events[e].script);
                    eval(evt);
                }
            }
            
            element.append(ctrl);
            // element.html(data);
            $compile(element.contents())(scope);
        });
    }

    return {
        template: '',
        restrict: 'E',
        scope: {
            // data: '=',
            field: '='
        },
        link: linker
    };
});