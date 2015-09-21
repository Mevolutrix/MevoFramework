package MySQLStore.operationExecutor
import akka.io.Tcp
import akka.actor._
import EntityAccess._
import SBEServiceAccess._
import EntityStore.Interface._
import akka.routing.{ RoundRobinRouter, DefaultResizer }
import MySQLStore.queryProcessor.RequestProcessContext

/**
 * Default Store request processor which will print the request and return the fixed result list for debug
 * Store Engine builder should copy this template and create their own StoreOperation Actor and child Object of StoreOperationExecutor
 */
class HsTaskProcessor() extends Actor with ActorLogging{

  override def preStart {
  }

  def receive = {
    // Got this request from ServiceRequestHandler.processRequest
    // The Handler will call when it received TCP request from SBE
    // channelActor is the ActorRef which received this TCP call
    case RequestTask(request, channelActor, ack) =>
      val rpContext = new RequestProcessContext(request)
      HsTaskProcessor.registerRequest(ack.ob, rpContext)
      rpContext.fillResult(ack.outOperator(ack.ob,_,_,_,_),ack.ob.finishOutput(_))
      sender() ! ack.ob
      if (rpContext.isFinished) HsTaskProcessor.finishContext(ack.ob)
    // When context was set with suspend command, ignore the Ack event
    case AckOutput(ob,outputWriter) =>
      val sessionActor = sender()
      val rpContext = HsTaskProcessor.getContext(ob).asInstanceOf[RequestProcessContext]
      ob.finishOutput(rpContext.isFinished)
      if (rpContext.isFinished)
        HsTaskProcessor.finishContext(ob)
      else {
        //
        rpContext.fillResult(outputWriter(ob,_,_,_,_),ob.finishOutput(_))
        sessionActor ! ob
      }
    case _ => log.error("RequestTaskProcessor received unknown command")
  }
}
/**
 * Create request task handling router number based on CPU core number
 */
object HsTaskProcessor extends ServiceOperationExecutor {
  override def system: ActorSystem = ServiceConfig.appSystem
  override def reqProcessor(alias:String):ActorRef = innerProcessorPool
  val minSize = ServiceConfig.conf.getInt("mevo.store.reqTaskProcessor.minSize")
  val maxSize = ServiceConfig.conf.getInt("mevo.store.reqTaskProcessor.maxSize")
  /**
   *  Resizable store processor actors pool, TODO: add config for these pool size
   */
  private lazy val innerProcessorPool = system.actorOf(Props(new HsTaskProcessor).withRouter(RoundRobinRouter(
    resizer = Some(DefaultResizer(lowerBound = minSize, upperBound = maxSize)))))
}
