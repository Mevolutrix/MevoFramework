package EntityStore.Client
import java.util
import CSON._
import akka.actor._
import akka.routing._
import EntityAccess._
import EntityInterface._
import akka.util.Timeout
import java.nio.ByteOrder
import java.nio.ByteBuffer
import EntityStore.Interface._
import scala.concurrent.duration._
import EntityStore.QueryProcessor._
import EntityStore.Interface.TxType._
import EntityStore.Interface.OutputMode._
import SBEServiceAccess.ServiceConfig

import ReqSendMode._
class StoreClientActor extends Actor with ActorLogging {
  type Binary = Array[Byte]
  override def postRestart(thr: Throwable): Unit ={
    log.info("Actor recovery. stop reason:"+thr)
  }

  def receive = {
    case CreateEntity(pKey,data,appSpace,schema,setName,tenantId,reqID) =>
      val sa = DSEClientAccessor.getSession(appSpace,reqID,schema,forward,sender())
      sa.insert(data, setName, schema.entityId, tenantId, EntityAccessor.getKeyRawvalue(pKey))
     // throw new Exception("Test crash resume.")
    case LoadEntityByKey(pKey,appSpace ,setName, returnSchema,tenantId,reqID) =>
      val sa = DSEClientAccessor.getSession(appSpace,reqID,returnSchema,forward,sender())
      sa.load(DSEClientAccessor.primaryKeyConditionHelper(EntityAccessor.getKeyRawvalue(pKey)),
              setName,tenantId, EntityAccessor.getKeyRawvalue(pKey))
    case UpdateEntity(pKey,data,appSpace,schema,setName,tenantId,reqID) =>
      val sa = DSEClientAccessor.getSession(appSpace,reqID,schema,forward,sender())
      sa.update(data,EntityAccessor.getKeyRawvalue(pKey),setName,schema.entityId,tenantId)
    case EntityQuery(querySt,args,select,alias,appSpace,setName,schema,tenantId,reqID) =>
      val requestContext = new SystemRequestContext(alias,appSpace,"","",tenantId,reqID)
      val q = new DynamicQuery(requestContext, schema.asInstanceOf[IEntitySet],setName)
      q.where(querySt, args).select(select).execute(forward,sender())
    case DelEntity(pKey,appSpace, returnSchema, setName, tenantId,reqID) =>
      val sa = DSEClientAccessor.getSession(appSpace,reqID,returnSchema,forward,sender())
      sa.delete(Array[Binary](EntityAccessor.getKeyRawvalue(pKey)),setName,
        returnSchema.entityId, tenantId)
    case a@_ => log.error("EntityAccessor - Unknown command:"+a)
  }
}
object EntityAccessor {
  import TxType._
  val accessorPoolSize = ServiceConfig.conf.getInt("mevo.store.accessorPool.size")
  val storeClient = ServiceConfig.appSystem.actorOf(Props(new StoreClientActor()).
    withRouter(RoundRobinPool(nrOfInstances = accessorPoolSize)),"DSEAccessor")
  type Binary = Array[Byte]
  implicit val timeOut = Timeout(30 seconds)
  def getSchemaID(entitySet:IEntitySet):String = entitySet.asInstanceOf[IEntitySchema].entityId
  def getRawValueList(valueList:Array[Object]):Array[Binary] = {
    if (valueList==null) return null
    
    val ret = new Array[Binary](valueList.length)
    for (i<-0 until valueList.length) ret(i) = if (valueList(i).isInstanceOf[QueryStatement]) null else getKeyRawvalue(valueList(i))
    ret
  }
  def getKeyRawvalue(key:Object):Array[Byte] = {
    if (key.isInstanceOf[Array[Byte]]) key.asInstanceOf[Array[Byte]]
    else GeneralEntityToCSON.getRawValue(key, true)
  }
  def createSubQuery(filter:String,requestContext:IRequestContext, entitySet:IEntitySet,argList:Array[Object]=null,selectColums:String=null):Object = {
    val q = new DynamicQuery(requestContext, entitySet).where(filter,argList)
    if (selectColums==null) q.getStatement
    else q.select(selectColums).getStatement
  }
  def createSubCondition(filter:String,requestContext:IRequestContext, entitySet:IEntitySet,argList:Array[Object]):Object =
    createSubQuery(filter,requestContext,entitySet,argList).asInstanceOf[QueryStatement].query.queryCondition
    
  def insert(data:Binary, requestContext:IRequestContext, entitySetName:String, schema:IEntitySchema, pKey:Object) = {
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID, schema,syncMode)
    sa.insert(data, entitySetName, schema.entityId, requestContext.tenantID, getKeyRawvalue(pKey))

    sa.waitForResult
  }
  def loadByKey(pKey:Object, requestContext:IRequestContext, entitySetName:String, returnSchema:IEntitySchema) = {
    val key = getKeyRawvalue(pKey)
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID, returnSchema,syncMode)
    sa.load(DSEClientAccessor.primaryKeyConditionHelper(key), entitySetName, requestContext.tenantID, key)

    sa.waitForResult
  }
  def update(requestContext:IRequestContext, entitySetName:String, pKey:Object, data:Binary,schema:IEntitySchema) = {
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID, schema,syncMode)
    sa.update(data, getKeyRawvalue(pKey), entitySetName,schema.entityId, requestContext.tenantID)

    sa.waitForResult
  }
  def update(requestContext:IRequestContext, entitySetSchema:IEntitySchema,pKey:Object,filter:String,argList:Array[Object],
      updateJS:String,preOpJS:String=null,jsParams:Array[Object]) = {
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID,entitySetSchema,syncMode)
    val updateRangeCondition = createSubCondition(filter, requestContext,
      entitySetSchema.asInstanceOf[IEntitySet], argList)
    sa.customUpdate(EntityAccessor.getKeyRawvalue(pKey), preOpJS, updateJS,
      entitySetSchema.asInstanceOf[IEntitySet].setName, entitySetSchema.entityId,
      requestContext.tenantID, updateRangeCondition, EntityAccessor.getRawValueList(jsParams))

    sa.waitForResult
  }
  def updateProperties(requestContext:IRequestContext, entitySetName:String, returnSchema:IEntitySchema,
                       properties:Array[Tuple2[String,Binary]],pKey:Object,preOpJS:String=null) = {
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID, returnSchema,syncMode)
    sa.updateProperty(properties, getKeyRawvalue(pKey), entitySetName, returnSchema.entityId, requestContext.tenantID, preOpJS)

    sa.waitForResult
  }
  def delete(requestContext:IRequestContext, entitySetName:String,schema:IEntitySchema,keyList:Array[Object],
             preOpJS:String=null, JSParams:Array[String]=null) = {
    val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,requestContext.requestID, schema,syncMode)
    sa.delete(getRawValueList(keyList), entitySetName, schema.entityId, requestContext.tenantID,
      preOpJS, JSParams)

    sa.waitForResult
  }
  def query(queryRequest:IQueryRequest,requestContext:IRequestContext, entitySetName:String,
            schema:IEntitySchema,requestMode:ReqSendMode=syncMode,sender:ActorRef=null) = {
    val q = new DynamicQuery(requestContext, schema.asInstanceOf[IEntitySet],entitySetName);
    q.where(queryRequest.filters, queryRequest.params).
      select(queryRequest.selectColumns).
      groupBy(queryRequest.groupBy, queryRequest.groupValue).
      execute(requestMode,sender)  //sync request. this will return result by await the future
  }
  def closeService = throw new Exception("Need to be implemented for gracefully shut down")
  def getResult(result:Any,typeInfo:Class[_]):AnyRef = {
    val serializer = GeneralEntityToCSON(typeInfo)
    val schema = GeneralEntitySchema(typeInfo)
    val retList = ResultFormat.getCSONResult(result.asInstanceOf[util.ArrayList[CSONDocument]], schema)
    if (retList.size > 0) serializer.getObject(retList(0))
    else null
  }

}

class EntityAccessor(requestContext:IRequestContext, returnSchema:IEntitySchema,TxMode:TxType = TxType.NA, 
    opTemplate:Option[CSONDocument]=None,buildTemplate:Boolean=false,reqSendMode:ReqSendMode=asyncMode,
    sender:ActorRef=null) {
  type Binary = Array[Byte]
  
  val sa = DSEClientAccessor.getSession(requestContext.appSpaceID,
      requestContext.requestID, returnSchema,reqSendMode, sender)
  sa.InitRequest(opTemplate)
  if (buildTemplate || TxMode!=TxType.NA)sa.beginTx
  // Need to be evaluated later, as these API was built for custom DSE service. They may not apply for registerRequest
  
  def insert(dataBuffer:Binary, schema:IEntitySchema,requestContext:IRequestContext, entitySetName:String,
             pKey:Object,operationJS:String=null, preOpJS:String = null, dataParams:Array[String] = null,
         output:OutputMode = ToClient, contextIndex:Array[Int] = null) = {
    sa.insert(dataBuffer, entitySetName, schema.entityId, requestContext.tenantID,
      EntityAccessor.getKeyRawvalue(pKey), operationJS,preOpJS,dataParams,output,contextIndex)
  }
  def batchInsert(insertRecords:Array[Tuple2[Object,IEntityRandomAccess]],requestContext:IRequestContext,
                  entitySetName:String,operationJS:String=null, preOpJS:String = null,
                  dataParams:Array[Binary] = null,output:OutputMode = ToClient, contextIndex:Array[Int] = null) = {
    val dataList = new Array[Binary](insertRecords.length)
    val keyList = new Array[Binary](dataList.length)
    for (i<-0 until insertRecords.length) {
      dataList(i) = insertRecords(i)._2.asInstanceOf[CSONDocument].getBytes
      keyList(i) = EntityAccessor.getKeyRawvalue(insertRecords(i)._1)
    }
    sa.batchInsert(dataList, keyList, entitySetName, insertRecords(0)._2.getSchema.entityId,
      requestContext.tenantID, output, contextIndex)
  } 
  /**
   * contextIndex used for Jscript engine load the query result into transaction context
   */
  def loadByKey(pKey:Object, entitySetName:String, returnSchema:IEntitySchema,output:OutputMode=ToClient,
                contextIndex:Array[Int]=null) = {
    val key = EntityAccessor.getKeyRawvalue(pKey)
    sa.load(DSEClientAccessor.primaryKeyConditionHelper(key), entitySetName, requestContext.tenantID,
      key,output,contextIndex)
  }
  def update(requestContext:IRequestContext, entitySetSchema:IEntitySchema,pKey:Object,
             filter:String,argList:Array[Object],updateJS:String, preOpJS:String=null,
             jsParams:Array[String]=null,output:OutputMode=ToClient,contextIndex:Array[Int]=null) = {
    val updateRangeCondition = EntityAccessor.createSubCondition(filter, requestContext,
      entitySetSchema.asInstanceOf[IEntitySet], argList)
    sa.customUpdate(EntityAccessor.getKeyRawvalue(pKey), preOpJS, updateJS,
      entitySetSchema.asInstanceOf[IEntitySet].setName,entitySetSchema.entityId,
      requestContext.tenantID, updateRangeCondition,EntityAccessor.getRawValueList(argList),
      jsParams, output, contextIndex)
  }
  def delete(requestContext:IRequestContext, entitySetName:String,schema:IEntitySchema,
             keyList:Array[Object],preOpJS:String=null,JSParams:Array[String]=null,
             contextIndex:Array[Int]=null) = {
    sa.delete(EntityAccessor.getRawValueList(keyList),entitySetName, schema.entityId, requestContext.tenantID,
      preOpJS,JSParams,contextIndex)
  }
  def query(queryRequest:IQueryRequest,requestContext:IRequestContext, entitySetSchema:IEntitySchema,
            output:OutputMode=ToClient,contextIndex:Array[Int]=null) = {
    val q = new DynamicQuery(requestContext, entitySetSchema.asInstanceOf[IEntitySet]).
      where(queryRequest.filters, queryRequest.params).select(queryRequest.selectColumns).
      groupBy(queryRequest.groupBy, queryRequest.groupValue)
    sa.query(q, output, contextIndex)
  }
  def replaceOpParams(opIndex:Int,replaceParams:OperationParams) =
    sa.replaceOperationParams(opIndex, requestContext.tenantID, replaceParams)
  
  def commit = sa.endTx(TxMode)

  /**
   * Wrap the statements in this SA session as cson into ByteBuffer
   * @param csonBuffer the ByteBuffer which will be written
   * @return
   */
  def saveTransactionTemplate(csonBuffer:ByteBuffer=null) = {
    val buf = if (csonBuffer!=null)csonBuffer.order(ByteOrder.LITTLE_ENDIAN) else null
    sa.saveOperationTemplate(TxMode,buf)
  }
  def getResult = sa.waitForResult
}