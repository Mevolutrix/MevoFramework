package EntityStore.Metadata
import EntityInterface.DefaultValueType._
import EntityInterface.EntityType


class EntityProperty
{
  // This class will be reflected thus we have to hide the Vars in an val class object to avoid the reflection error
  private class localVars () { var _complexType:EntitySchema = _; var _appSpaceName:String = _ }
  var name:String=_
  var description:String=_
  var pType:Byte=0
  var complexTypeName:String = null  // 为空表示非ComplexType if null then this is a simple value type property
  var nullable:Boolean = true
  var isArray:Boolean = false
  var verificationRegEx:ValidationRule = null
  /*def this(appSpace:String,nm:String,typ:Byte,ctName:String,canNull:Boolean,isArr:Boolean,verifyRegEx:ValidationRule,des:String=null) {
    this()
    name = nm
    pType = typ
    complexTypeName = ctName
    nullable = canNull
    isArray = isArr
    verificationRegEx = verifyRegEx
    privateStore._appSpaceName = appSpace
    description =des
  }*/
  //# method
  def complexType:EntitySchema = {
    if (privateStore._complexType == null && complexTypeName != null) initComplexType(privateStore._appSpaceName, complexTypeName)
    privateStore._complexType
  }
  def initComplexType(schema:EntitySchema) = privateStore._complexType=schema
  def initComplexType(appSpace:String,cTypeName:String)
  {
    if (privateStore._complexType==null) {
      privateStore._complexType = if (cTypeName != null)
        MetadataManager.getSchema(appSpace + "." + complexTypeName,EntityType.ComplexType).asInstanceOf[EntitySchema]
      else null
    }
  }
  private val privateStore = new localVars()
}

/*class PropertyUpdateRecord
{
  var entityId:String=_ // Add to which Entity definition 添加在哪个Entity实体定义中 (EntitySchema._id)
  var propertyId: Int=0  // 在实体定义中位于哪个位置存储
  var property2Update:EntityProperty=_
}*/

class PropertyDefaultValue
{
  var name:String=_
  var dType:DefaultValueType=_
  var defaultValue:String=_
  var evalExp:String=_
}
class ValidationRule
{
  var name:String=_
  var invokeType:Byte=0   //  0/1("watch"/"blur")
  var pattern:String=_
  var errMsg:String=_
}