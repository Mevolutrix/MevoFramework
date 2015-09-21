package EntityStore.Communication

import EntityStore.Connection.StoreInstances
import akka.actor._
import akka.pattern._
import akka.io.{Tcp, IO}
import akka.util.{Timeout, ByteString}
import EntityStore.Interface._
import java.net.InetSocketAddress
import java.util.concurrent.{TimeUnit, ConcurrentLinkedQueue, ConcurrentHashMap}
import EntityInterface.{IEntitySchema, IEntityRandomAccess}
import java.nio.ByteBuffer
import EntityAccess._
import CSON.CSONDocument
import EntityStore.Interface.AckOutput
import EntityStore.Interface.RequestTask

import scala.concurrent.Await

class EntityStoreServer(storeProcessor:ServiceOperationExecutor,handlerClass: Class[_],
                        val servicePort: Int = StoreInstances.servicePort) extends Actor with ActorLogging {
  import context.system
  private var isStart = false
  // restart Store Server which waiting on 9301
  override val supervisorStrategy = SupervisorStrategy.defaultStrategy

  // bind to the listen port; the port will automatically be closed once this actor dies
  override def preStart(): Unit = {
    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(EntityStoreServer.hostName, servicePort))
  }

  // do restart and waiting on the port
  override def postRestart(thr: Throwable): Unit = {
    log.error("Actor recovery. try to waiting on service port:"+servicePort)
    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(EntityStoreServer.hostName, servicePort))
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
      val handler = context.actorOf(Props(handlerClass, storeProcessor, connection))
      connection ! Tcp.Register(handler)
    case "Close" =>
      log.info("Close the server, stop waiting.")
      IO(Tcp) ! Tcp.Unbind
      context stop self
    case "isStarted" => sender()! isStart
    case command@_ => log.info("Unknown command:" + command)
  }
}
class ServiceRequestHandler(storeProcessor:ServiceOperationExecutor, connection: ActorRef) extends Actor with ActorLogging {
  import Tcp._

  // wrap the ack temp buffer data structure and operation
  private object OutputBuffer {
    private val storage:ConcurrentLinkedQueue[OutputBuffer] = new ConcurrentLinkedQueue[OutputBuffer]()
    private var stored = 0L
    private var transferred = 0L
    private val maxStored = 100000000L
    private val highWatermark = maxStored * 5 / 10
    private val lowWatermark = maxStored * 3 / 10
    private var suspended = false

    var closing = false
    def buffer(data: OutputBuffer): Unit = {
      storage.add(data)
      stored += data.getSize

      /*if (stored > maxStored) log.warning("too many data to client (buffer overrun)")
      else if (stored > highWatermark) {
        log.info(s"suspending reading")
        RequestTaskProcessor.getContext(data).handleCommand(Tcp.SuspendReading)
        suspended = true
      }*/
    }
    def acknowledge(providerActor:ActorRef): Unit = {
      // The first element in queue was already sent to client side, just remove them and mark as transferred
      val data2Send = storage.poll()
      val size = data2Send.getSize
      stored -= size
      transferred += size

      if (suspended && stored < lowWatermark) {
        log.info("resuming reading")
        //RequestTaskProcessor.getContext(self).handleCommand(Tcp.ResumeReading)
        suspended = false
      }

      if (storage.isEmpty) {
        if (closing) closeConnection()
        else context.unbecome()
      } else {
        val data = storage.peek()
        connection ! Tcp.Write(data.getResult ,Ack)
        if (!data.isFinished) {
          providerActor ! EntityStoreServer.getAckOutput(data2Send)
        }
      }
    }
  }
  private def processRequest(clientChannelActor:ActorRef,buffer:ByteString) = {
    // Now we only have MySQL Store so only HandlerSocket RequestProcessor will receive
    storeProcessor.reqProcessor("") ! RequestTask(storeProcessor.decodeMessage(buffer),
                                    clientChannelActor,EntityStoreServer.getAckOutput())
  }
  // sign death pact: this actor terminates when connection breaks
  context watch connection

  override def preStart(): Unit = EntityStoreServer.newClientChannel(self)
  // do not restart
  override def postRestart(thr: Throwable): Unit = {
    log.info("Actor recovery. Do not restart(stop self).")
    context stop self
  }
  private def closeConnection():Unit = {
    EntityStoreServer tryRemoveChannel self
    context stop self // Todo: clean the processing work in storeProcessor
  }
  def receive = {
    case Received(data) => processRequest(self,data)
    case result: OutputBuffer =>
      val qeActor = sender()
      // If the buffer queue only have one data buffered, it will be polled and drop
      OutputBuffer.buffer(result)
      connection ! Tcp.Write(result.getResult(),Ack)
      if (!result.isFinished) qeActor ! EntityStoreServer.getAckOutput()
      context.become({
        case Received(data) => processRequest(self,data)
        case result: OutputBuffer =>
          OutputBuffer.buffer(result)
        // unbecome will be called in AckBuffer.acknowledge.
        //Got Ack from client side channel, if there are multiple data trigger the
        // queryProcessor Actor to use Context return next slice of data.
        case Ack            =>
          // acknowledge will write to TCP channel(connection param) and if not finished
          // ack ping the backend actor to move next
          OutputBuffer.acknowledge(storeProcessor.reqProcessor(""))
                                                        //buffered, the second one will be sent (First one)
        case "Close"       => OutputBuffer.closing = true
        case PeerClosed     => OutputBuffer.closing = true
      }, discardOld = false)
    case "Close" => closeConnection()
    case PeerClosed => closeConnection()
    case msg@_ => log.info("ServiceRequestHandler got unkonwn message.->"+msg)
  }
}

/**
 * Usage: Start the store server by calling EntityStoreServer.start(<store processor instance:StoreOperationExecutor>) 
 */
object EntityStoreServer {
import SBEServiceAccess._
  val serverSystem = ServiceConfig.appSystem
  val REQUEST_FINISH_TIMEOUT = 300000 // 30 seconds to time out
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  val hostName = "0.0.0.0"
  private var storeProcessor: ServiceOperationExecutor = _
  private var storeServer: ActorRef = _
  private val channelMap = new ConcurrentHashMap[ActorRef,Int]
  /**
   * Compose the proper Store Operation processor object and use it to start the store server
   * @param opExecutor Store Query Executor routing actor which need to implement StoreOperationExecutor interface
   * @param handlerClass ClassOf[StoreServiceChannelHanlderClass]. class ServiceRequestHandler can be used as generic
   *                     handler. It can accept both JSON/CSON list result and need client support throtting read.
   */
  def start(opExecutor:ServiceOperationExecutor,handlerClass: Class[_]=classOf[ServiceRequestHandler]) = {
    storeServer = serverSystem.actorOf(Props(new EntityStoreServer(opExecutor,handlerClass))) // Todo: read configuration for service port
    storeProcessor = opExecutor
    var future = storeServer.ask("isStarted")
    while(!(Await.result(future,timeout.duration).asInstanceOf[Boolean])){
      future = storeServer.ask("isStarted")
    }
  }
  def stop = {
    shutdown
    serverSystem.stop(storeServer)
  }

  def getAckOutput(ob:OutputBuffer=new OutputBuffer()) = {
    AckOutput(ob,ResultFormat.outputWriter)
  }
  def newClientChannel(clientActor: ActorRef) : Unit = channelMap.put(clientActor, 0)
  def tryRemoveChannel(clientActor: ActorRef) : Unit =
    if (channelMap.containsKey(clientActor)) channelMap.remove(clientActor)

  private def shutdown = {
    val channels = channelMap.keys()
    while (channels.hasMoreElements() ) { 
      val clientSession = channels.nextElement
      storeProcessor.terminateRequest(clientSession)
      clientSession! "Close"
    }
    channelMap.clear()
    storeServer ! "Close"
  }
}