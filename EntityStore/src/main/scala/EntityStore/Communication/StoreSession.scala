package EntityStore.Communication

import java.nio.ByteOrder

import scala.concurrent.Future
import EntityStore.Interface._
import java.util.Collection
import akka.util.ByteString
import java.util.ArrayList
import EntityInterface._
import akka.actor._
import EntityAccess.GeneralEntitySchema

case class StoreCmd(request:ByteString,isAsync:Boolean,resultProcessor:Session)
abstract class Session(val requestID:Int,schema:IEntitySchema=null,
                       clientChannelFactory:ChannelFactory=null,connClosedEvent:()=>Unit=null) {
  val REQUEST_FINISH_TIMEOUT = 3000000 // 30 seconds to time out
  val notifyConnClosed:()=>Unit = if (connClosedEvent==null) ()=>{completeResult(true)}
                                  else connClosedEvent
  protected var errorCode : Int = _
  protected var errorDetail : Long = _
  private val resultList : ArrayList[IEntityRandomAccess] =  new ArrayList[IEntityRandomAccess]()
  private var retSchema:IEntitySchema = schema
  protected var requestSender : ActorRef = _
  protected var clientChannel:Tuple2[Int,ActorRef] = _
  protected val schemaDocSchema: IEntitySchema = GeneralEntitySchema(classOf[ResultSchema])
  def initRequest(sender:ActorRef) = requestSender = sender
//  def sendRequest(request:ByteString,requestMode:ReqSendMode,sender:ActorRef=null):Future[Any]
  /**
   * Call back method used in channel when reading returned result list.
   * 
   */
  def processResult(data: ByteString): Unit
  /**
   * Process the async mode result finished event and notify the remote side if connection is interrupted when result not finished 
   */
  def completeResult(interrupted:Boolean) = {
    if (interrupted) requestSender ! getErrorStatus
    else {
      if (requestSender != null) requestSender ! getResult // Send back to the caller with result list
      else throw new IllegalArgumentException("Client channel got incorrect request with with none sender!")
    }
  }

  /**
   * Returned result from store must be encoded in: 4byte:totoal result data block length,2byte:finished flag,
   * 4byte:ErrorCode=0 is no error,8byte：Error detail which can be associated with store service log id
   * @param result ByteString split from data block of store returned result
   */
  protected def getResult(dataBuf: ByteString): Tuple2[ByteString, Boolean] = {
    def toFinishStatus(bytes: ByteString): Boolean = {
      bytes.asByteBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort() == 1
    }
    def takeResultStatus(result:ByteString): Unit = {
      val resultBuffer = result.asByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      errorCode = resultBuffer.getShort()
      errorDetail = resultBuffer.getLong()
    }
    // Skip the length
    val (finish, data) = dataBuf.splitAt(4)._2.splitAt(2)
    // Fetch out error code(Int), error details(Long) and result part
    val (statusBuffer, resultBuffer) = data.splitAt(10)
    // take the error code and details into the session object in result processor tuple
    takeResultStatus(statusBuffer)
    (resultBuffer, toFinishStatus(finish))
  }
  protected def addResult(result:IEntityRandomAccess) = if (resultList!=null) resultList.add(result)
                                              else throw new Exception("client Session hasn't been initialized.")
  // result stream will be processed by the callback function and stored in a list of IEntityRandomAccess objects
  def getResult : Collection[IEntityRandomAccess] = resultList    // Todo: this result should be enumerable and can be written in result with reader in th same time
  def hasError : Boolean = errorCode != 0
  def getErrorStatus = (errorCode,errorDetail)
  def finishSession : Unit = if (clientChannel!=null) {
    clientChannelFactory.returnChannel(clientChannel)
  }
  def setReturnSchema(schema:IEntitySchema):Unit = retSchema = schema
  def getReturnSchema:IEntitySchema = retSchema
}