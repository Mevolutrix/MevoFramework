package EntityStore.Metadata
import java.util
import EntityAccess._
import CSON.CSONDocument
import CSON.Types.CSONTypes
import SBEServiceAccess.CallDSE
import EntityStore.Interface.{ResultFormat, EntityQuery, CreateEntity}
import EntityInterface.{IEntityRandomAccess, IEntitySchema, StorageType, IndexType}

object MetadataOperationMethod extends Enumeration {
  type MetadataOperationMethod = Value
  val add = Value(1)
  val update = Value(2)
  val delete = Value(3)
}
object SetUpdateType extends Enumeration {
  type SetUpdateType = Value
  val set = Value(1)
  val index = Value(2)
  val both = Value(3)
}
class SetUpdateRecord {
  import SetUpdateType._
  import MetadataOperationMethod._
  var id:Long = 0L
  var appSpace:String = _
  var updateObject:SetUpdateType = set
  var updateMethod:MetadataOperationMethod = add
  var setName:String = _
  var indexUpdated:Array[EntityQueryMark] = null   // Only valid while updateObject == "index"
  var pKey: String = null
  var primaryKeyType:Byte = 0
  var setIsFlat:Boolean = false
  def genKey = { id=(new java.util.Date()).getTime;id }
}

object SetUpdateRecord {
  val system_AppSpace = MetadataManager.system_AppSpace
  val typeInfo = classOf[SetUpdateRecord]
  val schemaOfUpdateRecord = GeneralEntitySchema(classOf[SetUpdateRecord])
  val uRecordSerializer = GeneralEntityToCSON(classOf[SetUpdateRecord])
  val setInfo = (new EntitySet() {
    entitySetName = system_AppSpace + ".SetUpdateRecord"
    appSpaceId = system_AppSpace
    description = "EntitySet for set update record"
    pKey = "id"
    pkeyType = CSONTypes.Int64.id.toByte
    index = Array[EntityQueryMark](new EntityQueryMark() {
      path = "appSpace"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.UTF8String.id.toByte
    },new EntityQueryMark() {
      path = "setName"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.UTF8String.id.toByte
    })
    _storageType = StorageType.Shared
    autoValue = ""
    minValueOfPKey = "-1"
  }).populate
  val schemaInfo = new EntitySchema(setInfo.asInstanceOf[EntitySet],system_AppSpace,
    Array[String]("id","appSpace","updateObject","updateMethod","setName","indexUpdated",
      "pKey","primaryKeyType","setIsFlat"),classOf[SetUpdateRecord],Array[(String,IEntitySchema)](
      ("EntityQueryMark",EntitySet.IndexSchema) )  ).populate()
  def insertSet(appSpaceName:String,set_Name:String,set:EntitySet) = {
    val updateRecord = new SetUpdateRecord {
      appSpace = appSpaceName
      updateObject = SetUpdateType.both
      setName = set_Name
      indexUpdated = set.index
      pKey = set.primaryKey
      primaryKeyType = set.pkeyType
      setIsFlat = set.setIsFlat
    }
    val pKey = updateRecord.genKey.asInstanceOf[Object]
    val binary = uRecordSerializer.getObjectCsonBinary(updateRecord,null)._1
    CallDSE(CreateEntity(pKey,binary, system_AppSpace,schemaInfo,setInfo.setName)) match {
      case a:util.Collection[IEntityRandomAccess] => true
      case _ => false
    }
  }
  def getSetUpdateInfo(appSpace:String):IndexedSeq[CSONDocument] = {
    val serializer = GeneralEntityToCSON(typeInfo)
    val schema = GeneralEntitySchema(typeInfo)
    CallDSE(EntityQuery("appSpace=@0",Array[Object](appSpace),null,"MDE",system_AppSpace,
      schemaInfo.setName,schemaInfo)) match {
      case retList: util.ArrayList[CSONDocument] =>
        ResultFormat.getCSONResult(retList, schema)
      case _ => new Array[CSONDocument](0)
    }
  }
}