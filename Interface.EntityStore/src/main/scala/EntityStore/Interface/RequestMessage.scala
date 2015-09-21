package EntityStore.Interface
import EntityInterface._
import EntityAccess._

object OperationScope extends Enumeration {
  type OperationScope = Value
  val EntitySet = Value(0)
  val Transaction = Value(1)
  val System = Value(2)
  val Security = Value(3)
}
object QueryMethod extends Enumeration {
  type QueryMethod = Value
  val Query = Value(0)
  val Insert = Value(1)
  val Update = Value(2)
  val Delete = Value(3)
}
object TxType extends Enumeration {
  type TxType = Value
  val NA = Value(0)
  val TCCTranscation = Value(1)
  val SingleInstanceTx = Value(2)
}
object OutputMode extends Enumeration{ 
  type OutputMode = Value
  val ToClient = Value(0)
  val ToContext = Value(1) 
  val ToBoth = Value(2) 
  val Non = Value(3) 
}
class RequestHeader {
  import TxType._
  import OperationScope._
  /**
   * Version number policy : for every update, version number increases 1, the initial number is 9 (which means version 0.9).
   */
  var version : Int = 9
  var operatingScope:OperationScope = EntitySet
  var trxType : TxType = NA
}
class OperationStatement {
  import QueryMethod._
  import OutputMode._
  type Binary = Array[Byte]
  var queryMethod:QueryMethod = Query
  var cacheID:java.util.UUID = null
  var argList:Array[Binary] = null
  var outputTarget:OutputMode = ToClient // 0 : to client, 1 : to context, 2 : both,  3 : none
  var contextRef:Array[Int] = null
  def schemaChooser(value:Object):IEntitySchema = QueryMethod(value.asInstanceOf[Int]) match {
                              case Query => OperationStatement.queryStatementSchema
                              case Insert => OperationStatement.insertStatementSchema
                              case Update => OperationStatement.updateStatementSchema
                              case Delete => OperationStatement.deleteStatementSchema
                            }
}
class RequestMessage {
  var header:RequestHeader = new RequestHeader
  var statement:Array[OperationStatement] = null
  var cacheID:java.util.UUID = null
}
object OperationStatement {
  val queryStatementSchema = GeneralEntitySchema(classOf[QueryStatement])
  val insertStatementSchema = GeneralEntitySchema(classOf[InsertStatement])
  val updateStatementSchema = GeneralEntitySchema(classOf[UpdateStatement])
  val deleteStatementSchema = GeneralEntitySchema(classOf[DeleteStatement])
}