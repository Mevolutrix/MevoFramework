package EntityStore.Metadata
import EntityInterface._
import CSON.Types.CSONTypes
import EntityAccess.JSONSerializer
import scala.collection.mutable.HashMap
import IndexType._

class EntitySet extends IEntitySet {
import StorageType._
import DefaultValueType._

  var entitySetName:String=_
  var appSpaceId:String=_
  var description:String=_
  var pKey:String=_
  var pkeyType:Byte=0
  var index:Array[EntityQueryMark]=null
  var _storageType:StorageType=StorageType.NoStore
  var defaultValues:Array[PropertyDefaultValue] = null
  var autoValue:String=_
  // Give the least value for Primary Key type. eg. String type is "", Int type is -65535
  var minValueOfPKey:String =_
  // User must encoding the data in BASE64(to compatible with some HTML tag/JSON Value)
  // Then the data will be compressed with GZIP and then BASE64 encoded
  var gzipProperty:String = _
  var setIsFlat:Boolean = false

  //# private dictionary and help function
  private val indexMap:HashMap[String,Int] = new HashMap[String,Int]
  private val defaultValueMap:HashMap[String,Int] = new HashMap[String,Int]
  //# private dictionary and help function

  def populate = {
    if (indexMap.size<=0)
      for(i<-0 until (if(index==null) 0 else index.length)) indexMap.put(index(i).path,i)

    if (defaultValueMap.size<=0)
      for(i<-0 until (if(defaultValues==null) 0 else defaultValues.length))
        defaultValueMap.put(defaultValues(i).name,i)
    this
  }
  //#Implement IEntitySet
  def appSpace : String = appSpaceId
  def setName : String = entitySetName
  def primaryKey : String = pKey
  def storageType : StorageType = _storageType
  def indexProperties:Array[String] = {
    val ret = new Array[String](index.length)
    for(i<-0 until index.length) ret(i)=index(i).path
    ret
  }
  def defaultValueProperties : Array[String] = defaultValueMap.keys.toArray
  def isIndex(propertyName : String) : Boolean = indexMap.contains(propertyName)
  def getIndexType(propertyName : String) : IndexType =
    if (indexMap.contains(propertyName))
      index(indexMap.get(propertyName).get).markType
    else IndexType.Default
  def getIndexPropertyTypeCode(propertyName : String) =
    if (indexMap.contains(propertyName))
      index(indexMap.get(propertyName).get).indexDataType
    else if (propertyName==primaryKey) pkeyType
    else throw new IllegalArgumentException("Get index typeCode with wrong property name:"+propertyName)

  def getDefaultValueType(propertyName:String) : DefaultValueType =
    if (defaultValueMap.contains(propertyName))
      defaultValues(defaultValueMap.get(propertyName).get).dType
    else DefaultValueType.Non
  def getDefaultValueDefinition(propertyName:String) : String =
    if (defaultValueMap.contains(propertyName)) {
      val theConfig = defaultValues(defaultValueMap.get(propertyName).get)
      if (theConfig.dType.id==DefaultValueType.Preset.id)
        theConfig.defaultValue
      else theConfig.evalExp
    }
    else null

  def getAutomaticProperty : String = autoValue
  def getPrimaryKeyMin:String = minValueOfPKey
  def getGZIPPropertyName:String = gzipProperty
  def isFlat:Boolean = setIsFlat
  //#Implement IEntitySet

  /*def addDefaultValue(propertyDefaultValue:PropertyDefaultValue):Unit = {
    if (propertyDefaultValue != null) {
      val count = defaultValues.length
      if (defaultValueMap.contains(propertyDefaultValue.name)) { // Update Default value record
        defaultValues(defaultValueMap.get(propertyDefaultValue.name).get) = propertyDefaultValue
      } else {  // 添加Default Value
      var newDefaultValues = new Array[PropertyDefaultValue](count + 1)
        Array.copy(defaultValues,0,newDefaultValues,0,count)
        newDefaultValues(count) = propertyDefaultValue
        defaultValues = newDefaultValues
      }
    }
  }*/
}
class EntityQueryMark {
  var path:String=_
  var direct:Boolean=false
  var markType:IndexType=IndexType.Default
  var indexDataType:Byte=0
  var expression:String=_
}
object EntitySet {
  val system_AppSpace = MetadataManager.system_AppSpace
  val IndexSchema:IEntitySchema = new EntitySchema(null,system_AppSpace,Array[String]("path","direct","markType",
    "indexDataType","expression"),classOf[EntityQueryMark],null).populate(EntityType.ComplexType)
  val defaultValueSchema:IEntitySchema = new EntitySchema(null,system_AppSpace,Array[String]("name",
    "dType","defaultValue","evalExp"),classOf[PropertyDefaultValue],null).populate(EntityType.ComplexType)
  val setInfo: IEntitySet = (new EntitySet {
    entitySetName=system_AppSpace+".EntitySet"
    appSpaceId=system_AppSpace
    description="EntitySet for EntitySet metadata"
    pKey="entitySetName"
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
  val schemaInfo: IEntitySchema = new EntitySchema(setInfo.asInstanceOf[EntitySet],system_AppSpace,
    Array[String]("entitySetName","appSpaceId","description","pKey","pkeyType","index",
      "_storageType","defaultValues","autoValue","minValueOfPKey","gzipProperty","setIsFlat"),
      classOf[EntitySet],Array[(String,IEntitySchema)](("EntityQueryMark",IndexSchema),
                                                       ("PropertyDefaultValue",defaultValueSchema))).populate()
  val metaInfo:SetMetadata = new SetMetadata {
    setName=setInfo.setName
    appSpaceId=system_AppSpace
    baseSchemaName=schemaInfo.entityId
    entitySchemaList=Array[Entity_Metadata](new Entity_Metadata{id="0";schemaName=baseSchemaName;isBaseSchema=true})
  }
  def apply(jsonData:String):IEntitySet =
    JSONSerializer.unapply(jsonData,classOf[EntitySet]).asInstanceOf[EntitySet]
}