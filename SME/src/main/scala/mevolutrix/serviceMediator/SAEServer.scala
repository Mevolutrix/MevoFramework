package mevolutrix.serviceMediator

import EntityStore.Connection.SAEInstances
import akka.actor._
import akka.pattern._
import akka.io.{Tcp, IO}
import scala.concurrent.Await
import EntityStore.Interface._
import java.net.InetSocketAddress
import akka.util.{ByteString, Timeout}
import mevolutrix.Interface.{SAE_GetReq, SAE_PostReq}
import java.util.concurrent.{TimeUnit, ConcurrentHashMap}

class SAEServer(saeProcessor:ServiceOperationExecutor,handlerClass: Class[_],
                 val servicePort: Int = SAEInstances.servicePort) extends Actor with ActorLogging {
  import context.system
  private var isStart = false
  // restart Store Server which waiting on 9302
  override val supervisorStrategy = SupervisorStrategy.defaultStrategy

  // bind to the listen port; the port will automatically be closed once this actor dies
  override def preStart(): Unit = {
    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(SAEServer.hostName, servicePort))
  }

  // do restart and waiting on the port
  override def postRestart(thr: Throwable): Unit = {
    log.error("Actor recovery. try to waiting on service port:"+servicePort)
    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(SAEServer.hostName, servicePort))
  }

  // Note: Server can save the socket handle with the IO.Read event buffer to query processor actor
  //       then query processor actor can use socket.asSocket write resultBuffer to write result to client channel.
  def receive = {
    case b@Tcp.Bound(localAddress) =>
      log.info("The server is listening on socket " + localAddress)
      isStart = true
    case Tcp.CommandFailed(_: Tcp.Bind) =>
      log.error("Binding to service failed, it may be caused by the port already opened for another server.")
      context stop self
    case c@Tcp.Connected(remote, local) =>
      log.info("Incoming connection from: " + remote + " to local: " + local)
      val connection = sender()
      val handler = context.actorOf(Props(handlerClass, saeProcessor, connection))
      connection ! Tcp.Register(handler)
    case "Close" =>
      log.info("Close the server, stop waiting.")
      IO(Tcp) ! Tcp.Unbind
      context stop self
    case "isStarted" => sender()! isStart
    case command@_ => log.info("Unknown command:" + command)
  }
}
class ServiceRequestHandler(reqProcessor:ServiceOperationExecutor, connection: ActorRef) extends Actor with ActorLogging {
  import Tcp._

  private def processRequest(clientChannelActor:ActorRef,buffer:ByteString) = {
    // we support SME/SVC service request
    val req = reqProcessor.decodeMessage(buffer)
    val isSvc = !req.getValue("isSMECall").asInstanceOf[Boolean]
    val appSpace = req.getValue("appSpace").asInstanceOf[String]
    val isPostCall = req.getValue("isPostCall").asInstanceOf[Boolean]
    val funcName = req.getValue("funcName").asInstanceOf[String]
    val pStr = req.getValue("params").asInstanceOf[String]
    val data = req.getValue("data").asInstanceOf[String]
    val reqMsg = if (isPostCall) SAE_PostReq(appSpace,funcName,pStr,data,isSvc)
    else SAE_GetReq(appSpace,funcName,pStr,isSvc)

    reqProcessor.reqProcessor(appSpace) ! reqMsg
  }
  // sign death pact: this actor terminates when connection breaks
  context watch connection

  override def preStart(): Unit = SAEServer.newClientChannel(self)
  // do not restart
  override def postRestart(thr: Throwable): Unit = {
    log.info("Actor recovery. Do not restart(stop self).")
    context stop self
  }
  private def closeConnection():Unit = {
    SAEServer tryRemoveChannel self
    context stop self
  }
  def receive = {
    case Received(data) => processRequest(self,data)
    case result: ByteString => connection ! Tcp.Write(result)
    case "Close" => closeConnection()
    case PeerClosed => closeConnection()
    case msg@_ => log.info("SAEServiceHandler got unkonwn message.->"+msg)
  }
}
/**
 * Usage: Start the store server by calling EntityStoreServer.start(<store processor instance:StoreOperationExecutor>)
 */
object SAEServer {
  import SBEServiceAccess._
  val serverSystem = ServiceConfig.appSystem
 // val REQUEST_FINISH_TIMEOUT = 300000 // 30 seconds to time out
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  val hostName = "0.0.0.0"
  private var reqProcessor: ServiceOperationExecutor = _
  private var saeServer: ActorRef = _
  private val channelMap = new ConcurrentHashMap[ActorRef,Int]
  /**
   * Compose the proper Store Operation processor object and use it to start the store server
   * @param opExecutor Store Query Executor routing actor which need to implement StoreOperationExecutor interface
   * @param handlerClass ClassOf[StoreServiceChannelHanlderClass]. class ServiceRequestHandler can be used as generic
   *                     handler. It can accept both JSON/CSON list result and need client support throtting read.
   */
  def start(opExecutor:ServiceOperationExecutor,handlerClass: Class[_]=classOf[ServiceRequestHandler]) = {
    saeServer = serverSystem.actorOf(Props(new SAEServer(opExecutor,handlerClass))) // Todo: read configuration for service port
    reqProcessor = opExecutor
    var future = saeServer.ask("isStarted")//(REQUEST_FINISH_TIMEOUT)
    while(!(Await.result(future,timeout.duration).asInstanceOf[Boolean])){
      future = saeServer.ask("isStarted")//(REQUEST_FINISH_TIMEOUT)
    }
  }
  def stop = {
    shutdown
    serverSystem.stop(saeServer)
  }

  def newClientChannel(clientActor: ActorRef): Unit = channelMap.put(clientActor, 0)
  def tryRemoveChannel(clientActor: ActorRef) : Unit =
    if (channelMap.containsKey(clientActor)) channelMap.remove(clientActor)

  private def shutdown = {
    val channels = channelMap.keys()
    while (channels.hasMoreElements() ) {
      val clientSession = channels.nextElement
      reqProcessor.terminateRequest(clientSession)
      clientSession! "Close"
    }
    channelMap.clear()
    saeServer ! "Close"
  }
}