package EntityStore.Communication
import akka.actor._
import akka.io.{IO, Tcp}
import java.nio.ByteOrder
import akka.io.Tcp.Register
import akka.util.ByteString
import EntityStore.Interface.ChannelFactory

/**
 * client communication channel to EntityStore Servers(There will be distributed cache server,
 * proxy server(reroute the request to MySQL through Handler Socket layer),
 * mixed server(both of cache and agent of MySQL+HS)
 * It will base on an assumption: One Request will be a session(When a StoreCmd received, it will be reinitialized)
 * Notice: session can't send multiple StoreCmd before the previous result handling finished
 */
class StoreClientChannel(val channelId:Int,connConfigSelector: () => Tcp.Connect) extends Actor with ActorLogging{
import context.system

  private var clientSession : Session = _
  private var connection : ActorRef = _

  /**
   * Begin a new request, this channel must finish the previous request handling
   */
  private def initRequest(requestSession:Session,reqSender:ActorRef) = {
    if (clientSession!=null || dataBlockLength>=0)
      log.error("Found request hasn't been finished on this channel.Old Session:"+clientSession)
    //log.info("Init req channel("+channelId+")-"+requestSession)
    clientSession = requestSession
    clientSession.initRequest(reqSender)
  }
  /**
   * Clear the context of request handling
   * The protocol is that every session with ONLY one request. Afater this request got result returned with finish flag, this session was cleanned from this channel
   * So another session can get this channel and send one request
   */
  private def finishRequest = {
    clientSession = null
  }
  // loop variable to store the data message and concrete to the processible buffer (Server write the accurate length in the begin of data block)
  private var resultData:ByteString = ByteString.empty
  private var dataBlockLength = -1
  /**
   * Called in channel Read event, merge result buffers and call session function to collect result list
   */
  private def collectData(buffer:ByteString):Unit = {
    /**
     * Check existing data block's length, if it's -1 means new data result then read the length and try to merge the result datagram in this length
     * buffer: received data buffer
     */
    def getResultBuffer(buffer:ByteString) = if (dataBlockLength<0) {
      dataBlockLength = buffer.toByteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt()
      buffer
    }
    else resultData++buffer
    /**
    * Analysis the received datagram, fetching the data part call resultProcessor to handle it. If finished
    * check whether need to send the result list to sync caller. Then clear this channel and dispose the session object
    */

    resultData = getResultBuffer(buffer)
    val bufferSize = resultData.size
    if (dataBlockLength<bufferSize) {
      log.info("Received data more than the returned data length.(data length:"+dataBlockLength+"), bufferSize("+bufferSize+")")
      val (dataBlock,restPart) = resultData.splitAt(dataBlockLength)
      clientSession.processResult(dataBlock)
      dataBlockLength = -1
      // if there is multiple result messages in one buffer, recursive to process it until finished
      collectData(restPart)
    }
    else if (dataBlockLength==bufferSize) {
      clientSession.processResult(resultData)
      finishRequest
      dataBlockLength = -1
    }
    // else is the dataBlock expected larger than the dataBuffers received so wait for next received data
  }

  override def preStart(): Unit = {
    // Call IO(Tcp) ! Connect, use the pool connection selector to remote service
    IO(Tcp) ! connConfigSelector()
  }
  override def postRestart(reason: Throwable): Unit = {
    log.error("Channel restarted.") //reason,
    // Call IO(Tcp) ! Connect, use the pool connection selector to remote service
    IO(Tcp) ! connConfigSelector()
  }
  override def postStop() {
    log.info("shut down channel Actor:" + channelId)
  }
  def receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.error("Connection failed, close store client channel actor.")
      if (clientSession!=null) clientSession.notifyConnClosed
      context stop self
    case c @ Tcp.Connected(remote, local) =>
      log.info(s"Connection to [$remote].")
      connection = sender()
      connection ! Register(self) // Must be sent then this connection be available to send/receive
      context become {
        case StoreCmd(request, isAsync, retProcessor) =>
          initRequest(retProcessor,if (isAsync) null else sender())
          connection ! Tcp.Write(request)
        case Tcp.CommandFailed(w: Tcp.Write) =>
          log.error("O/S buffer was full")
        case Tcp.Received(data) =>
          collectData(data)
        case "Close" => connection ! Tcp.Close
        case _: Tcp.ConnectionClosed =>
          log.info("Channel connection closed. Id("+channelId+").")

          if (clientSession != null) clientSession.notifyConnClosed
          context stop self
      }
    case req@StoreCmd(request, isAsync, retProcessor) =>
      self.tell(req,sender())
  }
}
/**
 * Channel pool for one instance, "ConnConfigSelector" should be provided here to let the Channels connect to proper remote host
 */
class StoreClientChannelPool(rPoolSize:Int,wrPoolSize:Int,connConfigSelector: () => Tcp.Connect) extends ChannelFactory {
  import Tcp._
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