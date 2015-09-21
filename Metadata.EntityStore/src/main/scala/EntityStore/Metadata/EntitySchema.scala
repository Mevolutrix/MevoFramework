package EntityStore.Metadata
import CSON.Types._
import EntityInterface._
import scala.collection.mutable.HashMap
import EntityInterface.DefaultValueType._
import EntityInterface.IndexType
import EntityAccess.JSONSerializer

/**
 * metadata for an Entity，it's a definition of sub-class or root-class of this EntitySet
 * 此数据集中某Class的Schema定义（但索引信息取自实体集合定义）
 */
class EntitySchema extends IEntitySchema with IEntitySet {
  import EntityType._
  import StorageType._
  import IndexType._
  private class LocalVars{var entitySet:EntitySet = null}
  var id:String = _             // appSpace+Entity short name
  var appSpaceId:String = _
  var entitySetName:String = _  // appSpace+Set's pure name
  var entityName:String = _     // Should be equal to full class name
  var description:String = _
  var version:String = "1.0"
  var properties:Array[EntityProperty] = new Array[EntityProperty](0)
  //Type and mode for this schema: Data=Entity has information need persistence;View= DTO or result type
  var entityType:EntityType=EntityType.View
  var status:Int=1
  private val privateStore = new LocalVars
  private val propertyMap:HashMap[String,Int] = new HashMap[String,Int]
  private def getPropInfo(property:Any):EntityProperty = property match {
    case i:Int => properties(i)
    case p:String => properties(getID(p))
  }
  private def getPropertyInfo(propType:Class[_],pName:String,
                              ctPropInfo:Array[(String,IEntitySchema)]):EntityProperty = {
    val ret = new EntityProperty()
    ret.name = pName
    ret.pType = if (pName=="[B")
      CSONTypes.BinaryData.id.toByte
    else if(propType.isArray) {
      ret.isArray = true
      val elementType:Byte = CSONTypesArray.typeCodeOf(propType.getComponentType)
      if (elementType < 0) {
        // ComplexElement
        ret.complexTypeName = propType.getComponentType.getSimpleName
        ret.initComplexType(ctPropInfo.iterator.find(p => (p._1 == ret.complexTypeName)).get._2.asInstanceOf[EntitySchema])
        CSONTypes.Array.id.toByte
      }else elementType
    } else {
      val elementType:Byte = CSONTypesArray.typeCodeOf(propType)
      if (elementType < 0) { // ComplexElement
        ret.complexTypeName = propType.getSimpleName
        ret.initComplexType(ctPropInfo.iterator.find(p=>(p._1==ret.complexTypeName)).get._2.asInstanceOf[EntitySchema])
        CSONTypes.EmbeddedDocument.id.toByte
      }
      else { //SimpleElement
        ret.nullable = false
        elementType
      }
    }
    ret
  }
/*  private def appendProperties(p:EntityProperty):Unit = {
    val count = properties.length
    propertyMap.put(p.name,count)
    val newPropArray = new Array[EntityProperty](count+1)
    Array.copy(properties,0,newPropArray,0,count)
    newPropArray(count) = p
    properties = newPropArray
  }*/
  def populate(eType:EntityType=EntityType.Data) = {
    entityType = eType
    if (privateStore.entitySet==null && entityType == EntityType.Data && entitySetName!=null)
      privateStore.entitySet = MetadataManager.getEntitySet(entitySetName).asInstanceOf[EntitySet]

    if (propertyMap.size<=0)
      for (i <- 0 until properties.length) {
        propertyMap.put(properties(i).name, i)
        properties(i).initComplexType(appSpaceId,properties(i).complexTypeName)
      }
    this
  }
  def sbePopulate(eType:EntityType=EntityType.Data) = {
    entityType = eType
    if (privateStore.entitySet==null && entityType == EntityType.Data && entitySetName!=null)
      privateStore.entitySet = SBEMetadata.getEntitySet(entitySetName).asInstanceOf[EntitySet]

    if (propertyMap.size<=0)
      for (i <- 0 until properties.length) {
        propertyMap.put(properties(i).name, i)
        properties(i).initComplexType(appSpaceId,properties(i).complexTypeName)
      }
    this
  }

  def this(set:EntitySet,appSpaceName:String,props:Array[String], classType:Class[_],ctPropInfo:Array[(String,IEntitySchema)]) {
    this()
    entityName = classType.getName
    if (set!=null) {
      appSpaceId = set.appSpaceId
      entityType=EntityType.Data
      entitySetName = set.setName
      privateStore.entitySet = set
    }else {appSpaceId = appSpaceName}
    description = entityName
    id = appSpaceId+"."+classType.getSimpleName
    properties = new Array[EntityProperty](props.length)
    for (i<-0 until props.length) {
      propertyMap.put(props(i), i)
      properties(i) = getPropertyInfo(classType.getDeclaredField(props(i)).getType,props(i),ctPropInfo)
    }
  }
  //#Implement IEntitySet
  def appSpace : String = appSpaceId
  def setName : String = entitySetName
  def primaryKey : String = if (privateStore.entitySet!=null) privateStore.entitySet.primaryKey else null
  def storageType : StorageType =
    if (privateStore.entitySet!=null) privateStore.entitySet.storageType else StorageType.NoStore
  def indexProperties:Array[String] =
    if (privateStore.entitySet!=null) privateStore.entitySet.indexProperties else null
  def defaultValueProperties : Array[String] =
    if (privateStore.entitySet!=null) privateStore.entitySet.defaultValueProperties else null
  def isIndex(propertyName : String) : Boolean =
    if (privateStore.entitySet!=null) privateStore.entitySet.isIndex(propertyName) else false
  def getIndexType(propertyName : String) : IndexType =
    if (privateStore.entitySet!=null) privateStore.entitySet.getIndexType(propertyName)
    else IndexType.Default
  def getDefaultValueType(propertyName:String) : DefaultValueType =
    if (privateStore.entitySet!=null)
      privateStore.entitySet.getDefaultValueType(propertyName)
    else DefaultValueType.Non
  def getDefaultValueDefinition(propertyName:String) : String =
    if (privateStore.entitySet!=null)
      privateStore.entitySet.getDefaultValueDefinition(propertyName)
    else null
  def getAutomaticProperty : String =
    if (privateStore.entitySet!=null) privateStore.entitySet.getAutomaticProperty else null
  def getPrimaryKeyMin : String =
    if (privateStore.entitySet!=null) privateStore.entitySet.getPrimaryKeyMin else null
  //#Implement IEntitySet
  def getGZIPPropertyName:String =
    if (privateStore.entitySet!=null) privateStore.entitySet.gzipProperty else null
  //# Implement indicate whether no _raw_DATA_ (true)
  def isFlat:Boolean = if (privateStore.entitySet!=null)privateStore.entitySet.isFlat else false
  //#Implement IEntitySchema
  def entityId : String =id
  def count : Int = properties.length
  def schemaType : EntityType =entityType
  def containsProperty(property:String):Boolean = propertyMap.contains(property)
  def objType : Class[_] = {
    if (entityName!=null) try {
      Class.forName(entityName)
    }
    catch {
      case e:ClassNotFoundException => null
    }
    else null
  }
  def getID(propertyName:String):Int = propertyMap.get(propertyName).getOrElse({
    throw new IllegalArgumentException("Incorrect schema property name:"+propertyName);-1})
  def getPropertyName(id:Int):String = properties(id).name
  def getTypeCode(property:Any):Byte = getPropInfo(property).pType
  def getElementType(property:Any):IElementType = {
    val propInfo = getPropInfo(property)
    val propElementTypeCode: Byte = propInfo.pType
    if (propInfo.isArray) {
      if (propInfo.complexTypeName!=null) return new CSONArrayType(propInfo.complexType)
      //  如果此属性为数组类型，且不是复杂类型的成员，则Property的pType字段表明了数组中的成员的类型
      else  new CSONArrayType(CSONTypesArray.CSONElementTypes(propElementTypeCode))
    }
    else {
      if (propInfo.complexTypeName!=null) return new CSONComplexType(propInfo.complexType)
      else CSONTypesArray.CSONElementTypes(propElementTypeCode)
    }
  }
  //#region Operations
  /*def getProperty(name:String):EntityProperty = {
    def getSubProperty(childSchema: EntitySchema, name: Array[String], index: Int): EntityProperty =
      if (index >= name.length - 1) childSchema.getProperty(name(index))
      else getSubProperty(childSchema.getProperty(name(index)).complexType, name, index + 1)

    val propNames = name.split("/")
    if (propNames.length == 1) getPropInfo(name)
    else getSubProperty(getPropInfo(propNames(0)).complexType, propNames, 1)
  }

  def addOrUpdateProperty(p:EntityProperty):Unit = {
    if (p != null) {
      val pos:Int = propertyMap.get(p.name).getOrElse( {
        appendProperties(p)
        properties.length-1
      } )
      properties(pos) = p
    }
  }*/
  def clonePropertyMap(source:HashMap[String,Int]):EntitySchema = {
    source.foreach(f=>propertyMap.put(f._1,f._2))
    this}
}
object EntitySchema {
  import EntityType._
  val system_AppSpace = MetadataManager.system_AppSpace
  val validationRuleSetInfo = (new EntitySet {
    entitySetName=system_AppSpace+".ValidationRule"
    appSpaceId=system_AppSpace
    description="EntitySet for Validation Rule"
    pKey="name"
    pkeyType=CSONTypes.UTF8String.id.toByte
    index=Array[EntityQueryMark]()
    _storageType=StorageType.Shared
    autoValue=""
    minValueOfPKey = ""
  }).populate
  val validationRuleSchema:IEntitySchema = new EntitySchema(validationRuleSetInfo,system_AppSpace,
    Array[String]("name","invokeType","pattern","errMsg"),
    classOf[ValidationRule],null).populate()
  val propertySchema:IEntitySchema = new EntitySchema(null,system_AppSpace,Array[String]("name","description",
    "pType","complexTypeName","nullable","isArray","verificationRegEx"),
    classOf[EntityProperty],Array[(String,IEntitySchema)](
      ("ValidationRule",validationRuleSchema) )).populate(EntityType.ComplexType)
  val setInfo: IEntitySet = (new EntitySet {
    entitySetName=system_AppSpace+".EntitySchema"
    appSpaceId=system_AppSpace
    description="EntitySet for Schema metadata"
    pKey="id"
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
    Array[String]("id","appSpaceId","entitySetName","entityName","description","version",
      "properties","entityType","status"),classOf[EntitySchema],Array[(String,IEntitySchema)](
      ("EntityProperty",propertySchema) )  ).populate()
  val metaInfo:SetMetadata = new SetMetadata {
    setName=setInfo.setName
    appSpaceId=system_AppSpace
    baseSchemaName=schemaInfo.entityId
    entitySchemaList=Array[Entity_Metadata](new Entity_Metadata{id="0";schemaName=baseSchemaName;isBaseSchema=true})
  }
  val validationRulMetaInfo:SetMetadata = new SetMetadata {
    setName=validationRuleSetInfo.setName
    appSpaceId=system_AppSpace
    baseSchemaName=validationRuleSchema.entityId
    entitySchemaList=Array[Entity_Metadata](new Entity_Metadata{id="0";schemaName=baseSchemaName;isBaseSchema=true})
  }
  def clone(source: EntitySchema, schemaObjType: EntityType): EntitySchema = {
    (new EntitySchema {
      id = source.id
      appSpaceId = source.appSpaceId
      entitySetName = source.entitySetName
      entityName = source.entityName
      description = source.description
      version = source.version
      properties = source.properties
      entityType = schemaObjType
      status = source.status
    }).clonePropertyMap(source.propertyMap)
  }
  def apply(jsonData:String):IEntitySchema =
    JSONSerializer.unapply(jsonData,classOf[EntitySchema]).asInstanceOf[EntitySchema]
}
