package EntityStore.Metadata

import CSON.Types.CSONTypes
import EntityInterface._

object StoreType extends Enumeration {
  type StoreType = Value
  val cacheType = Value(1)
  val entityGrid = Value(2)
  val dataBase = Value(3)
}
class AppSpaceInfo {
  var alias: String = _
  var appSpaceName: String = null
  var storeConfig: StoreConfiguration = null
}
class StoreConfiguration {
import StoreType._
  var hostName:String = null
  var storeType:StoreType = null
  var dbConnString:String = null
  var usr:String = null
  var pwd:String = null
}

object AppSpaceInfo {
  val system_AppSpace = MetadataManager.system_AppSpace
  val storeConfigSchema:IEntitySchema = new EntitySchema(null,system_AppSpace,Array[String]("hostName",
    "storeType","dbConnString","usr","pwd"), classOf[StoreConfiguration],null).populate(EntityType.ComplexType)
  val setInfo: IEntitySet = (new EntitySet {
    entitySetName=system_AppSpace+".AppSpaceInfo"
    appSpaceId=system_AppSpace
    description="EntitySet for AppSpace info and storage config"
    pKey="alias"
    pkeyType=CSONTypes.UTF8String.id.toByte
    index=Array[EntityQueryMark](new EntityQueryMark() {
      path="appSpaceName"
      direct=true
      markType=IndexType.Default
      indexDataType=pkeyType
    })
    _storageType=StorageType.Shared
    autoValue=""
    minValueOfPKey = ""
  }).populate
  val appSpaceConfigSchemaInfo: IEntitySchema = new EntitySchema(setInfo.asInstanceOf[EntitySet],system_AppSpace,
    Array[String]("alias","appSpaceName","storeConfig"),classOf[AppSpaceInfo],Array[(String,IEntitySchema)](
      ("StoreConfiguration",storeConfigSchema) )  ).populate()
  val metaInfo:SetMetadata = new SetMetadata {
    setName=setInfo.setName
    appSpaceId=system_AppSpace
    baseSchemaName=appSpaceConfigSchemaInfo.entityId
    entitySchemaList=Array[Entity_Metadata](new Entity_Metadata{id="0";schemaName=baseSchemaName;isBaseSchema=true})
  }
  val storeConfigSchemaInfo = new EntitySchema(setInfo.asInstanceOf[EntitySet],system_AppSpace,
    Array[String]("hostName","storeType","dbConnString","usr","pwd"),classOf[StoreConfiguration],null).populate()
}