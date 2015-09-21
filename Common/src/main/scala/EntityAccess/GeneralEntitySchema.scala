package EntityAccess
import EntityInterface._
import scala.reflect._
import java.util.concurrent.ConcurrentHashMap
import CSON.Types._
import EntityType._
import java.lang.reflect.{ Field,Method }
import CSON._
/**
 *  Super DTO class with child classes, should implement a function named "schemaChooser" for get correct schema and serializer 
 */
class GeneralEntitySchema(objectType:Class[_]) extends IEntitySchema {
  private val entityName = objectType.getName()
  private val entityFieldInfo = EntitySchemaCache.getFieldInfo(objectType)
  private var propertyNames : Array[String] = null
  private var propertyTypes : Array[IElementType] = null
  private var chooser : Method = null
  private var _getChildSchema: (CSONCursor)=>IEntitySchema = null

  private def childSchemaChooser(objType:Class[_]):(CSONCursor)=> IEntitySchema = {
    try {
      chooser = objType.getDeclaredMethod("schemaChooser", classOf[Object])
    }
    catch { // Ignore the method not found exception
      case e: java.lang.NoSuchMethodException => chooser = null
      case otherE:Exception => throw otherE
    }
     if (chooser==null) null
     else (cur:CSONCursor) => chooser.invoke(objectType.newInstance(),getElementType(0).getValue(cur,0).asInstanceOf[Object]).asInstanceOf[IEntitySchema]
  }

  private def getArrayTypeInfo(componentTypeInfo:Class[_]):CSONArrayType = {
    // We can define "Type Binary = Array[Byte]" in app then define binary array in this way "val barr = new Array[Binary](1)"
    val elementType = getTypeInfo(componentTypeInfo)
    if (elementType.isInstanceOf[CSONArrayType]) throw new Exception("Can't support Array of Array type.")
    else new CSONArrayType(elementType) 
  }
  private def getTypeInfo(fieldTypeInfo:Class[_]): IElementType = {
    if (fieldTypeInfo.isArray()&&fieldTypeInfo.getName()!="[B") getArrayTypeInfo(fieldTypeInfo.getComponentType())
    else {
      val elementType = CSONTypesArray.typeFactory(fieldTypeInfo)
      // This is a class type
      if (elementType == null) EntitySchemaCache.objectElementTypeFactory(fieldTypeInfo)
      else elementType
    }
  }
  private def getProperties(fields:Array[Field]) = {
    propertyNames = new Array[String](fields.length)
    propertyTypes = new Array[IElementType](fields.length)
    val fieldGetList = new Array[Method](fields.length)
    val fieldSetList = new Array[Method](fields.length)
    var index = 0
    fields.foreach((field:Field)=>{
      val mf = field.getModifiers()
      if (mf==2) { // ignore the class constructor parameters
        val fieldName = field.getName()
        val fieldTypeInfo = field.getType()
        propertyNames(index) = fieldName
        fieldGetList(index) = objectType.getMethod(fieldName)
        fieldSetList(index) = objectType.getMethod(fieldName + "_$eq", fieldTypeInfo)
        propertyTypes(index) = getTypeInfo(fieldTypeInfo)
        index += 1
      }
    })
    val key = objectType.getName()
    if (fields.length>index) { // shrink to ignore the class constructor parameters
      val tGetList = new Array[Method](index)
      Array.copy(fieldGetList, 0,tGetList, 0, index)
      EntitySchemaCache.addFiGet(key, tGetList)
      val tSetList = new Array[Method](index)
      Array.copy(fieldSetList, 0, tSetList, 0, index)
      EntitySchemaCache.addFiSet(key, tSetList)
      val tpNames = new Array[String](index)
      Array.copy(propertyNames,0,tpNames,0,index)
      propertyNames = tpNames
      val tpTypes = new Array[IElementType](index)
      Array.copy(propertyTypes,0,tpTypes,0,index)
      propertyTypes = tpTypes
    }
    else {
      EntitySchemaCache.addFiGet(key, fieldGetList)
      EntitySchemaCache.addFiSet(key, fieldSetList)
    }
  }

    private def getIndex(propertyIndex:Any):Int = propertyIndex match {
                      case i:Int => i
                    case s:String => if (s.charAt(0).isDigit)s.toInt else getID(s)
                    case _ => throw new IllegalArgumentException
    }
  def populate() = {
    if (propertyNames==null) {
      getProperties(entityFieldInfo)
      _getChildSchema = childSchemaChooser(objectType)
    }
  }
  def entityId : String = objectType.getName()
  def count : Int = propertyNames.length
  def schemaType : EntityType = EntityType.ComplexType
  def containsProperty(property:String):Boolean = propertyNames.contains(property)
  def objType : Class[_] = objectType
  def getID(PropertyName:String):Int = propertyNames.indexOf(PropertyName)
  def getPropertyName(ID:Int):String = propertyNames(ID)
  def getTypeCode(property:Any):Byte = {
    propertyTypes(getIndex(property)) match {
      case simpleElement:CSONSimpleType => simpleElement.getTypeCode
      case complex:CSONComplexType => CSONTypes.EmbeddedDocument.id.toByte
      case array:CSONArrayType => CSONTypes.Array.id.toByte
    }
  }
  /**
   * Return the element IElementType if this property is an array,
   * return CSONComplexType if this property is a Complex Element.
   */
  def getElementType(property:Any):IElementType = propertyTypes(getIndex(property))
  override def getChildSchema: (CSONCursor)=>IEntitySchema = _getChildSchema
}
object GeneralEntitySchema{
  def apply(objectType:Class[_]):IEntitySchema = EntitySchemaCache.objectElementTypeFactory(objectType).
    asInstanceOf[CSONComplexType].schema
}
private object EntitySchemaCache {
  private val schemaMap  = new ConcurrentHashMap[String,IElementType]
  private val fieldInfoCache = new ConcurrentHashMap[String,Array[Field]]
  private val fieldGetCache = new ConcurrentHashMap[String,Array[Method]]
  private val fieldSetCache = new ConcurrentHashMap[String,Array[Method]]

  private def addSchemaType(key:String, objType:Class[_]):IElementType = {
    val ret = if (!objType.isArray())  new CSONComplexType(new GeneralEntitySchema(objType)) 
            else {
              val objectTypeInfo = objType.getComponentType()
              val elementType = CSONTypesArray.typeFactory(objectTypeInfo)
              if (elementType!=null) new CSONArrayType(elementType)
              else new CSONArrayType(objectSchemaFactory(objectTypeInfo))
            }
    schemaMap.put(key, ret)
    ret
  }
  private def addFi(key:String, fiList:Array[Field]):Array[Field] = {
    fieldInfoCache.put(key, fiList)
    fiList
  }
  
  private def getAllFields(typeInfo:Class[_]):Array[Field] = {
    val superType = typeInfo.getSuperclass()
    if (superType.getName()=="java.lang.Object") typeInfo.getDeclaredFields()
    else {
      val superClassFields = getAllFields(superType)
      val thisFields = typeInfo.getDeclaredFields()
      var ret = new Array[Field](superClassFields.length+thisFields.length)
      superClassFields.copyToArray[Field](ret)
      thisFields.copyToArray[Field](ret, superClassFields.length)
      ret
    }
  }
  def getFieldInfo(typeInfo:Class[_]):Array[Field] = {
    val key = typeInfo.getName()
    val fiInfo = fieldInfoCache.get(key)
    if (fiInfo==null) {
      val ret = getAllFields(typeInfo)
      fieldInfoCache.put(key, ret)
      ret
    }
    else fiInfo
  }
  def getFiCache(typeName:String):Option[Array[Field]] = {
    val ret = fieldInfoCache.get(typeName)
    if (ret == null) None
    else Some(ret)
  }
  def getFiGetCache(typeName:String):Array[Method] = fieldGetCache.get(typeName)
  def addFiGet(key:String, FiGetList:Array[Method]):Array[Method] = {
    fieldGetCache.put(key, FiGetList)
    FiGetList
  }
  def getFiSetCache(typeName:String):Array[Method] = fieldSetCache.get(typeName)
  def addFiSet(key:String, FiSetList:Array[Method]):Array[Method] = {
    fieldSetCache.put(key, FiSetList)
    FiSetList
  }
  
  def objectSchemaFactory(objectType:Class[_]): IEntitySchema = {
    objectElementTypeFactory(objectType).asInstanceOf[CSONComplexType].schema
  }  
  def objectElementTypeFactory(objectType:Class[_]): IElementType = {
    val typeKey = objectType.getName()
    val schemaType = schemaMap.get(typeKey)
    // If can't find in the cache, use the concurrent putIfAbsent to create and get the schema. If this put fail, try to read 
    // this class's schema from the cache (It may be put into the cache by another thread)
    if (schemaType == null) {
      val ret = addSchemaType(typeKey,objectType)
      if (ret.isInstanceOf[CSONComplexType])
        ret.asInstanceOf[CSONComplexType].schema.asInstanceOf[GeneralEntitySchema].populate()
      ret
    }
    else schemaType
  }  
}