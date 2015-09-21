package mevolutrix.serviceMediator
import java.nio.{ByteOrder, ByteBuffer}
import java.util
import java.util.concurrent.ConcurrentHashMap
import CSON.CSONDocument
import EntityAccess.{DynamicSchema, GeneralEntityToCSON, GeneralEntitySchema}
import EntityInterface.{IEntitySchema, IEntityRandomAccess}
import EntityStore.Interface._
import EntityStore.Metadata.MetadataManager
import SBEServiceAccess.ServiceConfig
import SVCInterface.ISvcHandler
import akka.actor._
import akka.routing.{DefaultResizer, RoundRobinRouter}
import akka.util.ByteString
import mevolutrix.Interface._
import mevolutrix.ServiceRT.{ServiceHelper, SVCHandler}

/**
 * Default Store request processor which will print the request and return the fixed result list for debug
 * Store Engine builder should copy this template and create their own StoreOperation Actor and child Object of StoreOperationExecutor
 */
class SAEReqTaskProcessor() extends Actor with ActorLogging{
//  var appSpace:String = null
  var smeSvc:ISmeHandler = null
  var svc:ISvcHandler = null
  override def preStart {
  }

  override def postStop {
  }
  private def getParamMap(pStr:String):Map[String,String] = if (pStr!=null && pStr.contains(" -> ")) {
      val ret = for (item <- pStr.split(",")) yield {
        try {val t = item.split(" -> "); (t(0), t(1))}
        catch {
          case e:Exception => throw new IllegalArgumentException("Got error with param map:("+item+")")
        }
      }
      ret.toMap[String, String]
  }else {
    println("Found empty param String or wrong format:"+ (if(pStr!=null)pStr else "|null"))
    null}
  private def getParamJMap(pStr:String):util.Map[String,String] = if (pStr!=null) {
    val ret = new util.HashMap[String,String]()
    for (item<-pStr.split(",")) {
      val t=item.split(" -> ")
      if (t.length>1) ret.put(t(0),t(1))
    }
    ret
  }else null
  def receive = {
    case SAE_GetReq(appSpace,funcName,params,isSvc) =>
      val ret = if (isSvc) {
        if (svc==null) svc = SVCHandler(MetadataManager.getAppSpace(appSpace).alias)
        svc.getRequest(funcName,getParamJMap(params),ServiceHelper(appSpace))
      } else {
        if (smeSvc==null) smeSvc = SMEHandler(appSpace)
        smeSvc.getRequest(funcName,getParamMap(params))
      }
      sender() ! ResultFormat.writeResult(ret)
    case SAE_PostReq(appSpace,funcName,params,postData,isSvc) =>
      val ret = if (isSvc) {
        if (svc==null) svc = SVCHandler(MetadataManager.getAppSpace(appSpace).alias)
        svc.postRequest(funcName,getParamJMap(params),postData,ServiceHelper(appSpace))
      } else {
        if (smeSvc==null) smeSvc = SMEHandler(appSpace)
        smeSvc.postRequest(funcName,getParamMap(params),postData)
      }
      sender() ! ResultFormat.writeResult(ret)
    case _ => log.error("RequestTaskProcessor received unknown command")
  }
}
/**
 * Create request task handling router number based on CPU core number
 */
object SAEReqTaskProcessor extends ServiceOperationExecutor {
  override val reqMsgSerializer = GeneralEntityToCSON(classOf[SAEReqMsg])
  override val reqMsgSchema = EntityAccess.GeneralEntitySchema(classOf[SAEReqMsg])
  val maxSize = ServiceConfig.conf.getInt("mevo.sae.reqTaskProcessor.maxSize")
  def system: ActorSystem = ServiceConfig.appSystem
  def reqProcessor(appSpace:String):ActorRef =
    if (innerProcessorMap.containsKey(appSpace))
      innerProcessorMap.get(appSpace)
    /**
     *  putIfAbsent will creating resizable store processor actors pool for each appSpace
     *  and put into the map if no alias existed. Then use getOrElse to return the new created actor pool
     */
    else Option(innerProcessorMap.putIfAbsent(appSpace,system.actorOf(Props(new SAEReqTaskProcessor)
      .withRouter(RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = maxSize)))))))
      .getOrElse(innerProcessorMap.get(appSpace))
  private lazy val innerProcessorMap = new ConcurrentHashMap[String,ActorRef]()
/*  override def decodeMessage(reqBuf: ByteString): IEntityRandomAccess =
    new CSONDocument(reqMsgSchema, Option(reqBuf.asByteBuffer))
  override def convertMsg(reqMsg: IEntityRandomAccess): SMEReqMsg =
    reqMsgSerializer.getObject(reqMsg.asInstanceOf[CSONDocument]).asInstanceOf[SMEReqMsg]
*/
}