package EntityStore.Interface
import TxType._
import EntityInterface._
import CSON.{CSONElementArray, CSONDocument}
import java.nio.ByteBuffer
import scala.concurrent.{Await, Future}
import EntityAccess._

/**
 * Common statement template load, save and replace new parameters operation
 */
trait StatementTemplate {
type Binary = Array[Byte]
  // Class Object of Request Statement list which used to parameterize the cached store operation template
  var operationStatements:java.util.ArrayList[OperationStatement] = null
  // CSON document to save current operation list into it or load from external template CSON
  var templateCSON:Option[CSONDocument] = _
  val reqMsgSerializer = GeneralEntityToCSON(classOf[RequestMessage])
  var opStArray:CSONElementArray = null
  def InitRequest(operationTemplate:Option[CSONDocument] = None) = {
    templateCSON = operationTemplate
    operationStatements = null
  }
  def loadTemplate = {
    operationStatements = new java.util.ArrayList[OperationStatement]
    opStArray = templateCSON.get.getElement("statement").asInstanceOf[CSONElementArray]
    val statements = templateCSON.get.getElement("statement").asInstanceOf[CSONElementArray].iterator
    statements.foreach((opElement:IElementValue)=>
      operationStatements.add(reqMsgSerializer.getObject(opElement).asInstanceOf[OperationStatement]))
  }
  def replaceOperationParams(operationIndex:Int, tenantID:Int, replaceParams:OperationParams) = {
    def replaceArgList(opStatement:OperationStatement,argList:Array[Object]) = {
      val binaryArgList = new Array[Binary](argList.length)

      for (i<-0 until argList.length) binaryArgList(i)= GeneralEntityToCSON.getRawValue(argList(i), true)
      opStatement.argList= binaryArgList
    }
    if (operationStatements==null) loadTemplate

    val opStatement = operationStatements.get(operationIndex)

    if (opStatement.queryMethod != replaceParams.operationType)
      throw new Exception("Operation type not match, it's not valid to be replaced.")

    else opStatement.queryMethod match {
      case QueryMethod.Query => {
        if (replaceParams.argumentList.isInstanceOf[Array[Binary]])
          opStatement.argList = replaceParams.argumentList.asInstanceOf[Array[Binary]]
        else replaceArgList(opStatement, replaceParams.argumentList)
      }
      case QueryMethod.Insert => {
        val insertStatement = opStatement.asInstanceOf[InsertStatement]
        insertStatement.tenantID = tenantID
        insertStatement.paramdata = replaceParams.dataParams
        insertStatement.records(0).key = replaceParams.P_Key
        insertStatement.records(0).data = replaceParams.entity2Insert
      }
      case QueryMethod.Update => {
        val updateStatement = opStatement.asInstanceOf[UpdateStatement]
        updateStatement.tenantID = tenantID
        updateStatement.paramdata = replaceParams.dataParams
        if (replaceParams.argumentList != null && replaceParams.argumentList.length > 0)
          replaceArgList(opStatement, replaceParams.argumentList)
        else updateStatement.primaryKey = replaceParams.P_Key
      }
      case QueryMethod.Delete => {
        val delStatement = opStatement.asInstanceOf[DeleteStatement]
        delStatement.tenantID = tenantID
        delStatement.paramdata = replaceParams.dataParams
        delStatement.keys = replaceParams.delKeys
      }
    }
  }
  def saveOperationTemplate(transactionType:TxType,s:RequestMessage,csonBuffer:ByteBuffer):CSONDocument =
    reqMsgSerializer.writeObjectToCSON(s,csonBuffer)._1.asInstanceOf[CSONDocument]
}
trait IStoreAccessor {
  import OutputMode._
  
  type Binary = Array[Byte]
  def beginTx()
  def endTx(transactionType:TxType=SingleInstanceTx)
  def insert(entity2Insert:Binary,entitySetName:String, schemaId:String, tenantId:Int, P_Key:Binary = null, 
        operationJS:String=null, preOpJS:String = null,  DataParams:Array[Binary] = null,
        output:OutputMode = ToClient, contextIndex:Array[Int] = null)
  def batchInsert(entities:Array[Binary], primaryKeys:Array[Binary], entitySetName:String, schemaId:String,
                  tenantID:Int,output:OutputMode = ToClient, contextIndex:Array[Int] = null)
  def update(updateData:Binary, P_Key:Binary, entitySetName:String, schemaId:String, tenantId:Int)
  def update(updateData:Binary, updateCondition:Condition, entitySetName:String, schemaId:String, tenantId:Int)
  def updateProperty(properties:Array[Tuple2[String,Binary]],P_Key:Binary, entitySetName:String, schemaId:String,
                     tenantId:Int,preOperationJS:String = null)
  def customUpdate(P_Key:Binary, preOpJS:String, JScript:String, entitySetName:String, schemaId:String,tenantId:Int,
            queryCommand:Object = null, queryArgList:Array[Binary],dataParams:Array[Binary] = null,
            output:OutputMode = Non, contextIndex:Array[Int] = null)
  def load(loadCondition:Condition, entitySetName:String, tenantId:Int, P_Key:Binary = null,
           output:OutputMode = ToClient,contextIndex:Array[Int]=null)
  def query(queryCommand:Object, output:OutputMode = ToClient,contextIndex:Array[Int]=null)
  def delete(delKeys:Array[Binary], entitySetName:String, schemaId:String, tenantId:Int,
             preOpJS:String = null, dataParams:Array[Binary] = null,contextIndex:Array[Int]=null)
  def waitForResult:Any
}
// StoreAccessor interface can wrap all operations in a session and save it as template. We can load this saved template next time and just provide
// a parameter list. This is the Class definition for params.
class OperationParams {
  import QueryMethod._
  type Binary = Array[Byte]
  var operationType:QueryMethod = Query
  var argumentList:Array[Object] = _
  var entity2Insert:Array[Byte] = _
  var P_Key:Array[Byte] = _
  var delKeys:Array[Binary] = _
  var dataParams:Array[Binary] = _
}