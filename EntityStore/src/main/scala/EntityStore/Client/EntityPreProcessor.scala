package EntityStore.Client
import java.util.Date
import JSON.JsonString
import Encoding.B64GZIP
import JScriptEngine.JSCompiler
import akka.pattern.ask
import akka.util.Timeout
import java.util.regex._
import CSON.Types.CSONTypes
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import EntityStore.Metadata.EntitySchema
import MySQLStore.autoValueGenerator.AutoValue
import EntityAccess.{GeneralEntityToCSON, JSONSerializer}
import CSON.{CSONElementArray, CSONComplexElement, CSONDocument}
import EntityInterface.{IEntityRandomAccess, DefaultValueType, IEntitySchema, IEntitySet}

object EntityPreProcessor {
  val log = LoggerFactory.getLogger("PreProcessor")
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  private val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  // entity: target entity CSON document to put default values
  // jsonData: original json data which can be used when there is an JS eval statement for one default property
  // setInfo: stored the default value definition
  private def defaultValues(entity:IEntityRandomAccess,jsonData:String,setInfo:IEntitySet,isUpdate:Boolean=false):IEntityRandomAccess = {
    // create default value for timestamp or preset value
    def getRealVal(pValue:String,pValueType:CSONTypes.CSONTypes):AnyRef={
      pValue match {
        case null =>  if ( pValueType==CSONTypes.Timestamp) new Date()
                       else throw new Exception("Default value setting for "+pValueType+" is null.")
        case _ => (pValueType match {
          case CSONTypes.BinaryData => JsonString.hexStrToBinary(pValue)
          case CSONTypes.Boolean => (pValue=="true")
          case CSONTypes.Int8 => pValue.toByte
          case CSONTypes.Int16 => pValue.toInt
          case CSONTypes.Int32 => pValue.toInt
          case CSONTypes.Int64 => pValue.toLong
          case CSONTypes.Single => pValue.toFloat
          case CSONTypes.FloatingPoint => pValue.toDouble
          case CSONTypes.Decimal => pValue.toInt
          case CSONTypes.Timestamp|CSONTypes.UTCDatetime =>
            println("Preset Value for:"+pValue+"|")
            if (pValue==""||pValue=="null") new Date()
            else new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(pValue)
          case CSONTypes.UTF8String => pValue
          case _ => pValue
        }).asInstanceOf[Object]
      }
    }
    // call JS evaluation engine to generate default value
    def callEvalJs(pValue:String, data:String, pValueType:CSONTypes.CSONTypes):AnyRef = {
      val value = JSCompiler(pValue).invoke(data).asInstanceOf[String]
      (pValueType match {
        case CSONTypes.BinaryData => JsonString.hexStrToBinary(value)
        case CSONTypes.Boolean => (value=="true")
        case CSONTypes.Int8 => value.toByte
        case CSONTypes.Int16 => value.toInt
        case CSONTypes.Int32 => value.toInt
        case CSONTypes.Int64 => value.toLong
        case CSONTypes.Single => value.toFloat
        case CSONTypes.FloatingPoint => value.toDouble
        case CSONTypes.Decimal => value.toInt
        case CSONTypes.UTCDatetime => new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(value)
        case CSONTypes.UTF8String => value
        case _ => value
      }).asInstanceOf[Object]
    }
    // generate default value for each property with default value definition
    def getDefaultValue(pName:String,schema:IEntitySchema,setInfo:IEntitySet):AnyRef = {
      val pValue = setInfo.getDefaultValueDefinition(pName)
      val pValueType = CSONTypes(schema.getTypeCode(pName))
      val dTypeValue: Int = setInfo.getDefaultValueType(pName).id
      DefaultValueType(dTypeValue) match {
        case DefaultValueType.Preset => getRealVal(pValue, pValueType)
        case DefaultValueType.Eval => callEvalJs(pValue, jsonData,pValueType)
      }
    }

    setInfo.defaultValueProperties.foreach(pName => {
      //test if the property is a nested property(eg. one element of array or property of Complex obj
      val pList = pName.split("//")
      if (pList.length > 1) {
        // array obj, pList(0) maintain the type
        if (entity.getSchema.getTypeCode(pList(0)) == CSONTypes.Array.id.toByte) {
          val arr = entity.getElement(pList(0)).asInstanceOf[CSONElementArray]
          for (i <- 0 until arr.length) {
            if (arr.getElement(i).isInstanceOf[CSONComplexElement]) {
              val element = arr.getElement(i).asInstanceOf[CSONComplexElement]
              val itemSchema = arr.getSchema
              //pList(1) is the real name of property
              if (itemSchema.containsProperty(pList(1)))
              //Must be insert or update and the user set value = null then get default value(prevent default value
              //conflict with the updated data
                if (!isUpdate || (isUpdate && (element.getValue(pList(1)) == null)))
                  element.setValue(pList(1), getDefaultValue(pList(1), itemSchema, setInfo))
            }
          }
        }
        //complex obj
        else if (entity.getElement(pList(0)).isInstanceOf[CSONComplexElement]){
          val csonComplex = entity.getElement(pList(0)).asInstanceOf[CSONComplexElement]
          val itemSchema = csonComplex.getSchema
          if (itemSchema.containsProperty(pList(1)))
            //Must be insert or update and the user set value = null then get default value(prevent default value
            //conflict with the updated data
            if (!isUpdate || (isUpdate && (csonComplex.getValue(pList(1))==null)))
              csonComplex.setValue(pList(1), getDefaultValue(pList(1), itemSchema, setInfo))
        }
        //value
      } else {
        if (entity.getSchema.containsProperty(pName))
          //Must be insert or update and the user set value = null then get default value(prevent default value
          //conflict with the updated data
          if (!isUpdate || (isUpdate && (entity.getValue(pName)==null)))
            entity.setValue(pName, getDefaultValue(pName, entity.getSchema, setInfo))
      }
    })
    // Check and gzip+BASE64 encoding the property need to be compressed
    return gzipProperty(entity,setInfo)
  }
  private def validation(entity:CSONDocument,schema:IEntitySchema):(Boolean,String) = {
    val count = schema.count
    val properties = schema.asInstanceOf[EntitySchema].properties
    var foundError = false
    val ErrMsg = new StringBuilder()
    for(i<-0 until count) {
      val regExp = properties(i).verificationRegEx
      if (!foundError && regExp!=null) {
        val validationPattern = Pattern.compile(regExp.pattern)
        val matcher = validationPattern.matcher(entity.getValue(i).toString)
        if (!matcher.matches()) {
          foundError = true
          ErrMsg.append("<").append(properties(i).name).append("> validation error ").append(regExp.errMsg)
        }
      }
    }
    (!foundError,ErrMsg.toString())
  }
  // gzip compress the property which defined in EntitySet metadata
  private def gzipProperty(entity:IEntityRandomAccess,setInfo:IEntitySet):IEntityRandomAccess= {
    val pName = setInfo.getGZIPPropertyName
    if (pName!=null && !pName.isEmpty) {
      //test if the property is a nested property(eg. one element of array or property of Complex obj
      val pList = pName.split("//")
      if (pList.length > 1) {
        val csonComplex = entity.getElement(pList(0)).asInstanceOf[CSONComplexElement]
        val itemSchema = csonComplex.getSchema
        if (itemSchema.containsProperty(pList(1))) {
          val str = csonComplex.getValue(pList(1)).asInstanceOf[String]
          if (str.isInstanceOf[String])
            csonComplex.setValue(pList(1), B64GZIP.encodeGZIP(B64GZIP.b64Decoding(str)))
          else throw new IllegalArgumentException("Property [" + pName + "] is not a String type can't be compressed.")
        }
      } else {
        val schema = entity.getSchema
        if (pName != null && schema.containsProperty(pName)) {
          val str = entity.getValue(pName).asInstanceOf[String]
          if (str.isInstanceOf[String])
            entity.setValue(pName, B64GZIP.encodeGZIP(B64GZIP.b64Decoding(str)))
          else throw new IllegalArgumentException("Property [" + pName + "] is not a String type can't be compressed.")
        }
      }
    }
    entity
  }

  def apply(data:String,schema:IEntitySchema,setInfo:IEntitySet):Array[Byte] = {
    val entity:CSONDocument = JSONSerializer.unapply(data, schema).asInstanceOf[CSONDocument]
    val (isValid,errMsg) = validation(entity,entity.getSchema)
    if (isValid) defaultValues(entity,data,setInfo,true).asInstanceOf[CSONDocument].getBytes()
    else throw new IllegalArgumentException(errMsg)
  }
  def preCreate(data:String,schema:IEntitySchema,setInfo:IEntitySet):CSONDocument = {
    try {
      val entity: CSONDocument = JSONSerializer.unapply(data, schema).asInstanceOf[CSONDocument]
    val (isValid,errMsg) = validation(entity,entity.getSchema)
    if (isValid){
      val autoProperty = setInfo.getAutomaticProperty
      if (autoProperty!=null && !autoProperty.isEmpty) {
        val pkType = schema.getTypeCode(autoProperty) == CSONTypes.Int32.id.toByte
        val future = AutoValue(setInfo.setName,pkType).ask("getKey")
        val autoKey = Await.result(future, timeout.duration).asInstanceOf[AnyRef]
        log.info("processor get autoValue("+autoProperty+"):"+autoKey)
        entity.setValue(autoProperty,autoKey)
      }
      defaultValues(entity,data,setInfo).asInstanceOf[CSONDocument]
    }
    else throw new Exception(errMsg)
    } catch {
      case e:Exception => println("Wrong JSON format:"+data); throw e
    }
  }
  def smePostProcess(data:String,schema:IEntitySchema,setInfo:IEntitySet,needRawValue:Boolean=false):AnyRef = {
    val entity:CSONDocument = JSONSerializer.unapply(data, schema).asInstanceOf[CSONDocument]
    val (isValid,errMsg) = validation(entity,entity.getSchema)
    val ret = if (isValid) defaultValues(entity,data,setInfo,true).asInstanceOf[CSONDocument]
    else throw new Exception(errMsg)
    if (needRawValue) ret.getBytes()
    else GeneralEntityToCSON.deserializeCSON(ret)
  }
}
