/**
 * Created by jinjie on 2015/5/14.
 */

cffexApp.config(function($routeProvider){
    $routeProvider.when('/index', {
        templateUrl: 'views/index.html',
        controller: 'IndexCtrl'
    }).when('/train_notice', {
        templateUrl: 'views/train-notice.html',
        controller: 'TrainNoticeCtrl'
    }).otherwise({ redirectTo: '/IndexCtrl' });
});