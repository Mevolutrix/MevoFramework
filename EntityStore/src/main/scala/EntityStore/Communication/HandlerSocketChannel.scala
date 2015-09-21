package EntityStore.Communication
import akka.actor._
import akka.util.ByteString
import SBEServiceAccess.ServiceConfig
import EntityStore.Interface.{Ack, ChannelFactory}
import akka.io.{IO, Tcp}
import akka.io.Tcp.Register
import java.util.concurrent.ConcurrentLinkedQueue
import HandlerSocket.Protocol.{HsResult, ResultDecoder}
import java.util

case class HandlerSocketCmd(request: ByteString,retProcessor:Session)
/**
 * Implement the low level Handler Socket protocol which can send command and receive result from MySQL bypassing
 * the SQL layer
 */
class HandlerSocketChannel(val channelId:Int,pool:ChannelFactory,connConfigSelector: (Boolean) => Tcp.Connect,
                           readOnly:Boolean=false) extends Actor with ActorLogging {
  import context.system
  private var clientSession : Session = _
  private var requestSender : ActorRef = _
  private var connection : ActorRef = _
  private var retryConnCounter = 0
  /**
   * Begin a new request, this channel must finish the previous request handling
   */
  private def initRequest(requestSession:Session,sender:ActorRef) = {
    if (!receiveBuffer.hasResult) {
      clientSession = requestSession
      requestSender = sender
      receiveBuffer.reset
    }
    else throw new Exception("Found request hasn't been finished on this channel. New request can't init this channel. Old Session:"+clientSession)
  }
  private def processResult(lines:Array[ByteString]):Unit = {
    if (lines==null) log.warning("HandlerSocket channel process result got null lines")
    else {
      val resultArr = new util.ArrayList[HsResult]()
      lines.foreach(line => resultArr.add(ResultDecoder.assembly(line)))
      requestSender ! resultArr
    }
  }
  private object receiveBuffer {
    private var storage:ByteString = _
    private val outQueue = new ConcurrentLinkedQueue[ByteString]()
    private var stored = 0
    private var transferred = 0
    private var cursor = 0

    var closing = false
    def hasLine: Boolean = {
      if (storage!=null) {
        val bs = storage.drop(cursor).takeWhile(char => char != '\n')
        cursor += bs.size
        cursor < storage.size
      }
      else false
    }
    def hasResult = outQueue.size()>0
    def finished = storage == null || ((storage.size-cursor)==1)
    def buffer(data: ByteString): Unit = {
      def append(data: ByteString): ByteString = if (storage == null) data else storage ++ data
      storage = append(data)
      stored += data.size
      while (storage!=null && hasLine) {
        outQueue.add(storage.take(cursor))
        storage = if(!finished) storage.drop(cursor+1) else null
        cursor = 0
      }
    }
    def acknowledge(collectResult:(Array[ByteString])=>Unit): Unit = {
      def transArraySize(transferredArray:Array[ByteString],left:Int):Int = {
        if (left<1) 0
        else transferredArray(left).size+1+transArraySize(transferredArray,left-1)
      }
      if (finished) collectResult(
        if (outQueue.size()>0) {
          val retArray = new Array[ByteString](outQueue.size())
          outQueue.toArray(retArray)
          outQueue.clear()
          transferred = transArraySize(retArray, retArray.length-1)
          retArray
        } else null)
    }
    def reset:Unit = {
      storage = null
      stored = 0
      outQueue.clear()
      transferred = 0
      cursor = 0
    }
  }

  override def preStart {
    val connectInfo = connConfigSelector(readOnly)
    IO(Tcp) !  connectInfo
  }
  override def postRestart(reason: Throwable): Unit = {
    log.error("Channel restarted.") //reason,
    // Call IO(Tcp) ! Connect, use the pool connection selector to remote service
    IO(Tcp) ! connConfigSelector(readOnly)
  }
  override def postStop() {
    log.info("close HandlerSocket channel:" + channelId)
  }
  def receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.error("Connection failed, reconnecting... retry:"+(retryConnCounter+1))
      if (retryConnCounter<3) {
        IO(Tcp) ! connConfigSelector(readOnly)
        retryConnCounter+=1
      }
      else {
        requestSender ! null  // Notify the session to close
        context stop self
      }
    case c @ Tcp.Connected(remote, local) =>
      log.info(s"Connection to [$remote].")
      connection = sender()
      connection ! Register(self) // Must be sent then this connection be available to send/receive
      context become {
        case HandlerSocketCmd(request,retProcessor) =>
          initRequest(retProcessor,sender)
          connection ! Tcp.Write(request)
        case Tcp.CommandFailed(w: Tcp.Write) =>
          log.error("O/S buffer was full")
        case Tcp.Received(data) =>
          receiveBuffer.buffer(data)
          self ! Ack // Notify self to get a chance to check whether there are lines
        case Ack =>
          if (receiveBuffer.hasResult) receiveBuffer.acknowledge(processResult)
        case "Close" =>
          log.info("Handler socket channel got Close command from:"+sender())
          if (clientSession != null) clientSession.notifyConnClosed
          connection ! Tcp.Close
        case _: Tcp.ConnectionClosed =>
          log.info("HS Channel connection closed. Id("+channelId+").")

          if (clientSession != null) clientSession.notifyConnClosed
          pool.disconnectChannel(channelId)
          context stop self
      }
    case req:HandlerSocketCmd =>
      self.tell(req,sender())
  }
}

class HSConnectionPool(connConfigSelector: (Boolean) => Tcp.Connect) extends ChannelFactory {
  override val readerPoolSize:Int=100
  override val writerPoolSize:Int=16
  val connectionEncoding:String="utf-8"

  override def buildNewChannel(readOnly:Boolean=false):Tuple2[Int,ActorRef] = {
    incPoolCount(readOnly)
    val channelID = getNewChannelId
    val channelActor = ServiceConfig.appSystem.actorOf(Props(new HandlerSocketChannel(channelID, this,connConfigSelector, readOnly)))
    (channelID, channelActor)
  }
  /*override def returnChannel(channel:(Int, ActorRef)):Unit = {
    if (channelMap.get(channel._1)._2) {
      readOnlyPool.add(channel)
    }
    else {
      channel._2 ! "Close"
      disconnectChannel(channel._1)
    }
  }*/
}
