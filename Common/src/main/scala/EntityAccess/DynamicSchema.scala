package EntityAccess
import EntityInterface._
import EntityType._
import CSON.Types._
import CSON._

class DynamicSchema(id:String) extends IEntitySchema {
  private val propertyList = new java.util.ArrayList[String]
  private val propertyTypeList = new java.util.ArrayList[IElementType]
  def getIndex(property:Any):Int = {
    property match {
      case i:Int => i
      case s:String => if (s.charAt(0).isDigit) s.toInt else getID(s)
      case _ => throw new IllegalArgumentException("Property index should be int or String.")
    }
  }
  def add(property:String,propertyType:IElementType) = {propertyList.add(property);propertyTypeList.add(propertyType)}

  override def entityId : String = id
  override def count : Int = propertyList.size()
  override def schemaType : EntityType = View
  override def containsProperty(property:String):Boolean = propertyList.contains(property)
  override def objType : Class[_] = null
  override def getID(propertyName:String):Int = propertyList.indexOf(propertyName)
  override def getPropertyName(ID:Int):String = propertyList.get(ID)
  override def getTypeCode(property:Any):Byte = {
      getElementType(property) match {
        case simple:CSONSimpleType => simple.getTypeCode
        case complex:CSONComplexType => CSONTypes.EmbeddedDocument.id.toByte
        case array:CSONArrayType => CSONTypes.Array.id.toByte
      }
    }
  override def getElementType(property:Any):IElementType = propertyTypeList.get(getIndex(property))
}