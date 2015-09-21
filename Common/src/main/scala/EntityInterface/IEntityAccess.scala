package EntityInterface
import scala.language.dynamics._

trait IEntitySequenceAccess {
  def iterator:Iterator[IElementValue]
  def append(value:Any) : Boolean
  def length : Int
}

trait IEntityRandomAccess {
    def getSchema : IEntitySchema
    def getValue(index:Any): Any
    def getElement(index:Any):IElementValue
    def getRawValue(index:Any,NeedTypeCode:Boolean=true):Array[Byte]
    def setValue(index:Any, value:AnyRef)
}
