/**
 * @license
 * Copyright 2011 Dan Vanderkam (danvdk@gmail.com)
 * MIT-licensed (http://opensource.org/licenses/MIT)
 */

// A dygraph "auto-loader".

// Check where this script was sourced from. If it was sourced from
// '../dygraph-dev.js', then we should source all the other scripts with the
// same relative path ('../dygraph.js', '../dygraph-canvas.js', ...)
(function() {
    console.log("start");
    var src=document.getElementsByTagName('script');
    var script = src[src.length-1].getAttribute("src");

    // This list needs to be kept in sync w/ the one in generate-combined.sh
    // and the one in jsTestDriver.conf.
    var url ="/WEB/Portal/metadata-web/lib/";
    var source_files = [
        "jquery-2.1.3.js",
        "jquery.ztree.core-3.5.js",
        "Angular.js",
        "angular-animate.js",
        "angular-resource.js",
        "angular-route.js",
        "bootstrap.min.js",
        "jquery.cleanHtml.js",
        //     "jquery.json-2.4.js",
        "jquery-ui.js",
        "jsxcompressor.min.js",
        "ng-grid-2.0.11.debug.js",
        "../../lib/routeParams.js"

    ];

    for (var i = 0; i < source_files.length; i++) {
        document.write('<script type="text/javascript" src="' + url + source_files[i] + '"></script>\n');
    }

/*    var url =" ";
    var source_files = [ "directive.js"];
    for (var i = 0; i < source_files.length; i++) {
        document.write('<script type="text/javascript" src="' + url + source_files[i] + '"></script>\n');
    }*/
})();

