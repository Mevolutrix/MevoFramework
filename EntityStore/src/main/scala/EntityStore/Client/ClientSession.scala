package EntityStore.Client
import java.util.concurrent.{ConcurrentHashMap,TimeUnit}
import EntityStore.Connection.StoreInstances
import EntityStore.Metadata.MetadataManager
import EntityStore.Interface.ReqSendMode._
import EntityStore.Communication.StoreCmd
import org.slf4j.{LoggerFactory, Logger}
import scala.concurrent.{Await, Future}
import akka.util.{Timeout, ByteString}
import mevolutrix.Interface.SAEReqMsg
import EntityStore.Communication._
import EntityStore.Interface._
import java.nio.ByteBuffer
import java.nio.ByteOrder
import EntityInterface._
import akka.pattern._
import EntityAccess._
import akka.actor._
import CSON._

object DSEClientAccessor {
  val log = LoggerFactory.getLogger("DSEClientAccessor")
  val reqMsgSerializer = GeneralEntityToCSON(classOf[RequestMessage])
  val smeReqMsgSerializer = GeneralEntityToCSON(classOf[SAEReqMsg])
  val REQUEST_VERSION = 9
  //
  def getSession(appSpaceName:String,requestId:Int,schema:IEntitySchema=null,requestMode:ReqSendMode=asyncMode,
                 sender:ActorRef=null,readOnly:Boolean=false,connClosedEvent:()=>Unit=null) =
    StoreInstances(appSpaceName).session(requestId,schema,requestMode,sender,readOnly,connClosedEvent)

  def primaryKeyConditionHelper( keyValue:Array[Byte]):Condition = {
    new Condition {
      queryOperations = new Array[SearchOperation](1)
      queryOperations(0) = new SearchOperation {
        compareOperator = CompareOperator.Equal
        searchPropertyName = null
        keyType = KeyType.PrimaryKey //search key is primary key
        paramertIndex = 0
        compareValue =Array[SearchValue](new SearchValue {value = keyValue})
      }
    }
  }
}

import ReqSendMode._
class ClientSession(requestId:Int, returnSchema:IEntitySchema, clientChannelFactory: ChannelFactory=null,
                    requestMode:ReqSendMode=asyncMode,sender:ActorRef=null,
                    readOnly:Boolean=false,connClosedEvent:()=>Unit=null)
       extends Session(requestId,returnSchema,clientChannelFactory,connClosedEvent)   // Todo: complete the ConnectionClose event handler
       with IServiceAccessor with StatementTemplate {
  import TxType._
  import OutputMode._
  override type Binary = Array[Byte]
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)

  clientChannel = clientChannelFactory.getChannel(readOnly)

  private def sendRequest(request:ByteString,requestMode:ReqSendMode,sender:ActorRef=null):Future[Any] = {
    requestMode match {
      case ReqSendMode.asyncMode =>
        clientChannel._2 ! StoreCmd(request,true,this)
        null
      case ReqSendMode.syncMode =>
        (clientChannel._2).ask(StoreCmd(request,false,this))
      case ReqSendMode.forward =>
        clientChannel._2.tell(StoreCmd(request,false,this),sender)
        null
    }
  }
  private var syncFuture:Future[Any] = _

  private def submitRequest(reqMsg:ByteBuffer):Unit = syncFuture = sendRequest(ByteString(reqMsg), requestMode, sender)

  private def getTrxReqMessage(trxType:TxType):RequestMessage = {
    val ret = new RequestMessage
    ret.header.operatingScope = OperationScope.Transaction
    ret.header.trxType = trxType
    ret.statement = operationStatements.toArray[OperationStatement](new Array[OperationStatement](operationStatements.size()))
    ret
  }
  private def sendRequest(body:OperationStatement):Unit = {
    if (operationStatements!=null) operationStatements.add(body)
    else {
      val ret = new RequestMessage {
          header.operatingScope = OperationScope.Transaction
          header.trxType = NA
          statement = new Array[OperationStatement](1)
          statement(0)= body
        }
      val (reqMsgCSON,reqMsgBuf) = DSEClientAccessor.reqMsgSerializer.writeObjectToCSON(ret,
                                     ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN))
      reqMsgBuf.flip()
      submitRequest(reqMsgBuf)
    }
  }

  /**
   * Call back method used in channel when reading returned result list.
   *
   */
  def processResult(data: ByteString): Unit = {
    def getRetSchema(schemaDoc:CSONDocument): IEntitySchema = {
      val retSchemaName = schemaDoc.getValue(0).asInstanceOf[String]
      val ret = retSchemaName match {
        case null => null
        case "EntityStore.Interface.JSONRecord" =>
          GeneralEntitySchema(classOf[JSONRecord])
        case "select" => GeneralEntityToCSON.deserializeCSON(schemaDoc).asInstanceOf[ResultSchema].toSchema
        case schemaName:String => MetadataManager.getSchema(schemaName)
      }
      schemaDoc.completeRead
      ret
    }
    def getResultFromBuffer(buffer: ByteBuffer,recordLeft:Int): Unit = {
      if (recordLeft>0) {
        val ret = new CSONDocument(getReturnSchema,Some(buffer))
        addResult(ret)
        ret.completeRead
        getResultFromBuffer(ret.completeRead,recordLeft-1)
      }
    }

    val (resultByteStr, finished) = getResult(data)
    if (hasError) {
      // Send back to the caller with error code and details tuple
      if (requestSender != null) requestSender ! getErrorStatus
      // Dispose the session object to return this channel to connection pool
      finishSession
      //log.error("Got error code:"+clientSession.getErrorStatus)
    }
    else {
      val buffer = resultByteStr.asByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      val retNum = buffer.getInt()
      if (retNum > 0) {
        val resultSchema = getRetSchema(new CSONDocument(schemaDocSchema, Some(buffer)))
        if (resultSchema != null) setReturnSchema(resultSchema)
        getResultFromBuffer(buffer, retNum)
      }
      if (finished) completeResult(false)
      // reply the caller when this is a sync request, it will get the Await(future) call to get result
      if (finished) {
        // Dispose the session object to return this channel to connection pool
        finishSession
      }
    }
  }
  def beginTx = operationStatements = new java.util.ArrayList[OperationStatement]
  // Finish transaction with the specified txType, wrap request Statement to CSON message and submit to remote server
  def endTx(transactionType:TxType=SingleInstanceTx) = {
    val (csonDoc,reqMsgBuf) = DSEClientAccessor.reqMsgSerializer.writeObjectToCSON(getTrxReqMessage(transactionType),
                              ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN))
    reqMsgBuf.flip()
    submitRequest(reqMsgBuf) 
  }

  /**
   * submit SME request message to SME service port through TCP channel
   * @param request
   */
  private def submitSMEReq(request:SAEReqMsg):Unit = {
    val (reqMsgCSON, reqMsgBuf) = DSEClientAccessor.smeReqMsgSerializer.writeObjectToCSON(request,
      ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN))
    reqMsgBuf.flip()
    submitRequest(reqMsgBuf)
  }
  def smeGetRequest(app_Space:String,reqFunc:String,reqParams:String) = {
    val reqMsg = new SAEReqMsg {
      appSpace = app_Space
      funcName = reqFunc
      params = reqParams
    }
    submitSMEReq(reqMsg)
  }
  def smePostRequest(app_Space:String,reqFunc:String,reqParams:String,postData:String) = {
    val reqMsg = new SAEReqMsg {
      isPostCall = true
      appSpace = app_Space
      funcName = reqFunc
      params = reqParams
      data = postData
    }
    submitSMEReq(reqMsg)
  }
  def svcGetRequest(app_Space:String,reqFunc:String,reqParams:String) = {
    val reqMsg = new SAEReqMsg {
      isSMECall = false
      appSpace = app_Space
      funcName = reqFunc
      params = reqParams
    }
    submitSMEReq(reqMsg)
  }
  def svcPostRequest(app_Space:String,reqFunc:String,reqParams:String,postData:String) = {
    val reqMsg = new SAEReqMsg {
      isSMECall = false
      isPostCall = true
      appSpace = app_Space
      funcName = reqFunc
      params = reqParams
      data = postData
    }
    submitSMEReq(reqMsg)
  }
  def insert(entity2Insert:Binary,entitySetName:String, schemaId:String, tenantId:Int, P_Key:Binary = null,
        operationJS:String=null, preOpJS:String = null, dataParams:Array[String] = null,
        output:OutputMode = ToClient, contextIndex:Array[Int] = null) = {
    val insertStatement = new InsertStatement{
      queryMethod = QueryMethod.Insert
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      preOperationJS = preOpJS
      modifierJS = operationJS
      records = new Array[InsertRecord](1)
      records(0) = new InsertRecord {key = P_Key;data = entity2Insert}
      outputTarget = output
      contextRef = contextIndex
      jsParams = dataParams
    }
    sendRequest(insertStatement)
  }
  def batchInsert(entities:Array[Binary], primaryKeys:Array[Binary], entitySetName:String, schemaId:String,
                  tenantId:Int,output:OutputMode = Non, contextIndex:Array[Int] = null) = {
    val insertRecords = new Array[InsertRecord](entities.length)
    for (i<-0 until entities.length) insertRecords(i)= new InsertRecord {key = primaryKeys(i);data = entities(i)}
    sendRequest(new InsertStatement {
      queryMethod = QueryMethod.Insert
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      records = insertRecords
      outputTarget = output
      contextRef = contextIndex
      jsParams = null
    })
  }
  def update(updateData:Binary, P_Key:Binary, entitySetName:String, schemaId:String, tenantId:Int) = {
    sendRequest(new UpdateStatement{
      queryMethod = QueryMethod.Update
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      primaryKey = P_Key
      data = updateData
      func = TransformerFunction.REPLACE
    })
  }
  def update(updateData:Binary, updateCondition:Condition, entitySetName:String, schemaId:String, tenantId:Int) = {
    sendRequest(new UpdateStatement{
      queryMethod = QueryMethod.Update
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      condition = updateCondition
      data = updateData
      func = TransformerFunction.REPLACE
    })
  }
  def updateProperty(properties:Array[Tuple2[String,Binary]],P_Key:Binary, entitySetName:String, schemaId:String,
                     tenantId:Int,preOperationJS:String = null) = {
    val updateProperties = new Array[Transformer](properties.length)
    for (i<-0 until properties.length) updateProperties(i) =
      new Transformer {fieldIndex=properties(i)._1;data=properties(i)._2}
    
    sendRequest(new UpdateStatement{
      queryMethod = QueryMethod.Update
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      primaryKey = P_Key
      transformer = updateProperties
      func = TransformerFunction.PATIAL_REPLACE
    })
  }
  def customUpdate(P_Key:Binary, preOpJS:String, JScript:String, entitySetName:String, schemaId:String,tenantId:Int, 
            queryCommand:Object = null, queryArgList:Array[Binary],dataParams:Array[String] = null,
            output:OutputMode = Non, contextIndex:Array[Int] = null) = {
    val updateRangeCondition = if (queryCommand.isInstanceOf[Condition]) 
                     queryCommand.asInstanceOf[Condition]
                   else null

    sendRequest(new UpdateStatement{
      queryMethod = QueryMethod.Update
      argList = if (queryCommand==null) null else queryArgList 
      es_name = entitySetName
      schemaID = schemaId
      tenantID = tenantId
      primaryKey = P_Key
      condition = if (queryCommand==null) null else updateRangeCondition
      func = TransformerFunction.JS_CODE
      jsParams = dataParams
      preOperationJS = preOpJS
      modifierJS = JScript
      outputTarget = output
      contextRef = contextIndex
    })
  }
  
  def load(loadCondition:Condition, entitySetName:String, tenantId:Int, P_Key:Binary = null,
           output:OutputMode = ToClient,contextIndex:Array[Int]=null) = {
    sendRequest(new QueryStatement{
      queryMethod = QueryMethod.Query
      argList = if (P_Key==null) null 
            else {
              val ret = new Array[Binary](1)
              ret(0) = P_Key
              ret
            }
      outputTarget = output
      contextRef = contextIndex
      tenantID = tenantId
      query = new QueryOperationSet {
        queryEntitySet = entitySetName
        queryCondition = loadCondition
      }
    })
  }
  def query(queryCommand:Object, output:OutputMode = ToClient,contextIndex:Array[Int]=null) = {
    val query = queryCommand.asInstanceOf[QueryStatement]
    query.outputTarget = output
    query.contextRef = contextIndex
    sendRequest(query)
  }
  def delete(delKeys:Array[Binary], entitySetName:String, schemaId:String, tenantId:Int, preOpJS:String = null,
             dataParams:Array[String] = null,contextIndex:Array[Int]=null) = {
    sendRequest(new DeleteStatement {
      queryMethod = QueryMethod.Delete
      es_name = entitySetName
      schemaID = schemaId
      outputTarget = OutputMode.Non
      tenantID = tenantId
      keys = delKeys
      preOperationJS = preOpJS
      jsParams = dataParams
      this.contextRef = contextIndex
    })
  }
  def waitForResult:Any = {
    Await.result(syncFuture,timeout.duration)
  }
  def saveOperationTemplate(transactionType:TxType,csonBuffer:ByteBuffer):CSONDocument =
    saveOperationTemplate(transactionType,getTrxReqMessage(transactionType),csonBuffer)
}
