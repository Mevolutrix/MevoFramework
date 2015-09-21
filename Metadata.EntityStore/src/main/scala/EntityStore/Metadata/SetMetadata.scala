package EntityStore.Metadata
import java.util
import java.util.Collection
import CSON.CSONDocument
import CSON.Types.CSONTypes
import EntityAccess.GeneralEntityToCSON
import EntityInterface._
import EntityStore.Interface.{UpdateEntity, CreateEntity, ResultFormat, LoadEntityByKey}
import SBEServiceAccess.CallDSE

class SetMetadata extends ISetMetadata {
  var setName:String=_
  var appSpaceId:String=_
  var baseSchemaName:String=_
  var entitySchemaList:Array[Entity_Metadata]=_
  def getName:String = setName
  def baseSchema:IEntitySchema = MetadataManager.getSchema(baseSchemaName)
  def supportSchemas:Iterator[IEntitySchema]=(for (s<-entitySchemaList.iterator) yield s.schema)
  def populate():Unit = entitySchemaList.iterator.foreach(_.populate(appSpaceId))
}
class Entity_Metadata {
  class LocalStore {var schema:IEntitySchema=null }
  var id:String=_
  var schemaName:String=_
  var isBaseSchema:Boolean=false
  private val _schema=new LocalStore
  def schema = _schema.schema
  def populate(appSpace:String):Unit = _schema.schema=MetadataManager.getSchema(schemaName)
}
object SetMetadata {
  val setInfo: IEntitySet = (new EntitySet{
    entitySetName=EntitySet.system_AppSpace+".SetMetadata"
    appSpaceId=EntitySet.system_AppSpace
    description="Schema inheritance metadata for EntitySet"
    pKey="setName"
    pkeyType=CSONTypes.UTF8String.id.toByte
    index=Array[EntityQueryMark](new EntityQueryMark() {
      path="appSpaceId"
      direct=true
      markType=IndexType.Default
      indexDataType=pkeyType
    })
    _storageType=StorageType.Shared
    autoValue=""
    minValueOfPKey = ""
  }).populate
  val Entity_MetaSchema: IEntitySchema = new EntitySchema(null,EntitySet.system_AppSpace,
    Array[String]("id","schemaName","isBaseSchema"),classOf[Entity_Metadata],
    Array[(String,IEntitySchema)]()).populate()
  val schemaInfo: IEntitySchema = new EntitySchema(setInfo.asInstanceOf[EntitySet],EntitySet.system_AppSpace,
    Array[String]("setName","appSpaceId","baseSchemaName","entitySchemaList"),
    classOf[SetMetadata],Array[(String,IEntitySchema)](("Entity_Metadata",Entity_MetaSchema))).populate()
/*  private val entityMetadataCache = new FutureCache[String,SetMetadata](loadSetMetadata)
  private val serializer = GeneralEntityToCSON(classOf[SetMetadata])
  private def getResult(input:Any):Boolean = input match {
    case a: util.ArrayList[CSONDocument] => true
    case _ => false
  }
  private def loadSetMetadata(setName:String):SetMetadata = {
    (setName match {
      case "System.Metadata.EntitySet" => EntitySet.metaInfo
      case "System.Metadata.EntitySchema" => EntitySchema.metaInfo
      case _ =>
        val ret = CallStore(LoadEntityByKey(setName,EntitySet.system_AppSpace,setInfo.setName,
        schemaInfo)).asInstanceOf[util.ArrayList[CSONDocument]]
        serializer.getObject(ResultFormat.getCSONResult(ret,schemaInfo)(0))
    }).asInstanceOf[SetMetadata]
  }
  def apply(setName:String):SetMetadata = entityMetadataCache.get(setName)
  def create(setName:String,metaInfo:SetMetadata) = {
    entityMetadataCache.set(setName,metaInfo)
    val insertItem = serializer.getObjectCsonBinary(metaInfo,null)._1
    getResult(CallStore(CreateEntity(setName,insertItem,EntitySet.system_AppSpace,schemaInfo,setInfo.setName)))
  }
  def update(setName:String,metaInfo:SetMetadata,updator:(SetMetadata)=>SetMetadata=null) = {
    val updateItem = if (metaInfo != null) metaInfo
    else updator(entityMetadataCache.get(setName))
    entityMetadataCache.set(setName, updateItem)
    val item = serializer.getObjectCsonBinary(updateItem, null)._1

    getResult(CallStore(UpdateEntity(setName, item, EntitySet.system_AppSpace, schemaInfo, setInfo.setName)))
  }*/
}