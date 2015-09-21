package EntityAccess
/**
 * Not a full JSON Serializer, just as a companion for CSON binary format which optimized for performance
 */
import CSON._
import CSON.Types._
import spray.json._
import JSON._
import EntityInterface._
import java.text.SimpleDateFormat
import akka.util.{ByteString, ByteStringBuilder}

object Object2JSON {
  def apply(obj:AnyRef):String = {
    val csonSerializer = GeneralEntityToCSON(obj.getClass)
    val (csonDoc,_) = csonSerializer.writeObjectToCSON(obj,null)
    CSON2JSON(csonDoc.asInstanceOf[CSONDocument].toCSONElement)
  }

  def readJSONString(s:String,schema:IEntitySchema):AnyRef = {
    val csonSerializer = GeneralEntityToCSON(schema.objType)
    val csonDoc = CSON2JSON.readJSONString(s,schema)
    csonSerializer.getObject(csonDoc.asInstanceOf[CSONDocument])
  }
}
object CSON2JSON {
  def apply(element:IElementValue):String =
    element match {
      case null => "null"
      case e: CSONElement => JsonString(e.getValue)
      case array: CSONElementArray => array.iterator.map(apply).mkString("[",", ","]")
      case obj: CSONComplexElement => obj.mapIterator.map(p => {"\""+p._1+"\":"+apply(p._2)}).mkString("{",",","}")
    }
  def toCSONElement(x: JsValue,parentElement: IEntityRandomAccess,name:Any,schemaOrElementType:Any): AnyRef = x match {
    case JsNull => CSONElement.NullValue
    case JsBoolean(b) => new CSONElement(CSONTypes.Boolean.id.toByte, b)
    case JsString(s) =>
      val typeCode = CSONElement.getElementTypeCode(schemaOrElementType)
      new CSONElement(typeCode,CSONTypes(typeCode) match {
        case CSONTypes.Timestamp|CSONTypes.UTCDatetime =>
          new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(s)
        case CSONTypes.BinaryData => JsonString.hexStrToBinary(s)
        case CSONTypes.Boolean => s=="true" || s=="True"
        case CSONTypes.Decimal => BigDecimal(s)
        case _ => s
      })
    case JsNumber(n) => {
      val elementTypeCode = CSONElement.getElementTypeCode(schemaOrElementType)
      new CSONElement(elementTypeCode, CSONTypes(elementTypeCode) match {
        case CSONTypes.Boolean => if (n.toIntExact==1) true else false
        case CSONTypes.Int8 => n.toByteExact
        case CSONTypes.Int16=> n.toShortExact
        case CSONTypes.Int32=> n.toIntExact
        case CSONTypes.FloatingPoint => n.toDouble
        case CSONTypes.Single => n.toFloat
        case CSONTypes.UTF8String => n.toString
        case CSONTypes.Int64 => n.toLongExact
        case _ => n                                                     }
      )
    }
    case JsArray(xs) => {
      val elementArray = new CSONElementArray(parentElement.getSchema.getElementType(name),None)
      xs.foreach(jsv => {elementArray.append(toCSONElement(jsv,elementArray,0,
                                        elementArray.getElementSchemaOrType))})
      elementArray
    }
    case JsObject(m) => {
      // call universal function to check whether this is a ElementType or real schema and get the proper schema value
      val schema = CSONElement.getSchema(schemaOrElementType)
      val complexElement = if (name == null) parentElement // This is the root document
                            else new CSONComplexElement(schema,None)
      m.foreach(p => {
        val (key,v) = p
        complexElement.setValue(key, toCSONElement(v, complexElement, key, schema.getElementType(key)))
      })
      complexElement
    }
  }
  def readJSONString(s:String,schema:IEntitySchema):IEntityRandomAccess = {
    val csonDoc = new CSONDocument(schema,None)
    toCSONElement(JsonParser(s),csonDoc,null,schema)
    csonDoc
  }
}

object JSONSerializer {
type Binary = Array[Byte]
  /**
   * Convert the object or CSON document into JSON format text string
   *
   */
  def apply(value: AnyRef): String = value match {
    case cson: IEntityRandomAccess => CSON2JSON(value.asInstanceOf[CSONDocument].toCSONElement)
    case _ => Object2JSON(value)
  }
  /**
   * Read JSON format text and convert to plain object or CSON Document
   * Set param "resultType" with Class[_] format (getClass() result) when you expect this function return with an object
   * Set "resultType" with an IEntitySchema object when you want to deserialize to a CSONDocument
   */
  def unapply(s:String,resultType:AnyRef):AnyRef =
    resultType match {
      case schema:IEntitySchema => CSON2JSON.readJSONString(s,schema)
      case classInfo:Class[_]   =>
        Object2JSON.readJSONString(s,GeneralEntitySchema(classInfo))
    }
  def getStringsFromRawValue(rawValues:Array[Binary]):Array[String] = {
    val ret = new Array[String](rawValues.length)
    for(i<-0 until ret.length) ret(i)=JsonString.getString(GeneralEntityToCSON.readRawValue2Obj(rawValues(i),false))
    ret
  }
  def getValueFromString(s:String,typeCode:Byte):Object = {
    val value:String = s.trim
    try {
      (CSONTypes(typeCode) match {
        case CSONTypes.Boolean => value == "true"
        case CSONTypes.Int8 => value.toInt.toByte
        case CSONTypes.Int16 => value.toInt.toShort
        case CSONTypes.Int32 => value.toInt
        case CSONTypes.FloatingPoint => value.toDouble
        case CSONTypes.Single => value.toFloat
        case CSONTypes.BinaryData => JsonString.hexStrToBinary(value)
        case CSONTypes.Decimal => BigDecimal(value)
        case CSONTypes.Int64 => value.toLong
        case CSONTypes.Timestamp => new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(s)
        case CSONTypes.UTCDatetime => new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(s)
        case CSONTypes.ObjectId => java.util.UUID.fromString(value)
        case _ => null
      }).asInstanceOf[Object]
    }catch {
      case e: Exception => throw new Exception("Encounter value conversion exception,value = "+value)
    }
  }
}