package EntityStore.Interface

import java.util

import CSON._
import HandlerSocket.Protocol.MySQLErrorCodes
import akka.io.Tcp
import CSON.Types._
import EntityInterface._
import akka.util.ByteString
import java.nio.{ByteOrder, ByteBuffer}
import akka.actor.{ActorRef,ActorSystem}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import EntityAccess.{GeneralEntitySchema, JSONSerializer, GeneralEntityToCSON, DynamicSchema}
import org.slf4j.LoggerFactory

object ReqSendMode extends Enumeration {
  type ReqSendMode = Value
  val forward = Value(2)
  val asyncMode = Value(1)
  val syncMode = Value(0)
}
object ResultFormat extends Enumeration {
  val log = LoggerFactory.getLogger("Communication")
  type ResultFormat = Value
  val json = Value(2)
  val cson = Value(1)
  val na = Value(0)
  def getCSONResult(list:java.util.List[CSONDocument],schema:IEntitySchema):IndexedSeq[CSONDocument] =
    for (i<-0 until list.size()) yield {
      val ret = list.get(i)
      ret.getSchema.entityId match {
        case "EntityStore.Interface.JSONRecord" => JSONSerializer.unapply(
          ret.getValue("jsonData").asInstanceOf[String], schema).asInstanceOf[CSONDocument]
        case _ => ret
      }
    }
  def getJsonResult(list:java.util.List[CSONDocument]):IndexedSeq[String] =
    for (i<-0 until list.size()) yield {
      val ret = list.get(i)
      ret.getSchema.entityId match {
        case "EntityStore.Interface.JSONRecord" => ret.getValue("jsonData").asInstanceOf[String]
        case _ => JSONSerializer(ret)
      }
    }
  def getResult(result:Any):String = result  match {
    case retList: util.ArrayList[CSONDocument] =>
      if (retList.size() == 1) ResultFormat.getJsonResult(retList)(0)
      else ResultFormat.getJsonResult(retList).mkString("[", ",", "]")
    case ret: CSONDocument => ret.getSchema.entityId match {
      case "EntityStore.Interface.JSONRecord" =>
        ret.getValue("jsonData").asInstanceOf[String]
      case _ => JSONSerializer(ret)
    }
    case error: (Int, Long) => "{Error:'" + MySQLErrorCodes(error._2) + "'}"
  }
  private def finishThenUpdateLength(buf:ByteBuffer,lengthPos:Int):ByteBuffer = {
    val lastWritePos = buf.position()
    buf.putInt(lengthPos, lastWritePos - lengthPos)
    buf.position(lastWritePos)
    buf
  }
  private def formatReturnError(buf:ByteBuffer,errCode:Int=0,details:Long=0): ByteBuffer = {
    val lengthPos = buf.position()
    buf.putInt(0) // Occupied the Length position (4 Bytes)
    buf.putShort(0) // Finish flag
    buf.putShort(errCode.toShort)
    buf.putLong(details) // put error details reference ID which can be used to search log
    buf.putInt(0) //RetNum
    finishThenUpdateLength(buf,lengthPos)
  }
  private def formatReturnMessage(buf:ByteBuffer,count:Int=0, finished:Boolean=true,
                          resultList:IndexedSeq[IEntityRandomAccess]):ByteBuffer = {
    var outBuffer = buf
    val lengthPos = outBuffer.position()
    outBuffer = formatReturnError(outBuffer)
    // move to write Finish flag
    buf.putShort(lengthPos + 4, if (finished) 1 else 0) // =1 When finished, no more result
    buf.putInt(lengthPos + 16, count) // RetNum

    if (count > 0) {
      var item = resultList(0)
      outBuffer = writeStoredSchema(buf, item.getSchema) // Output return schema if it's dynamic generated or JSON format
      // format the return list into ByteBuffer
      for(n <- 0 until count)
        outBuffer = resultList(n).asInstanceOf[CSONDocument].toByteBuffer(outBuffer)
      finishThenUpdateLength(outBuffer, lengthPos)
    }
    outBuffer
  }
  private def writeStoredSchema(buf:ByteBuffer,schema:IEntitySchema) =
    GeneralEntityToCSON.serializeObject(schema match {
      case s:GeneralEntitySchema => new ResultSchema(schema)
      case s:DynamicSchema => new ResultSchema(schema)
      case _ =>
        val ret = new ResultSchema(null)
        ret.schemaName = schema.entityId
        ret
    },buf)._2
  def outputWriter(ob:OutputBuffer,errCode:Int,details:Long,count:Int,resultList:IndexedSeq[IEntityRandomAccess]):Unit =
  {

    if (errCode!=0) ob.formatResult(formatReturnError(_,errCode,details))
    // formatResult may get new returned ByteBuffer and update into ob
    else ob.formatResult(formatReturnMessage(_,count,ob.isFinished,resultList))
  }
  def writeResult(result:String):ByteString = {

    val source = Array[IEntityRandomAccess]{GeneralEntityToCSON.serializeObject(new JSONRecord(result),null)._1}
    val bufLength = 64*1024
    val resultBuffer = ByteBuffer.allocate(bufLength).order(ByteOrder.LITTLE_ENDIAN)

    formatReturnMessage(resultBuffer,1,true,source)
    resultBuffer.flip()
    ByteString(resultBuffer)
  }
}
/**
 * Convert the incoming request data block into a RequestMessage format CSON Document used for Store Executor decoding
 * request message and get parameters
 * @param requestMessage RequestMessage format CSON Document（readonly IEntityRandomAccess interface)
 * @param channelActor ActorRef holding this client request connection, Store Executor need it to send back result
 */
case class RequestTask(requestMessage:IEntityRandomAccess,channelActor:ActorRef,ack:AckOutput)
class JSONRecord(json:String) { var jsonData:String = json }
class Property {
  var name:String = _
  var typeCode:Byte = _
}

/**
 * Serialization helper for return result schema information transferred to Store client.
 * The return result may be: Generanl schema which was loaded using MetadataManager, in this case, just return
 * the schema id in schemaName property. For dynamic schema which generated in Query projection scenario, it will
 * be serialized simply in this class; for server side POJO object, it's schema definition will be serialized as
 * well here.
 * @param schema
 */
class ResultSchema(schema:IEntitySchema) {
  var schemaName:String = if (schema!=null) schema.entityId else null
  var properties:Array[Property] = if (schema!=null && schema.count>0) new Array[Property](schema.count) else null
  if (properties != null) for (i<-0 until schema.count) properties(i) = new Property {
    name=schema.getPropertyName(i);
    typeCode=schema.getTypeCode(i)
  }
  def toSchema:IEntitySchema = {
    val ret = new DynamicSchema(schemaName)
    for (item<-properties) { ret.add(item.name, CSONTypesArray.CSONElementTypes(item.typeCode)) }
    ret
  }
}
class OutputBuffer(allocator:()=>ByteBuffer=null) {
  val bufAllocator = if (allocator==null) ()=> ByteBuffer.allocate(64*1024) else allocator
  private var resultBuffer: ByteBuffer = _
  private var finished = false
  private var resultSize = 0
  def formatResult(formatWriter:(ByteBuffer)=>ByteBuffer):Unit = {
    resultBuffer = formatWriter( (if (resultBuffer == null) bufAllocator()
                                  else resultBuffer.clear().asInstanceOf[ByteBuffer]).order(ByteOrder.LITTLE_ENDIAN) )
    resultBuffer.flip()
    resultSize = resultBuffer.limit()
  }
  def getSize = resultSize
  def getResult():ByteString = ByteString(resultBuffer)
  def finishOutput(b:Boolean=true) = finished = b
  def isFinished = finished
}
/**
 * Store Executor will receive this Ack(From Store server channel Handler Actor) and move next step(ouput and return ob)
 * @param ob Output buffer to be used (for performance and implementation isolation reason)
 * @param outOperator Output to buffer implementation function, the gneral implementation for this method is in
 *                    EntityStoreServer.scala getAckOutput.outputWriter. For schema of returned result, it's handled in
 *                    this way: 1. GeneralEntitySchema (returned is an POJO class object) - same as bellow
 *                              2. Dynamic generated Schema(Query Select results) - Store the schema into ResultSchema
 *                              3. standard Schema(load by MetadataManager), save the schema name into ResultSchema.schemaName
 */
case class AckOutput(ob:OutputBuffer, outOperator:(OutputBuffer,Int,Long,Int,
  IndexedSeq[IEntityRandomAccess])=>Unit) extends Tcp.Event
/**
 * Ack based send/read command object
 */
case object Ack extends Tcp.Event
/**
 * Interface for channel configuration and load balance strategy
 */
trait ChannelFactory {
  val readerPoolSize:Int
  val writerPoolSize:Int
  protected def buildNewChannel(readOnly:Boolean=false):(Int, ActorRef)
  def getChannel(readOnly:Boolean=false): (Int, ActorRef) = {
    def pollChannel(readOnly:Boolean):(Int,ActorRef) = {
      var ret = if (readOnly) readOnlyPool.poll() else channelPool.poll()
      if (ret == null) null
      else if (channelMap.containsKey(ret._1)) ret
      else {
        decPoolCount(readOnly)
        pollChannel(readOnly)
      }
    }
    var ret = pollChannel(readOnly)
    var loopLimit = 4
    while (ret==null && loopLimit>0) {
      if (!exceedPoolSizeLimit(readOnly)) {
        val newChannel@(id, channel) = buildNewChannel(readOnly)
        channelMap.put(id, (channel, readOnly))
        return newChannel
      }
      else {
        ResultFormat.log.error("DEBUG: get channel exceed limit("+(if (readOnly) readerPoolSize else writerPoolSize)+"), retry to get again.")
        ret = pollChannel(readOnly)
        loopLimit-=1
      }
    }
    if (loopLimit<=0) throw new RuntimeException("Too many request for connections. out of connection pool resource.")
    ret
  }
  def returnChannel(channel:(Int, ActorRef)):Unit = {
    if (channelMap.get(channel._1)._2) {
        readOnlyPool.add(channel)
      //if (!readOnlyPool.contains(channel)) {
      //ResultFormat.log.info("Channel"+channel+" returned")
      //}
      //else throw new Exception("Channel pool for:"+this+" found channel"+channel+" returned already in pool.")
    }
    else {
        channelPool.add(channel)
      //if (!channelPool.contains(channel))
      //else throw new Exception("Channel pool for:"+this+" found channel"+channel+" returned already in pool.")
    }
  }
  def disconnectChannel(channelId:Int):Unit = if (channelMap.containsKey(channelId)) {
    val channel@(_,readOnly) = channelMap.remove(channelId)
    decPoolCount(readOnly)
  }

  protected def getNewChannelId = channelCount.incrementAndGet()
  protected def decPoolCount(readOnly:Boolean) = if (readOnly) readerPoolCount.decrementAndGet()
                                                   else writerPoolCount.decrementAndGet()
  protected def incPoolCount(readOnly:Boolean) = if (readOnly) readerPoolCount.incrementAndGet()
                                                   else writerPoolCount.incrementAndGet()
  private def exceedPoolSizeLimit(readOnly:Boolean):Boolean =
    if (readOnly) (readerPoolCount.get()>=readerPoolSize)
    else (writerPoolCount.get()>=writerPoolSize)
  protected val channelMap = new ConcurrentHashMap[Int,(ActorRef, Boolean)]
  protected val channelPool = new java.util.concurrent.ConcurrentLinkedQueue[(Int, ActorRef)]
  protected val readOnlyPool = new java.util.concurrent.ConcurrentLinkedQueue[(Int, ActorRef)]
  private val channelCount = new AtomicInteger(0)
  private val readerPoolCount = new AtomicInteger(0)
  private val writerPoolCount = new AtomicInteger(0)

  def shutdown = {
    val channels = channelMap.elements()
    while (channels.hasMoreElements()) channels.nextElement()._1 ! "Close"
    channelMap.clear()
    channelPool.clear()
    readOnlyPool.clear()
  }
}

/**
 * client request session is an actor which responsible for receiving request from socket and sending back result
 * The Store Executor actor will do part of the query work(Query task may need many processing cycles or return multiple
 * slice of result data. To avoid blocking the query engine with some long running requests, one request will be executed
 * in one cycle then save the executing context) and replied to the client request Session Actor. Then the request context
 * need to be sent back to Store Executor by client request session actor(Thus can do some network flow control).
 * “dispose" method need to be called when the client request session cancel this reqeust processing
 */
trait SessionContext {
  def fillResult(outputWriter:(Int,Long,Int,IndexedSeq[IEntityRandomAccess])=>Unit,updateFinish:(Boolean)=>Unit)
  def isFinished:Boolean
  def dispose:Unit
}
/**
 * Abstract store query and operation processor. Provide as an interface so other modules can access the store functions by interface
 * Usage: call the "queryProcessor" function to get actor thus we can send request
 */
trait ServiceOperationExecutor {
  /**
   * Vector for client channel Actor(Hold the TCP connection) -> request processing session data
   */
  private val reqProcessingMap = new ConcurrentHashMap[OutputBuffer,SessionContext]
  val reqMsgSerializer = GeneralEntityToCSON(classOf[RequestMessage])
  val reqMsgSchema = EntityAccess.GeneralEntitySchema(classOf[RequestMessage])

  def system : ActorSystem
  def reqProcessor(alias:String):ActorRef
  def registerRequest(ob:OutputBuffer, sessionData:SessionContext):Unit = reqProcessingMap.put(ob, sessionData)
  def getContext(ob:OutputBuffer):SessionContext = reqProcessingMap.get(ob)
  def finishContext(ob:OutputBuffer) = {
    val context =reqProcessingMap.remove(ob)
    if (context!=null) context.dispose
  }
  /**
   * Cancel the IO Handle associated Actor processing task as the connection may be closed or broken
   */
  def terminateRequest(reqConn:ActorRef): Unit = {
    val sessionContext = reqProcessingMap.get(reqConn)
    if (sessionContext !=null) {
      sessionContext.dispose
      reqProcessingMap.remove(reqConn)
    }
  }
  /**
   * Return the read only CSON document which is very performance efficiency than convert to class object using reflection
   */
  def decodeMessage(reqBuf: ByteString): IEntityRandomAccess = new CSONDocument(reqMsgSchema, Option(reqBuf.asByteBuffer))
  def convertMsg(reqMsg: IEntityRandomAccess): RequestMessage =
    reqMsgSerializer.getObject(reqMsg.asInstanceOf[CSONDocument]).asInstanceOf[RequestMessage]
}
