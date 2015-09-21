package EntityStore.Connection
import akka.io.Tcp
import java.net.InetSocketAddress
import akka.actor.{ActorRef, Props}
import EntityInterface.IEntitySchema
import EntityStore.Client.ClientSession
import com.typesafe.config.ConfigFactory
import EntityStore.Interface.ReqSendMode._
import EntityStore.Interface.ChannelFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import EntityStore.Communication.StoreClientChannel

class SAEInstance(val appSpaceName:String,rPoolSize:Int,wrPoolSize:Int,connSelector:()=>Tcp.Connect) {
  //val token = SAEInstances.getInstanceToken(appSpaceName)
  private val clientChannelFactory:ChannelFactory =
    new SaeClientChannelPool(rPoolSize,wrPoolSize,connSelector)
  def session(requestId:Int,schema:IEntitySchema=null,requestMode:ReqSendMode=asyncMode,
              sender:ActorRef=null,readOnly:Boolean=false,connClosedEvent:()=>Unit=null) =
    new ClientSession(requestId,schema,clientChannelFactory,requestMode,sender,readOnly,connClosedEvent)
}

object SAEInstances {
  val servicePort = 9302
  private val instances = new ConcurrentHashMap[String,SAEInstance]
  //private val tokenMap = new ConcurrentHashMap[String,Int]
  private val channelFactoryMap = new ConcurrentHashMap[Int,ChannelFactory]
  private val instanceCount = new AtomicInteger(0)
  private val config = ConfigFactory.load()
  val rPoolSize = config.getInt("mevo.sae.client.readSize")
  val wrPoolSize = config.getInt("mevo.sae.client.writeSize")
  def apply(appSpace:String):SAEInstance =
    if (instances.containsKey(appSpace))instances.get(appSpace)
    else Option(instances.putIfAbsent(appSpace,new SAEInstance(appSpace,rPoolSize,wrPoolSize,
      () => Tcp.Connect(new InetSocketAddress(config.getString("mevo.sae.host."+appSpace),servicePort),
      pullMode = false)))).getOrElse(instances.get(appSpace))
}
/**
 * Channel pool for one instance, "ConnConfigSelector" should be provided here to let the Channels connect to proper remote host
 */
class SaeClientChannelPool(rPoolSize:Int,wrPoolSize:Int,connConfigSelector: () => Tcp.Connect) extends ChannelFactory {
import SBEServiceAccess._
  override val readerPoolSize:Int = rPoolSize
  override val writerPoolSize:Int = wrPoolSize
  override def buildNewChannel(readOnly:Boolean=false):Tuple2[Int,ActorRef] = {
    if (readOnly) throw new java.lang.IllegalArgumentException("This store has no read only support.")
    val channelID = getNewChannelId
    val channelActor = ServiceConfig.appSystem.actorOf(Props(new StoreClientChannel(channelID, connConfigSelector)))
    (channelID, channelActor)
  }
}