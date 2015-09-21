package MySQLStore.client
import java.util
import akka.pattern.ask
import akka.actor.ActorRef
import EntityStore.Interface._
import HandlerSocket.Protocol._
import EntityInterface.IEntitySchema
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import scala.concurrent.{Await, Future}
import EntityStore.Communication.Session
import EntityStore.Interface.ReqSendMode._
import HandlerSocket.Protocol.OpenIndexSpec
import EntityStore.Communication.HandlerSocketCmd
import akka.util.{Timeout, ByteStringBuilder, ByteString}

/**
 * Handler Socket session which handling the store operation command map to protocol byteString generation
 */
class HsClientSession(requestId:Int, returnSchema:IEntitySchema, readOnly:Boolean=true,
                      instance:HandlerSocketInstance, clientChannelFactory: ChannelFactory=null,
                      connClosedEvent:()=>Unit=null) extends
                      Session(requestId,returnSchema,clientChannelFactory,connClosedEvent){
  //#private members
  implicit val timeout = Timeout(15,TimeUnit.SECONDS)
  clientChannel =  clientChannelFactory.getChannel(readOnly)
  private var syncFuture: Future[Any] = _

  private def callHsService(idx:Int,cmd:HsCommand):util.ArrayList[HsResult] = {
    val target = new ByteStringBuilder()
    val cmdEncoder:CommandEncoder = new CommandEncoder(target)
    cmd.toCommand(idx,cmdEncoder)
    syncFuture = sendRequest(target.result())
    waitForResult.asInstanceOf[util.ArrayList[HsResult]]
  }
  def executeCommands(openIndex:OpenIndexSpec, cmds:IndexedSeq[HsCommand],jsStmts:(String,String,Array[String])=null): AnyRef = {
    val resultArr = new util.ArrayList[Row]()
    val idxID@(idx, needOpen) = instance.getIndexId(clientChannel._1, openIndex)
    if (needOpen) {
      val opIdxStatus = callHsService(idx, openIndex)
      if (opIdxStatus == null || opIdxStatus.size() < 1) return (-1, "ConnectionFail")
      else {
        val opIdxRet = opIdxStatus.get(0)
        if (opIdxRet.errorCode != 0) return (opIdxRet.errorCode,
          if (opIdxRet.columns.length > 0) opIdxRet.columns(0) else "None")
      }
    }
    for (n <- 0 until cmds.length) {
      val hsCollection = callHsService(idx, cmds(n))
      for (i <- 0 until hsCollection.size()) {
        val ret = hsCollection.get(i)
        if (ret.errorCode == 0) resultArr.addAll(ret.iterator())
        else {
          return (ret.errorCode, if (ret.columns.length > 0) ret.columns(0) else "None")
        }
      }
    }
    resultArr
  }
  //#private members

  //#Session Implementation
  def sendRequest(request:ByteString,requestMode:ReqSendMode=syncMode,sender:ActorRef=null):Future[Any] =
    clientChannel._2.ask(HandlerSocketCmd(request,this))//(REQUEST_FINISH_TIMEOUT)
  def processResult(data: ByteString): Unit = {}
  //#Session Implementation

  def waitForResult:Any = {
    Await.result(syncFuture,timeout.duration)
  }
}
object HsClientAccessor {
  val log = LoggerFactory.getLogger("HS Client")
  def getSession(appSpaceName:String,requestId:Int,readOnly:Boolean=true,
                 schema:IEntitySchema=null,connClosedEvent:()=>Unit=null) =
    HandlerSocketInstances(appSpaceName).session(requestId,readOnly,schema,connClosedEvent)
}