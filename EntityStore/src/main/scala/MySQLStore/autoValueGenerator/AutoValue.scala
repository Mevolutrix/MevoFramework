package MySQLStore.autoValueGenerator
import java.util.concurrent.ConcurrentHashMap
import akka.actor.{Props, ActorRef}

/**
 * Cache for singleton actors which generate auto_increment value
 */
object AutoValue {
  private val autoKeyActorPool = new ConcurrentHashMap[String,ActorRef]()
  // One autoValue pool will allocate 1000 auto_increment keys each time. So can't expect the key is sequential
  val poolStep:Int = 1000
  val actorSystem = SBEServiceAccess.ServiceConfig.appSystem
  def apply(setName:String,keyType:Boolean=true):ActorRef =
    Option(autoKeyActorPool.get(setName)).getOrElse({
      val ret = Option(autoKeyActorPool.putIfAbsent(setName,
        actorSystem.actorOf(Props(new KeyGenerator))))
        .getOrElse(autoKeyActorPool.get(setName))
      ret ! AutoKeyInit(setName, keyType)
      ret
    })
}
