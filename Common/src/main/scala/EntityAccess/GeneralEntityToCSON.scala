package EntityAccess
import java.lang.reflect.Method
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.ConcurrentHashMap
import CSON.Types._
import CSON._
import EntityInterface._
import EnumHelper._

object GeneralEntityToCSON {
  def serializeObject(obj:AnyRef,outBuffer:ByteBuffer) =
    EntitySerializerCache.getEntityToCSON(obj.getClass).writeObjectToCSON(obj,outBuffer)
  def deserializeCSON(cson:IEntityRandomAccess) = {
    val deserializer = new GeneralEntityToCSON(cson.getSchema)
    deserializer.getObject(cson.asInstanceOf[CSONDocument])
  }
  def apply(typeInfo:Class[_]):GeneralEntityToCSON = EntitySerializerCache.getEntityToCSON(typeInfo)
  def getRawValue(value:Object,needTypeCode:Boolean):Array[Byte] = {
    val outBuffer = ByteBuffer.wrap(new Array[Byte](4096)).order(ByteOrder.LITTLE_ENDIAN)
    var len:Int = 0

    if (value==null) len = CSONElement.NullValue.elementType.getRawValue(outBuffer, value, needTypeCode).flip().limit()
    else {
      val objectTypeInfo = value.getClass()
      val elementType = CSONTypesArray.typeFactory(objectTypeInfo)
      if (elementType!=null) len = elementType.getRawValue(outBuffer, value, needTypeCode).flip().limit()
      else len = EntitySchemaCache.objectElementTypeFactory(objectTypeInfo).
                  getRawValue(outBuffer, value, needTypeCode).flip().limit()
    }
    val ret = new Array[Byte](len)
    outBuffer.get(ret)
    ret
  }

  /**
   * Deserialize Binary raw value to object
   * @param b :Array[Byte]
   * @param needTypeCode true for use the "typeCode" param to deserialize, false to read type code and decode
   * @param typeCode if needTypeCode = false, ignored
   * @return
   */
  def readRawValue2Obj(b:Array[Byte],needTypeCode:Boolean,typeCode:Byte=0):Any = {
    def convertRawValue(typeCode:Byte,bb:ByteBuffer): Any =
    {
      CSONTypes(typeCode) match {
        case CSONTypes.Boolean => (bb.getInt() == 1)
        case CSONTypes.Int8 => bb.getInt()
        case CSONTypes.Int16 => bb.getInt()
        case CSONTypes.Int32 => bb.getInt()
        case CSONTypes.FloatingPoint => bb.getDouble()
        case CSONTypes.Single => bb.getFloat()
        case CSONTypes.UTF8String =>
          val strBytes = new Array[Byte](bb.getInt())
          bb.get(strBytes)
          new String(strBytes, "UTF-8")
        case CSONTypes.JavaScriptCode =>
          val strBytes = new Array[Byte](bb.getInt())
          bb.get(strBytes)
          new String(strBytes, "UTF-8")
        case CSONTypes.BinaryData =>
          val buf: Array[Byte] = new Array[Byte](bb.getInt())
          bb.get(buf)
          buf
        case CSONTypes.Decimal =>
          val signPart = bb.getInt()
          if (signPart == 0) return BigDecimal(signPart)
          val longPart: BigDecimal = BigDecimal(bb.getLong())
          val mantissaPart: BigDecimal = BigDecimal(bb.getInt().toLong,4)
          if (signPart < 0) -(longPart+mantissaPart)
          else longPart+mantissaPart
        case CSONTypes.Int64 => bb.getLong()
        case CSONTypes.Timestamp => new java.util.Date(bb.getLong())
        case CSONTypes.UTCDatetime => new java.util.Date(bb.getLong()).getTime()
        case CSONTypes.ObjectId|CSONTypes.DBPointer =>
          val highBits = bb.getLong()
          val LowBits = bb.getLong()
          new java.util.UUID(highBits, LowBits)
        case CSONTypes.NullValue|CSONTypes.NullElement =>
          bb.getInt(0)
        case _ => null
      }
    }
    val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
    val rawValueType = if (needTypeCode) typeCode else bb.get()
    convertRawValue(rawValueType,bb)
  }
}
class GeneralEntityToCSON(schema:IEntitySchema) {
  val count = schema.count
  def this(classOfEntity:Class[_]) {
    this(EntitySchemaCache.objectSchemaFactory(classOfEntity))
  }
  /**
   * Deserializer for reading the object value from CSONBuffer
   */
  private def getValueFromCSONElement(element:IElementValue):Any = {
    // As implementation tricks of scala array, if the component is class object, we can se the generic "Array[AnyRef]" to hold it's content
    // For value type array, it's instance must be created in exact type, eg. "Array[Int], Array[String]..."
    def getComplexElementArray(elementArray:CSONElementArray,componentType:Class[_]): Any = {
        val objArray = java.lang.reflect.Array.newInstance(componentType, elementArray.length).asInstanceOf[Array[Object]]
          //new Array[AnyRef]()

        for (i <- 0 until elementArray.length)
          objArray.update(i, getValueFromCSONElement(elementArray.getElement(i)).asInstanceOf[AnyRef])

        objArray
    }

    var ret : Any = null
    var el: Any = null
    var methodSet:Method = null
    var propertyTypeName:String = null
    element match {
      case simpleElement:CSONElement => ret = simpleElement.getValue
      case complexElement:CSONComplexElement => {
        val elementSchema = complexElement.getSchema
        val objType = elementSchema.objType
        val elementClassName = elementSchema.entityId
        val elementFieldInfo = EntitySchemaCache.getFiCache(elementClassName).getOrElse(null)
        try {
          ret = objType.newInstance()
          val setFuncs = EntitySchemaCache.getFiSetCache(elementClassName)
          for (i<-0 until setFuncs.length) {
            val setter = setFuncs(i)
            propertyTypeName = elementFieldInfo(i).getType().getName()
            propertyTypeName match {
              case "scala.Enumeration$Value"|"scala.Enumeration$Val" =>
                setter.invoke(ret, EnumType(complexElement.getValue(i).asInstanceOf[Int]))
              case _ =>
                val v = getValueFromCSONElement(complexElement.getElement(i))
                if (v!=null) {
                  el = v
                  methodSet = setter
                  setter.invoke(ret,v.asInstanceOf[Object])
                }
            }
          }
        } catch {
          case e: InstantiationException =>
            if (objType.getName.indexOf('$') >= 0)
              throw new IllegalArgumentException("Inner class used in POJO class member:" + objType.getName + ". Not allowed.")
            else throw e
          /*case e:java.lang.IllegalArgumentException =>
            throw new IllegalArgumentException("Wrong property name:"+methodSet.getName+" type:"+propertyTypeName+" element:"+el.getClass.getName)*/
        }
      }
      case elementArray:CSONElementArray => {
        val elementSchema = elementArray.getSchema
        ret = if (elementSchema!=null) getComplexElementArray(elementArray,elementSchema.objType)
            else elementArray.elementType.asInstanceOf[CSONArrayType].elementType.writeArrayValue(elementArray)
      }
    }
    ret
  }
  def readCSONToObject(dataBuffer:ByteBuffer):AnyRef = {
    val csonDoc = new CSONDocument(schema,Some(dataBuffer))

    val ret = getValueFromCSONElement(csonDoc.toCSONElement).asInstanceOf[AnyRef]
    csonDoc.completeRead
    ret
  }
  def getObject(csonDoc:CSONDocument):AnyRef = getValueFromCSONElement(csonDoc.toCSONElement).asInstanceOf[AnyRef]
  def getObject(complexElement:IElementValue):AnyRef = getValueFromCSONElement(complexElement).asInstanceOf[AnyRef]

  // Serializer functions to write object values into CSONBuffer
  private def getCSONElementFromObject(value:Any,target:Option[IEntityRandomAccess]):IEntityRandomAccess = {
    val targetElement = (target.getOrElse(new CSONComplexElement(EntitySchemaCache.objectSchemaFactory(value.getClass),None)))
    val schema = targetElement.getSchema
    val fiGetList = EntitySchemaCache.getFiGetCache(schema.entityId)
    for (i <- 0 until schema.count) {
      try {
        val propertyValue = fiGetList(i).invoke(value)
        if (propertyValue == null) targetElement.setValue(i, CSONElement.NullValue)
        else targetElement.setValue(i, schema.getElementType(i) match {
          case simpleType:CSONSimpleType =>  propertyValue
          case complexType:CSONComplexType =>getCSONElementFromObject(propertyValue,None)
          case arrayType:CSONArrayType => {
            val childElementType = schema.getElementType(i).asInstanceOf[CSONArrayType].elementType
            getCSONArrayFromObject(propertyValue,childElementType)}
        })
      }catch {
        case e:NullPointerException => throw new Exception("Encoding error: value="+value+" |schema="
          +schema.entityId+" |propName="+schema.getPropertyName(i)+" |FI="+fiGetList(i)+"|e:"+e)
        case e:Exception => throw new Exception("Encoding error: value="+value+" |schema="
          +schema.entityId+" |propName="+schema.getPropertyName(i)+" |FI="+fiGetList(i)+"|e:"+e)
      }
    }
    targetElement
  }

  private def getCSONArrayFromObject(value:AnyRef,schemaOrType:Any):CSONElementArray = {
    val elementArray = new CSONElementArray(schemaOrType,None)
    val elementType = CSONElement.getElementType(schemaOrType)

    if (elementType.isEmpty) {
      val objArray = value.asInstanceOf[Array[AnyRef]]
      val len = objArray.length
      for (i <- 0 until len) elementArray.append(
                              if (objArray(i)== null) CSONElement.NullValue
                              else getCSONElementFromObject(objArray(i),None))
    }
    else elementType.get.readArrayValue(elementArray,value)

    elementArray
  }
  /**
   * Serialize the given value to a CSONDocument(in this serializer specified schema)
   */
  def writeObjectToCSON(value:AnyRef,dataBuffer:ByteBuffer):(IEntityRandomAccess,ByteBuffer) = {
    val doc = new CSONDocument(schema,None)

    getCSONElementFromObject(value,Some(doc))

    (doc,if (dataBuffer!=null) doc.toByteBuffer(dataBuffer) else null)
  }

  /**
   * Serialize Plain Object into a CSON format Byte Array
   * @param value plain object to be serialized
   * @param dataBuffer ByteBuffer to be written. Notice: if you want to get Array[Byte], you have to set this null
   * @return Array[Byte] encoded in CSON format if dataBuffer param is null. Otherwise return (null, ByteBuffer contains
   *         the encoded CSON format binary). This ByteBuffer size may be insufficient and new ByteBuffer generated and returned.
   */
  def getObjectCsonBinary(value:AnyRef,dataBuffer:ByteBuffer):(Array[Byte],ByteBuffer) = {
    val doc = new CSONDocument(schema,None)
    getCSONElementFromObject(value,Some(doc))
    if (dataBuffer!=null) (null,doc.toByteBuffer(dataBuffer))
    else (doc.getBytes(),null)
  }
}
object EntitySerializerCache {
  private val serializerCache = new ConcurrentHashMap[String, GeneralEntityToCSON]
  
  def getEntityToCSON(typeInfo:Class[_]):GeneralEntityToCSON = {
    val key = typeInfo.getName()
    val serializer = serializerCache.get(key)
    if (serializer==null) {
      val ret = new GeneralEntityToCSON(typeInfo)
      serializerCache.put(key, ret)
      ret
    }
    else serializer
  }
}