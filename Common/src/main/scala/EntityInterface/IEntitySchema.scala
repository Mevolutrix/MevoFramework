/**
 * Interface for accessing entity Schema and schema related types metadata
 */
package EntityInterface
/**
 * @author znhu
 *
 */
import CSON.{CSONCursor,WriteCursor}
import scala.collection.{Iterator,mutable}
import java.nio.ByteBuffer

object EntityType extends Enumeration {
  type EntityType = Value
  val Data = Value(1)
  val View = Value(2)
  val ComplexType = Value(3)
}

trait IElementType {
    protected var typeCode : Byte = 0
  var getValue : (CSONCursor, Int)=>Any = null
  def getRawValue(dataBuffer:ByteBuffer,value:Any,needTypeCode:Boolean):ByteBuffer = {
    if (needTypeCode) dataBuffer.put(typeCode)

    writeRawFunc(value,dataBuffer)
    dataBuffer
  }
  var addValue : (Any,Any)=>Any = null
  def compare(x:Any,y:Any):Int = {
    var flag = false
      var v1:Int = 1
      var v2:Int = 1
      if (x==null) {
        flag = true;
        v1 = 0 
      }
      if (y==null) {
        flag = true;
        v2 = 0
      }
      if (flag) v1-v2
      else getCompareValue(x,y).asInstanceOf[Int]
    }
  var writeElement : (Any,WriteCursor,Int)=>Unit = null

  protected var writeRawFunc : scala.Function2[Any,ByteBuffer,Unit] = null
  protected var getCompareValue : Function2[Any,Any,Any] = null
}

trait IElementValue {
  def add(that: IElementValue)
  def elementType : IElementType
  def compare(that:IElementValue):Int
  def getValue() : Any
  def setValue(newValue:Any):Unit
}

trait IEntitySchema {
import EntityType._
  def entityId : String
  def count : Int
  def schemaType : EntityType
  def containsProperty(property:String):Boolean
  def objType : Class[_]
  def getID(propertyName:String):Int
  def getPropertyName(ID:Int):String
  def getTypeCode(property:Any):Byte
  /**
   * Return the element IElementType if this property is an array,
   * return CSONComplexType if this property is a Complex Element.
   */
  def getElementType(property:Any):IElementType
  /**
   * Function reading the CSON structure inside values to decide which real schema it should be.
   * This schema is only the parent class schema. Schema of child class can be get by this function.
   * User must implement a child class schema selecting function and set it's pointer into this schema property
   */
  def getChildSchema: (CSONCursor)=>IEntitySchema = null
}

object IndexType extends Enumeration {
  type IndexType = Value
  val Default = Value(0)
  val Hash = Value(1)
  val Async = Value(2)
} 

object StorageType extends Enumeration {
  type StorageType = Value
  val NoStore = Value(0) //Pure cache will be discard when expire
  val TenantSeperated = Value(1) //stored in sharding table by tenant
  val TenantCentralized = Value(2)  // all tenants sharing same table(A hidden tenant id will be stored in primary key to seperate tenant's records
  val Shared = Value(3) //
}

object DefaultValueType extends Enumeration {
  type DefaultValueType = Value
  val Non = Value(0) //No default value
  val Preset = Value(1) //a predefined value such as current time
  val Eval = Value(2) // expression for calculating the default value
}

trait IEntitySet {
  import DefaultValueType._
  import StorageType._
  import IndexType._
  
  def appSpace : String
  def setName : String
  def primaryKey : String
  def storageType : StorageType
  def indexProperties:Array[String]
  def defaultValueProperties : Array[String]
  def isIndex(PropertyName : String) : Boolean
  def getIndexType(PropertyName : String) : IndexType
  def getDefaultValueType(propertyName:String) : DefaultValueType
  def getDefaultValueDefinition(propertyName:String) : String
  def getAutomaticProperty : String
  def getPrimaryKeyMin:String
  def getGZIPPropertyName:String
  def isFlat:Boolean
}
trait ISetMetadata {
  def getName:String
  def baseSchema:IEntitySchema
  def supportSchemas:Iterator[IEntitySchema]
}
trait IReferenceInfo {
  def refId: String
  def appSpace: String
  def referenceMap: mutable.HashMap[String, String]
}