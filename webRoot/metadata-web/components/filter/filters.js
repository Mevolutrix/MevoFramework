/**
 * Created by zhoutao1 on 2015/6/9.
 */
app.filter('breakFilter',function(){
    return function(text){
        console.log(text)
        if(text !== undefined) return text.replace(/\n/g,'<br />');
    };
});
