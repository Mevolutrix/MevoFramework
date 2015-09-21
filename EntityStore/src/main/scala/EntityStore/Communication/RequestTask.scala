package EntityStore.Communication
import CSON.CSONDocument
import akka.io.Tcp
import akka.actor._
import EntityAccess._
import SBEServiceAccess._
import EntityStore.Interface._
import EntityInterface.IEntityRandomAccess
import akka.routing.{ RoundRobinRouter, DefaultResizer }

/**
 * Default Store request processor which will print the request and return the fixed result list for debug
 * Store Engine builder should copy this template and create their own StoreOperation Actor and child Object of StoreOperationExecutor
 */
class RequestTaskProcessor() extends Actor with ActorLogging{

  override def preStart {
  }
  
  override def postStop {
  }
  class RequestProcessContext(val reqMessage:IEntityRandomAccess) extends SessionContext {
    //def handleCommand(opCmd:Tcp.Command):Unit = {}
    def dispose:Unit = {}
    def fillResult(outputWriter:(Int,Long,Int,IndexedSeq[IEntityRandomAccess])=>Unit,updateFinish:(Boolean)=>Unit) = {}
    val csonList = new Array[IEntityRandomAccess](2)
    val storedSchema = GeneralEntitySchema(classOf[JSONRecord])
    //val jsonString = JSONSerializer(new ResultSchema(storedSchema))
    //val schemaDocSerializer = GeneralEntityToCSON(classOf[ResultSchema])
    //val jsonList = new Array[IEntityRandomAccess](2)
    for (i<-0 until 2) {
      csonList(i)=(GeneralEntityToCSON.serializeObject(new ResultSchema(storedSchema),null)._1)
      //jsonList(i)=(GeneralEntityToCSON.serializeObject(new JSONRecord(jsonString),null)._1)
    }
    var replyCount = 1
    def isFinished:Boolean = true
  }
  def receive = {
    case RequestTask(request, channelActor, ack) =>
      val rpContext = new RequestProcessContext(request)
      RequestTaskProcessor.registerRequest(ack.ob, rpContext)
      ack.outOperator(ack.ob,0,0L,rpContext.csonList.length,rpContext.csonList)
      sender() ! ack.ob
    case cmd:Tcp.Command =>
      log.info("Executor handleCommand:"+cmd)
      //RequestTaskProcessor.getContext(sender()).handleCommand(cmd)
    // When context was set with suspend command, ignore the Ack event
    case AckOutput(ob,outputWriter) =>
      val sessionActor = sender()
      val rpContext = RequestTaskProcessor.getContext(ob).asInstanceOf[RequestProcessContext]
      rpContext.replyCount -=1
      ob.finishOutput(rpContext.replyCount<=0)
      outputWriter(ob,0,0L,rpContext.csonList.length,rpContext.csonList)
      sessionActor ! ob
      if (rpContext.replyCount<=0) {
        log.debug("Store Executor moved next to finished:"+rpContext.replyCount)
        RequestTaskProcessor.finishContext(ob)
      }
    case _ => log.error("RequestTaskProcessor received unknown command")
  }
}
/**
 * Create request task handling router number based on CPU core number
 */
object RequestTaskProcessor extends ServiceOperationExecutor {
  def system: ActorSystem = ServiceConfig.appSystem 
  def reqProcessor(alias:String):ActorRef = innerProcessorPool
  val minSzie = ServiceConfig.conf.getInt("mevo.store.reqTaskProcessor.minSize")
  val maxSize = ServiceConfig.conf.getInt("mevo.store.reqTaskProcessor.maxSize")
  /**
   *  Demo for creating resizable store processor actors pool
   */ 
  private lazy val innerProcessorPool = system.actorOf(Props(new RequestTaskProcessor).withRouter(RoundRobinRouter(
                                        resizer = Some(DefaultResizer(lowerBound = minSzie, upperBound = maxSize)))))
  def obj2String(prompt:String,value:AnyRef) = prompt+JSONSerializer(value)
}