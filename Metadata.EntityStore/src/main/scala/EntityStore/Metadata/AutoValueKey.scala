package EntityStore.Metadata

import CSON.Types.CSONTypes
import EntityInterface.{IndexType, StorageType}

/**
 * General implementation for auto_increment key
 */
class AutoValueKey {
  var fullSetName:String = null
  var intAutoValue:Int = 0
  var longAutoValue:Long = 0L
}

object AutoValueKey {
  val system_AppSpace = "System.Configuration"
  val autoKeySetInfo = (new EntitySet {
    entitySetName=system_AppSpace + ".AutoValueKey"
    appSpaceId=system_AppSpace
    description="EntitySet for each set's autoKey record"
    pKey="fullSetName"
    pkeyType=CSONTypes.UTF8String.id.toByte
    index=Array[EntityQueryMark](new EntityQueryMark(){
      path = "intAutoValue"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.Int32.id.toByte
    },new EntityQueryMark(){
      path = "longAutoValue"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.Int64.id.toByte} )
    defaultValues = Array[PropertyDefaultValue](new PropertyDefaultValue() {})
    _storageType=StorageType.Shared
    autoValue=""
    minValueOfPKey = ""
    gzipProperty = null
    setIsFlat = true
  }).populate
  val autoKeySchemaInfo = new EntitySchema(autoKeySetInfo,system_AppSpace,
    Array[String]("fullSetName","intAutoValue","longAutoValue"),
    classOf[AutoValueKey],null).populate()
}