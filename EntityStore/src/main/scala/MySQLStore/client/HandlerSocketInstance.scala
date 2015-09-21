package MySQLStore.client
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import EntityStore.Metadata.MetadataManager
import HandlerSocket.Protocol.OpenIndexSpec
import EntityStore.Interface.ChannelFactory
import EntityStore.Communication._
import akka.io.Tcp
import java.net.InetSocketAddress
import EntityInterface.IEntitySchema
import java.util

/**
 * Currently MySQL instance connected with HS protocol will be seperated by "AppSpace"
 * Todo: choose a better strategy and implement the configuration for seperating dbs
 */
class HandlerSocketInstance(val appSpaceName:String) {
  private val indexCount = new AtomicInteger(0)
  private val channelIdxSpecMap = new ConcurrentHashMap[Int,util.HashMap[(String,String,String),Int]]()
  private val setIdxSpecCache = new ConcurrentHashMap[String,OpenIndexSpec]()
  private val clientChannelFactory:ChannelFactory = HandlerSocketInstances.getClientConnFactory(appSpaceName)

  /**
   * lookup the old opened Index OpenSpec id from cache, if not yet opened, create a new Id and indicate to open it
   * @param channelId
   * @param indexSpec
   * @return idx Id for OpenSpec, true for need call HandlerSocket request to open IndexSpec
   */
  def getIndexId(channelId:Int,indexSpec:OpenIndexSpec):(Int,Boolean) = {
    var idxSpecMap = channelIdxSpecMap.get(channelId)
    val idxKey = (indexSpec.db,indexSpec.table,indexSpec.columns.mkString("",",",""))
    if (idxSpecMap!=null) idxSpecMap.get(indexSpec)
    else {
      idxSpecMap = new util.HashMap[(String,String,String), Int]()
      channelIdxSpecMap.put(channelId, idxSpecMap)
    }
    if (idxSpecMap.containsKey(idxKey))
      (idxSpecMap.get(idxKey),false)
    else{
      val ret = indexCount.incrementAndGet()
      idxSpecMap.put(idxKey,ret)
      (ret,true)
    }
  }
  def getIdxSpec(setName:String,buildNew:(String)=>OpenIndexSpec):OpenIndexSpec = {
    var ret = setIdxSpecCache.get(setName)
    if (ret!=null) ret else {ret = buildNew(setName);setIdxSpecCache.put(setName,ret);ret}
  }
  def session(requestId:Int,readOnly:Boolean=true,schema:IEntitySchema=null,connClosedEvent:()=>Unit=null) =
    new HsClientSession(requestId,schema,readOnly,this,clientChannelFactory,connClosedEvent)
}
object HandlerSocketInstances {
  val port4r:Int=9998
  val port4wr:Int=9999
  private val instances = new ConcurrentHashMap[String,HandlerSocketInstance]
  private val connPools = new ConcurrentHashMap[String,HSConnectionPool]
  def apply(appSpace:String):HandlerSocketInstance =
    if (instances.containsKey(appSpace))instances.get(appSpace)
    else {
      val ret = new HandlerSocketInstance(appSpace)
      instances.put(appSpace, ret)
      ret
    }
  def getClientConnFactory(appSpaceName:String):ChannelFactory = {
    var ret = connPools.get(appSpaceName)
    if (ret==null) {
      ret = new HSConnectionPool((readOnly) => Tcp.Connect(new InetSocketAddress(
        MetadataManager.getAppSpace(appSpaceName).storeConfig.hostName,
        if (readOnly) port4r else port4wr), pullMode = false))
      connPools.put(appSpaceName, ret)
    }
    ret
  }
}