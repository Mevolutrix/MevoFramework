package EntityStore.Connection
import akka.actor._
import akka.io.Tcp
import EntityInterface._
import EntityStore.Interface._
import java.net.InetSocketAddress
import EntityStore.Communication._
import EntityStore.Client.ClientSession
import com.typesafe.config.ConfigFactory
import EntityStore.Interface.ReqSendMode._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Store Instance manage client channel configuration (Own and implement the clientChannelFactory)
 */
class StoreInstance(val appSpaceName:String,rPoolSize:Int,wrPoolSize:Int,connSelector:()=>Tcp.Connect) {
  private val clientChannelFactory:ChannelFactory = new StoreClientChannelPool(rPoolSize,wrPoolSize,connSelector)
  def session(requestId:Int,schema:IEntitySchema=null,requestMode:ReqSendMode=asyncMode,
              sender:ActorRef=null,readOnly:Boolean=false,connClosedEvent:()=>Unit=null) =
    new ClientSession(requestId,schema,clientChannelFactory,requestMode,sender,readOnly,connClosedEvent)
}

object StoreInstances {
  private val config = ConfigFactory.load()
  val rPoolSize = config.getInt("mevo.store.client.readSize")
  val wrPoolSize = config.getInt("mevo.store.client.writeSize")
  private val instances = new ConcurrentHashMap[String,StoreInstance]
  private val channelFactoryMap = new ConcurrentHashMap[String,ChannelFactory]
  private val instanceCount = new AtomicInteger(0)
  val servicePort = 9301
  def apply(appSpace:String):StoreInstance =
    if (instances.containsKey(appSpace))instances.get(appSpace)
    else {
      val ret = new StoreInstance(appSpace,rPoolSize,wrPoolSize,() =>
        Tcp.Connect(new InetSocketAddress(config.getString("mevo.store.host."+appSpace), servicePort),pullMode = false))
      instances.put(appSpace, ret)
      ret
    }
}