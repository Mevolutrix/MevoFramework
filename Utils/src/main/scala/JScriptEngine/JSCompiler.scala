package JScriptEngine
import java.util.concurrent.ConcurrentHashMap
import javax.script._

/**
 * Created by huzn on 2015/8/15.
 */
class JSCompiler(script:String) {
import JSCompiler._
  engine.eval({val sb = new StringBuilder()
    sb.append("function fun1(jsonData){var data = JSON.parse(jsonData);").append(script).append("; }; ")
    sb.result()})
  private val invokable = engine.asInstanceOf[Invocable]
  def invoke(jsonData:String) = invokable.invokeFunction("fun1",jsonData)
}
object JSCompiler {
  val engine = new ScriptEngineManager(this.getClass.getClassLoader).getEngineByName("nashorn")
  private val jsCache = new ConcurrentHashMap[String,JSCompiler]()
  def apply(script:String):JSCompiler =
    if (jsCache.contains(script)) jsCache.get(script)
    else Option(jsCache.putIfAbsent(script,new JSCompiler(script))).getOrElse(jsCache.get(script))
}
