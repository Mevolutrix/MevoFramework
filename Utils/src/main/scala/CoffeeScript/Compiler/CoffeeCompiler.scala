/*package CoffeeScript.Compiler
import javax.script._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.{Await, Future}
import akka.actor.{ActorSystem, Props, Actor}
import java.io.{File, FileInputStream, IOException}
//import jdk.nashorn.api.scripting.NashornScriptEngineFactory
//import org.mozilla.javascript._

/*object CoffeeCompiler {
  private var globalScope:Scriptable = null
  def init(baseDir:String) = {
    val context = Context.enter()
    globalScope = try {
      val file = new File(baseDir + "/coffee-script.js")
      val input = new FileInputStream(file)
      val src = new Array[Byte](input.available())
      input.read(src)
      context.setOptimizationLevel(-1) // Without this, Rhino hits a 64K bytecode limit and fails
      val ret = context.initStandardObjects()
      context.evaluateString(ret, new String(src, "utf-8"), "coffee-script.js", 0, null);
      ret
    } catch {
      case e:IOException => throw new Exception("Coffee script file in webRoot missing or config is wrong.")
    } finally {
      Context.exit()
    }
  }
  def compile(src:String):String = {
    val context = Context.enter()
    try {
      val compileScope = context.newObject(globalScope)
      compileScope.setParentScope(globalScope)
      compileScope.put("coffeeScriptSource", compileScope, src)
      context.evaluateString(compileScope, "CoffeeScript.compile(coffeeScriptSource,{bare: true});",
        "JCoffeeScriptCompiler", 0, null).asInstanceOf[String]
    } catch {
      case e: JavaScriptException => throw new Exception(e.details());
    } finally {
      Context.exit()
    }
  }
  def dispose = Context.exit()
}*/

object CoffeeCompiler {
  val appSystem = ActorSystem("CoffeeScript")
  private val engine = new ScriptEngineManager(this.getClass.getClassLoader)
    .getEngineByName("nashorn")
  private val invokable = engine.asInstanceOf[Invocable]
    //scriptFactory.getScriptEngine

  private val initialActor = appSystem.actorOf(Props(new Actor {
    def receive = {
      case baseDir : String =>
        try {
          val file = new FileInputStream(new File(baseDir + "/coffee-script.js"))
          var binary = new Array[Byte](file.available())
          file.read(binary)
          engine.eval(new String(binary,"utf-8"))
          engine.eval("\nvar fun1 = function(jsonData){return jsonData("property"); }; " )
          sender() ! "OK"
        } catch {
          case e:IOException => throw new Exception("Coffee script file in webRoot missing or config is wrong.")
        }
    }
  }))
  private var initFuture:Future[Any] = _
  private var _initialized:Boolean = false
  def init(baseDir:String) = {
    initFuture=initialActor.ask(baseDir)(60000)
  }
  private def initialized():Unit = {
    if (!_initialized) {
      val timeout = Timeout(150000)
      Await.result(initFuture,timeout.duration)
      _initialized = true
      appSystem.shutdown()
    }
  }
  def compile(src:String):String = {
    try {
      initialized()
      println("Compile:"+src.substring(0,20))
      invokable.invokeFunction("fun1",src).asInstanceOf[String]
    } catch {
      case e: Exception => throw new Exception(e.getMessage())
    }
  }
}

*/