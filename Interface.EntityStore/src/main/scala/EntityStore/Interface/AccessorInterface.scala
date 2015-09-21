package EntityStore.Interface
import EntityInterface.OrderByType.OderByType
import EntityInterface._

class StoreOperation()
case class EntityQuery(queryStatement:String,args:Array[Object],select:String,alias:String,appSpace:String,
                       entitySetName:String,schema:IEntitySchema,tenantId:Int=0,reqID:Int=0) extends StoreOperation
case class LoadEntityByKey(pKey:Object,appSpace:String,entitySetName:String,
                           returnSchema:IEntitySchema,tenantId:Int=0,reqID:Int=0) extends StoreOperation
case class CreateEntity(pKey:Object,data:Array[Byte],appSpace:String,
                        schema:IEntitySchema,setName:String,tenantId:Int=0,reqID:Int=0) extends StoreOperation
case class UpdateEntity(pKey:Object,data:Array[Byte],appSpace:String,
                        schema:IEntitySchema,setName:String,tenantId:Int=0,reqID:Int=0) extends StoreOperation
case class DelEntity(pKey:Object,appSpace:String,schema:IEntitySchema,setName:String,
                        tenantId:Int=0,reqID:Int=0) extends StoreOperation
/*class SystemQueryRequest(queryStatement:String,args:Array[Object]=null) extends IQueryRequest {
  import OrderByType._

  override def filters : String = queryStatement
  override def params : Array[Object] = args
  override def orderByColumns : Array[String] = null
  override def orderByTypes : Array[OderByType] = null
  override def selectColumns : String = null
  override def groupBy  : String = null
  override def groupValue : String = null
  override def topCount : Int = -1

}*/
class SystemRequestContext(aliasId:String,appSpaceId:String,uid:String,role:String,tId:Int,reqId:Int,oId:String="0")
  extends IRequestContext {
  def alias : String = aliasId
  def appSpaceID : String = appSpaceId
  def userID : String = uid
  def tenantID : Int = tId
  def operationID : String = oId
  def requestID : Int = reqId
  def userRole : String = role
}
class SystemQueryRequest(query:String,select:String=null,p:Array[Object]=null,gBy:String=null,gVal:String=null,
                         oBy:Array[String]=null,oByTypes:Array[OderByType]=null,top:Int=10000) extends IQueryRequest{
  override def filters: String = query
  override def orderByTypes: Array[OderByType] = oByTypes
  override def selectColumns: String = select
  override def groupBy: String = gBy
  override def groupValue: String = gVal
  override def topCount: Int = top
  override def orderByColumns: Array[String] = oBy
  override def params: Array[Object] = p
}
