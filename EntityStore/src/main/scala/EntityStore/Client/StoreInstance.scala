package EntityStore.Client
import akka.actor._
import akka.io.Tcp
import EntityInterface._
import java.nio.ByteBuffer
import EntityStore.Interface._
import EntityStore.Communication._
import java.net.InetSocketAddress
import EntityStore.Interface.ReqSendMode._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Store Instance manage client channel configuration (Own and implement the clientChannelFactory)
 */
class StoreInstance(val appSpaceName:String) {
  val token = StoreInstances.getInstanceToken(appSpaceName)
  def clientChannelFactory:ChannelFactory = StoreInstances.getClientConnFactory(token)
  // Todo: Complete the ClientSession new parameter
  def session(requestId:Int,schema:IEntitySchema=null,requestMode:ReqSendMode=asyncMode,
              sender:ActorRef=null,readOnly:Boolean=false,connClosedEvent:()=>Unit=null) =
    new ClientSession(requestId,schema,clientChannelFactory,requestMode,sender,readOnly,connClosedEvent)
}

object StoreInstances {
  private val instances = new ConcurrentHashMap[String,StoreInstance]
  private val tokenMap = new ConcurrentHashMap[String,Int]
  private val channelFactoryMap = new ConcurrentHashMap[Int,ChannelFactory]
  private val instanceCount = new AtomicInteger(0)
  private val config = ConfigFactory.load("mevolutrix")
  private def buildNewPool(appSpace:String,token:Int):ChannelFactory = {
    val ret = new StoreClientChannelPool(rPoolSize,wrPoolSize,() =>
      Tcp.Connect(new InetSocketAddress(config.getString("mevo.store.host."+appSpace), 9301),pullMode = false))
    channelFactoryMap.put(token, ret)
  }
  val rPoolSize = config.getInt("mevo.store.client.readSize")
  val wrPoolSize = config.getInt("mevo.store.client.writeSize")
  def apply(appSpace:String):StoreInstance =
    if (instances.containsKey(appSpace))instances.get(appSpace)
    else {
      val ret = new StoreInstance(appSpace)
      instances.put(appSpace, ret)
      ret
    }
  def getInstanceToken(appSpace:String) =
    if(tokenMap.containsKey(appSpace)) tokenMap.get(appSpace)
    else {
      val ret = getAppSpaceToken(appSpace)
      tokenMap.put(appSpace, ret)
      ret
    }
  def getAppSpaceToken(appSpace:String):Int =
    if (tokenMap.containsKey(appSpace)) tokenMap.get(appSpace)
    else {
      val token = instanceCount.incrementAndGet()
      tokenMap.put(appSpace,token)
      buildNewPool(appSpace,token)
      token
    }
  def getClientConnFactory(token:Int):ChannelFactory = channelFactoryMap.get(token)
}